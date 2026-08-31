# Módulo `insights` — compute, cubos y recomendaciones

> **Decisión de diseño (2026-08-31):** toda lógica que **piensa** (rollups, KPIs, baseline, copy clínico, recomendaciones) vive fuera del SOR, en un subproyecto dedicado — no en `bootstrap` ni en proyecciones passthrough de `views/`.

---

## 1. Posición en la arquitectura

```
┌──────────────┐     scene_events, episodes, rounds
│  mana-hive   │──────────────────────────────────────────┐
│  (detecta)   │                                          │
└──────────────┘                                          ▼
┌──────────────┐     GET /internal read o JDBC replica   ┌─────────────────┐
│  mana-hub    │◄─────────────────────────────────────────│  insights       │
│  (SOR)       │     POST summaries + insight records     │  (piensa)       │
└──────────────┘─────────────────────────────────────────►└────────┬────────┘
       ▲                                                              │
       │ GET proyecciones enriquecidas (opcional)                      │
       │ o panel lee insights API directo                               ▼
┌──────────────┐                                              cubo / warehouse
│  panel       │◄──── GET /api/v1/insights/... ────────────────────┘
└──────────────┘
```

| Capa | Rol | Ejemplos |
|------|-----|----------|
| **hub** | Persiste hechos y agregados **canónicos** por día | `sleep_summaries`, `care_summaries`, `scene_events` |
| **insights** | Calcula, interpreta, recomienda | avg 7d, eficiencia sueño, baseline, “en su rango”, alarm suggestion |
| **panel** | Presenta | gráficos + copy |

`bootstrap/.../views/ProjectionService` queda **passthrough + zero-fill + observedFrom** — no fórmulas clínicas.

---

## 2. Gradle: subproyecto propuesto

```
mana-hub/
  insights/                    ← nuevo módulo (app Spring Boot separada o library + runner)
    build.gradle.kts
    src/main/kotlin/com/hub/insights/
      InsightsApplication.kt   ← puerto distinto (ej. 8081), no Flyway
      rollup/                  ← batch nocturno
        NightlyRollupJob.kt
        SleepRollup.kt         ← scene_events → SleepSummaryData
        MobilityRollup.kt
        BathroomRollup.kt
        CareRollup.kt          ← rounds + staff visits + episodios
      stream/                  ← reactivo
        EpisodeResolvedHandler.kt
        StaffVisitAggregator.kt
      derive/                  ← KPIs panel (lo que hoy hace el front)
        SleepInsights.kt       ← 7h16, +9m, 16%, eficiencia 95%
        CareInsights.kt
        BaselineService.kt     ← ventana mínima 7d, Susan → baseline_ready=false
      recommend/               ← texto + acciones sugeridas
        AlarmRecommendation.kt
        WellbeingRecommendation.kt
      outbound/
        HubSummaryPublisher.kt ← POST /internal/v1/clinical/*, care-summaries
        InsightStore.kt        ← cubo PG / parquet / ClickHouse (fase 2)
      inbound/
        HubReadClient.kt       ← scene-events, episodes, residents (clients DSL)
      config/
        ObservationWindow.kt   ← TZ facility, sueño 19:00→12:00
```

**`settings.gradle.kts`:** `include("insights")`

**Dependencias:** `clients`, `shared-kernel`, Spring Batch o `@Scheduled`, **sin** JPA entities duplicadas del hub (HTTP first; JDBC read-replica opcional).

**No integrar en `bootstrap`** — deploy independiente, escala aparte, fallos de rollup no tumban SOR.

## 3. Scene events — tipos hive (no strings inventados)

Contrato: `com.manahive.contracts.scene`

| Hecho hive (`SceneEvent`) | Cambia FSM persona | Uso insights |
|---------------------------|--------------------|--------------|
| `TransitionDetected(from, to)` | sí | dwell sueño / movilidad / baño |
| `NightOpened(initialState)` | sí | abre la noche; hub copia `initialState` → `toState` |
| `ComeBackExceeded` / `ComeBackWarning` | no | mismo estado; no abren dwell |
| `DwellExceeded` / `DwellWarning` | no | mismo estado |
| `StaffPresenceDetected` / `StaffLeftDetected` | no | cuidado reactivo (visitas) |
| `SceneStateChanged` / `SceneDwell*` | no | `from`/`to` son flags de escena, no `PersonState` |
| `SignalLost` / `SignalRecovered` | no | hub puede ignorarlos al persistir |
| `NightClosed` | no | trigger futuro de rollup; no cambia FSM |

Wire (`SceneEventSerializer`): `from`/`to`/`initialState` = **simpleName** de `PersonState` (`Lying`, `SittingInBed`, `InBathroom`…). Hub aplana a `fromState`/`toState`.

### `PersonState` / `StateKind` → summaries (catálogo hive, 13 estados)

Comentarios de `PersonState.kt` — **no hay Walking**.

