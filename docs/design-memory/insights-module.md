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
| in_bed | `Lying` | ≥ N min → calm; primeros N → restless | in_bed | — |
| in_bed | `SittingInBed`, `BedEdge` | awake in bed | in_bed | — |
| in_bed | `AttemptingExit` | restless (gusanito) | in_bed | — |
| out_of_bed | `Standing`, `InRoom` | out of bed | out_of_bed, **no** andar | — |
| out_of_bed | `InBathroom` | out of bed | out_of_bed | visita |
| out_of_bed | `InHallway`, `Outdoor`, `Absent` | out of bed | **andar** (fuera de habitación) | — |
| furniture | `OnFloor`, `InChair`, `InWheelchair` | out of bed | out_of_bed, no andar | — |
| unknown | `Unknown` | out of bed | out_of_sight | — |

`deepSleepAfterMinutes` default 10. Andar ≈ tiempo **fuera de la habitación**, no `Standing`.

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

App: puerto **8081**. Cron 06:00 **apagado** por defecto (`insights.rollup.enabled=false`). Trigger manual:

```
POST /internal/v1/insights/rollup/{residentId}?date=2026-08-31&publish=false
POST /internal/v1/insights/rollup?date=2026-08-31&publish=true
```

---

## 9. Checklist antes de codear

- [ ] TZ única por facility en config
- [ ] Ventana sueño documentada por residencia
- [ ] Idempotencia POST summaries
- [ ] `baselineReady` en toda respuesta con KPI
- [ ] Blueprint: E1 vuelve solo + Susan alta reciente
- [ ] No `@Scheduled` en bootstrap

Ver también: [summaries-resident-chart-cube.md](summaries-resident-chart-cube.md)