| Grupo hive | Estado | Sueño | Movilidad | Baño |
|------------|--------|-------|-----------|------|
| in_bed | `Lying` | primer Lying de la noche: N min latencia (restless), resto calm; vueltas siguientes todo calm | in_bed | — |
| in_bed | `SittingInBed`, `BedEdge` | awake in bed | in_bed | — |
| in_bed | `AttemptingExit` | restless (gusanito) | in_bed | — |
| out_of_bed | `Standing`, `InRoom` | out of bed | out_of_bed, **no** andar | — |
| out_of_bed | `InBathroom` | out of bed | out_of_bed | visita |
| out_of_bed | `InHallway`, `Outdoor`, `Absent` | out of bed | **andar** (fuera de habitación) | — |
| furniture | `OnFloor`, `InChair`, `InWheelchair` | out of bed | out_of_bed, no andar | — |
| unknown | `Unknown` | out of bed | out_of_sight | — |

`deepSleepAfterMinutes` = latencia **una vez por noche** (default 10). Andar ≈ tiempo **fuera de la habitación**, no `Standing`. Distancia en metros es estimado de insights (`walkingMinutes × N`); el SOR guarda `distanceMeters = 0`.

Cada día de ficha lleva `measured`: el spine sigue existiendo; un cero con `measured=false` no es hallazgo. Insights derive ignora no medidos.

Ingest de summaries es **upsert** por `(resident_id, observed_on)` (replay del rollup). GET `scene-events?from=&to=` acota la noche + lookback 12h.

### A — Batch nocturno (obligatorio)

```
06:00 America/Argentina/Buenos_Aires
  FOR EACH active resident:
    IF observed_on < admission_date → SKIP
    READ scene_events [ventana D]
    COMPUTE sleep | mobility | bathroom | care (daily)
    POST hub (sourceRecordId determinístico)
    WRITE cubo fact_* partition D
    COMPUTE derive.* (KPIs, baseline flags)
    WRITE insight_snapshots (resident, D, json)
```

### B — Stream en cierre de episodio (complementario)

```
ON episode.resolved (NATS / webhook):
  EMIT fact_episode
  IF staff_present during episode → fact_care_visit { trigger: REACTIVE }
  OPTIONAL: increment care partial (o esperar batch — preferir batch para total diario)
  EMIT recommendation si aplica (ej. “3ª salida de cama en 2h — revisar perfil E1”)
```

**Regla:** batch cierra el día; stream no reemplaza rollup de sueño.

---

## 4. Contratos de salida

### 4.1 Hacia hub (SOR) — ya existen

| POST | Contenido |
|------|-----------|
| `/internal/v1/clinical/sleep-summaries` | `SleepSummaryData` |
| `/internal/v1/clinical/mobility-summaries` | `MobilitySummaryData` |
| `/internal/v1/clinical/bathroom-summaries` | `BathroomSummaryData` |
| `/internal/v1/care-summaries` | total/proactive/rounds/notes |

`sourceRecordId = "insights-{residentId}-{observedOn}-{kind}"` — idempotente.

### 4.2 Hacia panel / cubo (nuevo — fase insights API)

```json
GET /api/v1/insights/resident-chart/{id}/sleep?from=&to=

{
  "residentId": "jose",
  "observedFrom": "2024-01-15",
  "baselineReady": true,
  "summaries": [ /* días crudos o mirror hub */ ],
  "derived": {
    "avgCalmMinutes7d": 436,
    "deltaCalmMinutesWoW": 9,
    "restlessShare": 0.16,
    "sleepEfficiency": 0.95,
    "habitualWindow": { "from": "01:15", "to": "09:30" }
  },
  "recommendations": [
    {
      "code": "SLEEP_IN_RANGE",
      "severity": "info",
      "text": "Comparado contra su propia línea base… en su rango."
    }
  ]
}
```

Alternativa: hub persiste `insight_snapshots` (tabla futura) y panel solo lee — insights calcula y POSTea snapshot.

---

## 5. Cero, Susan y alertas

| Flag | Uso |
|------|-----|
| `observedFrom` | max(from, admissionDate) — spine del gráfico |
| `baselineReady` | `days_since_admission >= 7` AND enough summary rows |
| `measureObserved` | true si el rollup corrió para ese día |

**Alertas / recomendaciones en insights:**

- `baselineReady == false` → solo copy informativo, **no** thresholds de tendencia
- `totalMinutes == 0` post-rollup → “sin visitas”, no “caída 100%”
- Episodio E1 resuelto solo → recomendación de **episodio**, no recalcular sueño

---

## 6. Qué NO va en insights (sigue en hive/hub)

| Responsabilidad | Dónde |
|-----------------|-------|
| Detectar transición / hysteresis | SceneEngine (hive) |
| Disparar episodio ComeBack | EpisodeEngine |
| Persistir scene_events | hub |
| Config alarmas | hub `policy` |
| Enviar SMS/push | NotificationService externo |

---

## 7. Relación con módulos existentes

| Módulo | Relación |
|--------|----------|
| `clients` | insights usa DSL para leer/escribir hub |
| `blueprints` | escenarios E1 (`01-e1-vuelve-solo`) validan rollup + insight |
| `panel-api` | legacy duplicado — panel migraría a hub views + insights API |
| `event-bridge` | NATS → insights handlers (episode.resolved) |
| `bootstrap/views` | sigue crudo; panel puede migrar a insights API para KPIs |

---

## 8. Fases de implementación

1. **F1 — Rollup library:** ✅ `insights/` — scene_events → sleep/mobility/bathroom (heurística v1)
2. **F2 — Derive:** ✅ SleepInsights + BaselineService (José vs Susan)
3. **F3 — Insights HTTP:** ✅ `GET /api/v1/insights/resident-chart/{id}/sleep|care|mobility|bathroom`
4. **F4 — Cubo:** pendiente
5. **F5 — Stream:** ✅ stub `POST /internal/v1/insights/episodes/resolved`
6. **F6 — Hallazgos + informes:** ✅ catálogo fijo en `find/` + JSON de report + script `python-docx`

App: puerto **8081**. Cron 06:00 **apagado** por defecto (`insights.rollup.enabled=false`). Trigger manual:

```
POST /internal/v1/insights/rollup/{residentId}?date=2026-08-31&publish=false
POST /internal/v1/insights/rollup?date=2026-08-31&publish=true
```

---

## 10. Hallazgos, decisiones e informes

Después del rollup nocturno el cubo diario ya está. Insights **destila hallazgos** (no “regresiones” estadísticas): patrones contra uno mismo, concentración horaria, y huecos de política. El panel los pinta; un script arma el Word; más adelante el mismo JSON se persiste o se manda por mail.

| Tipo (`FindingKind`) | Qué es | Ejemplo |
|----------------------|--------|---------|
| `BRIEFING` | Párrafo de la ventana | “Durmió 5h 48… Las salidas vienen aumentando…” |
| `TREND` | Contra la semana anterior | salidas de cama ↑, baño noche ↑, inquieto alto |
| `CLUSTER` | Concentración horaria | salidas entre 5:00 y 6:05 |
| `POLICY` | Hallazgo + propuesta de umbral | avisar en el borde apenas se detecte |
| `WATCH` | Nota / positivo / línea base | “en su rango”, Susan en formación |

No se inventa cobertura de turno (“una sola enfermera en el piso”) si no está en el cubo. “Lo que avisa hoy” sale del **nivel + overrides** del hub; los números del catálogo FALL_RISK/NIGHT_WANDERING/CRITICAL están espejados en `PolicyCopy` (insights no depende de `policy`). Un override se marca **ajuste manual**.

POC — detectores fijos en `FindingCatalog` (umbral 7 días, `baselineReady`):

- `SLEEP_14D_BRIEFING`, KPIs inquieto / salidas / tiempo en cama / eficiencia
- `BED_EXITS_RISING` (WoW ≥ 15% y +0.3)
- `BED_EXIT_DAWN_CLUSTER` (≥3 salidas en 7d, ≥2/3 entre 05:00–06:05)
- `POLICY_BED_EDGE_DAWN` si el cluster existe y el aviso de borde es ≥ 1 min → decisión con propuesta
- `SLEEP_RESTLESS_HIGH` / `SLEEP_IN_RANGE` / `BATHROOM_NIGHT_UP` / `CARE_THIN`
- `BASELINE_FORMING` y nada más (Susan)

Una **tarjeta de decisión** es un `Finding` `POLICY` con `awaitingDecision`, `proposal.applyLabel = Aplicar el cambio`. Al aplicar (más adelante, comando de alarmas) el hallazgo queda como motivo, con `evidence.episodeIds`.

### API (on-demand; persistir snapshots es fase 2)

```
GET /api/v1/insights/resident-chart/{id}/sleep          ← cards + narrative + findings de sueño
GET /api/v1/insights/resident-chart/{id}/briefing?days=14
GET /api/v1/insights/resident-chart/{id}/report?days=30  ← JSON del Word (episodios incluidos)
GET /api/v1/insights/facility/briefing?days=14           ← a revisar vs tendencias positivas
GET /api/v1/insights/facility/report?days=30
```

`downloadUrl` va `null` en esta POC. El panel puede pedir el JSON ahora o un link más tarde; es el mismo documento.

### Word

`scripts/reports/generate.py` (python-docx) — dos plantillas: ficha del residente (30d) y briefing de dirección.

```
pip install -r scripts/reports/requirements.txt
python scripts/reports/generate.py --resident jose --days 30
python scripts/reports/generate.py --facility --days 14
```

Salida: `scripts/reports/generated/`. Insights **no** shell-out a Python. Email diario a una lista de contactos: mismo JSON, otro job.

---

## 11. Checklist antes de codear

- [ ] TZ única por facility en config
- [ ] Ventana sueño documentada por residencia
- [ ] Idempotencia POST summaries
- [ ] `baselineReady` en toda respuesta con KPI
- [ ] Blueprint: E1 vuelve solo + Susan alta reciente
- [ ] No `@Scheduled` en bootstrap

Ver también: [summaries-resident-chart-cube.md](summaries-resident-chart-cube.md)
