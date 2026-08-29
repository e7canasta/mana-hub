# Integración mana-hive ↔ mana-hub — Propuesta Arquitectónica

> **Pregunta:** tenemos `mana-hive` (motores + NATS + SOR en memoria) y `mana-hub` (SOR PostgreSQL real, 44 tablas, 101 endpoints). ¿Cómo hacemos que el **ciclo de vida de episodios/eventos/alertas** llegue al SOR, y que los **cambios de presets** del director (desde el panel) lleguen a hive para recalibrar? Hive usa NATS JetStream interno.

**TL;DR:** `mana-hub` = **Source of Truth** persistente. `mana-hive` = **motores efímeros** + pipeline NATS. Bridge **bidireccional NATS JetStream** con **Outbox** en hub + **consumidores idempotentes** en ambos lados. REST se mantiene para paneles/lecturas; NATS para M2M.

## 1. Mapa Real Hoy

| Repo | Stack | SOR | Bus | Rol |
|------|-------|-----|-----|-----|
| **mana-hive** `settings.gradle.kts:60` + `gradle/libs.versions.toml:jnats 2.21.4` | Kotlin 2.4.20-RC + Spring Boot 4.0.1 + NATS JetStream | `hub/hub-service` **InMemory** (`InMemoryPolicyLayerStore`, `InMemoryLedger`, `application.yml:8` excluye `DataSource`) | NATS `nats://localhost:4222` (`hub/hub-service/src/main/resources/application.yml:26`) + `platform/messaging/Subjects.kt:11` (8 subjects versionados) | El que **PIENSA** (4 motores puros: Scene→Sentinel→Harbor→Recorder `docs/wiki/2-core-architecture...:22` + `NightWatchRuntime` por residente) |
| **mana-hub** `gradle/libs.versions.toml:2` | Kotlin 2.4.20-RC + Spring Boot 4.0.1 + PostgreSQL 17 | **PostgreSQL 44 tablas** (`data-model.md`, Flyway V1-V10) + 101 endpoints (`api.md`) + DSL 11 scopes (`clients/core/ManaHubClient.kt:22`) | Hoy **sin NATS** (solo `POST /internal/v1/*` y `POST /api/v1/*`) | El que **RECUERDA** (SOR externo) |

**Problema actual:** hive guarda `PolicyLayers → AlarmProfile → PolicyCalibration` en memoria (se pierde al reiniciar) y emite `hub.policy.change.v1` solo intra-hive. Hub guarda `alarm_profile_versions + alarm_profile_overrides (DAG)` persistente y expone `PATCH /api/v1/alarm-presets/{residentId}` desde panel, pero hive no lo ve. Episodios: hive los genera (`SentinelSignal EpisodeOpened/Closed` en `sentinel.signal.v1.<bed>`) y Harbor (`alarm.event.v1.<alert>`) pero hub solo los recibe si alguien hace `POST /api/v1/episodes` manual — no hay bridge automático → SOR vacío.

## 2. Principios de Integración (Arquitecto)

1. **SOR único:** `mana-hub` PostgreSQL es la única verdad persistente. `mana-hive/hub` se vuelve **caché derivada** (o se retira). Todo `ChangeWatchLevel` / `AddAdjustment` del panel pasa por `hub` y se proyecta a `hive` vía bus.
2. **Bus como buffer, no como DB:** NATS JetStream 7 días retención (`platform/messaging/NatsTopology.kt:20`) = transporte. Hub = retención definitiva.
3. **Hexagonal + Outbox:** ni hive ni hub hacen `dual-write` (DB + NATS sin transacción). Hub persiste y escribe **Outbox** en la misma TX PostgreSQL; un relay publica a JetStream. Hive es `pure domain` (`ResidentRuntime.kt:53`) — solo NATS adapters (`SceneNatsEgress`, `SentinelNatsIngest`) hacen I/O.
4. **Contrato versionado:** subject incluye versión (`scene.fact.v1.<bed>` `Subjects.kt:8`). Cambio rompedor = nuevo subject `v2`, consumidores viejos siguen en `v1`.
5. **Idempotencia por diseño:** todo evento lleva `eventId / sourceEventId / source_record_id` UNIQUE (`sensor_events.source_event_id`, `scene_events.event_id`, `episodes.id`, `sleep_summaries.source_record_id`). Consumidor reintenta sin duplicar.

## 3. Mecanismo Propuesto: **NATS JetStream Bridge Bidireccional** (Recomendado)

### 3.1 Topología

```
PANEL (Director) ──PATCH /api/v1/alarm-presets/{id}──▶ mana-hub (PostgreSQL)
                                                     │  AlarmProfileApplicationService
                                                     │  ├─ INSERT alarm_profile_versions + overrides (TX)
                                                     │  └─ INSERT outbox (hub_policy_outbox) ──────┐
                                                     │                                            │ TX
                                                     │  OutboxRelay (poll JetStream) ◀────────────┘
                                                     │         │ publish EventEnvelope
                                                     ▼         ▼
                                              NATS JetStream (nats://localhost:4222)
                                              ┌──────────────────────────────┐
                         hub.policy.change.v1 │  hub.policy.effective-rules.v1.<resident> │ sentinel.signal.v1.<bed> │ alarm.event.v1.<alert> │ scene.fact.v1.<bed> │ recorder.command.v1.<bed>
                                              └──────┬───────────────────────┘
                                                     │
   mana-hive NightWatchRuntime ◀──────────────────────┤
     PolicyNatsIngest (hub.policy.>)                  │  SentinelNatsEgress ──▶ hub NATS Bridge Consumer
     Sentinel: recalibration per-resident             │  HarborNatsEgress   ──▶ hub
     ResidentRuntime per bed (ConcurrentHashMap)      │  SceneNatsEgress    ──▶ hub
                                                     ▼
                                              mana-hub NATS Bridge Consumers (durable, pull)
                                                     │
                                              ┌──────┴──────────────────────────────┐
                                              │ sensor_events │ scene_events │ episodes (+transitions) │ notification_events / deliveries │ clip_windows / evidence │
                                              └─────────────────────────────────────┘
                                                     │
   Consultores ◀────────── GET /api/v1/wings/{id}/board|/residents/{id}/timeline|/reports ── mana-hub
```

**Dos flujos:**

- **Sur (hub → hive):** Director cambia preset en panel → hub persiste + outbox → relay publica `hub.policy.change.v1` (`PolicyChangeDetected` `PolicyNatsEgress.kt:57`) y `hub.policy.effective-rules.v1.<resident>` (`EffectiveRules` `PolicyNatsEgress.kt:88`). Hive `PoliticaEngine` resuelve vs `DagCatalog` → emite calibraciones a `Scene/Sentinel/Harbor/Recorder`. `NightWatchRuntime` recarga `ResidentRuntime` sin reiniciar.
- **Norte (hive → hub):** Sensor `Observation` → Scene `SceneEvent` (`scene.fact.v1.<bed>`) → Sentinel `SentinelSignal EpisodeOpened` (`sentinel.signal.v1.<bed>`) → Harbor `NoticeCommand Dispatch` (`alarm.event.v1.<alert>`) → Recorder `RecordingCommand` → **hub Bridge** consume y persiste idempotente.

REST se mantiene para lecturas panel y como **fallback** si NATS caído (hive puede `POST /internal/v1/events` directo con retry).

### 3.2 Mapeo Subjects → Tablas/Endpoints (Contrato)

| Hive Subject (`Subjects.kt:11`) | Payload hive (`platform/contracts`) | Hub destino | Hub tabla / endpoint |
|--------------------------------|-------------------------------------|-------------|----------------------|
| `perception.observation.v1.<bed>` | `Observation` (raw sensor) | `POST /internal/v1/events` | `sensor_events` + `current_bed_states` (`staff_present` V10) |
| `scene.fact.v1.<bed>` | `SceneEvent` / `SceneFact` (`TransitionDetected`, `DwellExceeded`) | `POST /internal/v1/scene-events` (implementado 2026-08-29) | `scene_events` |
| `sentinel.signal.v1.<bed>` | `SentinelSignal` (`EpisodeOpened`, `EpisodeClosed`, `EpisodeUpdated`) | `POST /api/v1/episodes` + `POST /acknowledge` + `PATCH` | `episodes` + `episode_transitions` |
| `alarm.event.v1.<alert>` | `NoticeCommand` (`Dispatch`, `Cancel`) | `POST /internal/v1/notifications` | `notification_events` + `notification_deliveries` |
| `recorder.command.v1.<bed>` | `RecordingCommand` (`Start`, `Stop`) | `POST /api/v1/clip-windows` + `evidence` | `clip_windows` + `evidence` |
| `evidence.record.v1.<bed>` | `EvidenceRecord` | `POST /api/v1/evidence` | `evidence` |
| `hub.policy.change.v1` | `PolicyChangeDetected {residentId, snapshot: AlarmProfile}` | hub → hive | `PolicyNatsIngest` → `PolicyResolver` recalibration |
| `hub.policy.effective-rules.v1.<resident>` | `EffectiveRules {rules, fingerprint}` | hub → hive (Sentinel) | `SentinelNatsIngest` |
| `hub.census.snapshot.v1` | `Census` (bed assignments) | hub → hive | `NightWatchRuntime` asignación residente↔cama |

**Envelope:** todo via `EventEnvelope {eventId, type, version, occurredAt, source, payloadJson}` (`platform/messaging/NatsObjectMapper.kt:27` ISO-8601). Hub lo desenvuelve y persiste `sourceEventId / eventId` para idempotencia.

### 3.3 Componentes Nuevos en `mana-hub`

**No tocar `mana-hive` (salvo exponer `hub.policy.*` ya existente).** Todo lo nuevo vive en `mana-hub`:

```
hub (nuevo módulo opcional hub-messaging o en bootstrap)
├── hub-messaging/
│   ├── HubNatsBridge.kt          // @Configuration @Import(NatsClientConfiguration)
│   ├── HubPolicyOutboxRelay.kt   // poll hub_policy_outbox → JetStream (cada 100ms)
│   ├── HubPolicyNatsEgress.kt    // publica PolicyChangeDetected + EffectiveRules (copia de hive PolicyNatsEgress.kt:30 pero con hub's AlarmProfileVersion)
│   └── consumers/
│       ├── SceneFactConsumer.kt      // subscribe scene.fact.v1.> → POST interno → scene_events
│       ├── SentinelSignalConsumer.kt // sentinel.signal.v1.> → episodes
│       ├── AlarmEventConsumer.kt     // alarm.event.v1.> → notification_events
│       └── RecorderConsumer.kt       // recorder.command.v1.> → clip_windows
├── bootstrap/src/main/resources/db/migration/V11__hub_policy_outbox.sql
│       CREATE TABLE hub_policy_outbox (id TEXT PK, aggregate_id TEXT, type TEXT, payload_json TEXT, occurred_at TIMESTAMP, published BOOLEAN DEFAULT FALSE)
└── bootstrap/src/main/resources/application.yml  → nats.enabled=true, nats.url=nats://localhost:4222
```

**Dependencia:** `hub-messaging/build.gradle.kts` → `api(projects.platform.messaging)` no es posible cross-repo, así que duplica `libs.jnats:2.21.4`, `NatsClientConfiguration.kt` y `Subjects.kt` en hub (copia exacta) o extrae `platform/contracts` como artefacto Maven compartido (recomendado largo plazo).

### 3.4 Flujo Detalle: Director cambia preset

```
1. Panel → PATCH /api/v1/alarm-presets/{residentId} {riskLevel:HIGH, templateId:FALL_RISK, overridesJson}
     → AlarmProfileApplicationService.patch() (TX)
       - INSERT alarm_profile_versions (valid_to = null) + alarm_profile_overrides
       - INSERT hub_policy_outbox {type:PolicyChangeDetected, payload: {residentId, snapshot, fingerprint}}

2. HubPolicyOutboxRelay (Spring @Scheduled, poll SELECT ... WHERE published=false FOR UPDATE SKIP LOCKED)
     → NatsObjectMapper.mapper.writeValueAsBytes(EventEnvelope)
     → jetStream.publish("hub.policy.change.v1", envelope)
     → jetStream.publish("hub.policy.effective-rules.v1.<resident>", envelope2)
     → UPDATE outbox SET published=true
     (si falla, reintenta; 7 días retención en JetStream → no pérdida)

3. mana-hive:
   - politica-engine: @NatsListener("hub.policy.change.v1") → PolicyResolver.resolve(catalog, profile) → publica `hub.policy.effective-rules.*` (si no lo hizo hub)
   - sentinel-service: SentinelNatsIngest@76 subscribe `hub.policy.effective-rules.v1.<resident>` → recarga SentinelCalibration para ese ResidentRuntime
   - scene-engine: recarga SceneCalibration (hysteresis)
   - harbor/recorder: recarga budget/recording window

4. GET /api/v1/alarm-presets/{id}/history en hub muestra 2 versiones (audit).
```

Idempotencia: `alarm_profile_versions` ya tiene `UNIQUE (resident_id) WHERE valid_to IS NULL` + `version` optimista. Replay de NATS no duplica.

### 3.5 Flujo Detalle: Episodio llega al SOR

```
1. Camera → perception.observation.v1.bed-101 (hive) → SceneEngine DigitalTwin → scene.fact.v1.bed-101 {from:Lying, to:Sitting, trigger:hysteresis}
2. SentinelEvaluator (hive) vs SentinelCalibration (FALL_RISK: dwell 15m) → sentinel.signal.v1.bed-101 {EpisodeOpened id:ep-123, severity:WARNING, ruleId: dwell_sitting}
3. Harbor HarborEngine → alarm.event.v1.ep-123 {Dispatch channel:CONSOLE, recipient:staff-group-1}
4. Recorder → recorder.command.v1.bed-101 {Start window: -30s +5m}

5. mana-hub NATS Bridge (hub-messaging):
   - SentinelSignalConsumer (durable consumer, pull, batch 10, ack explicit):
     envelope.type == "EpisodeOpened" → POST /api/v1/episodes {residentId, bedId, severity:WARNING, ruleId, sourceEventId: envelope.eventId}
       → INSERT episodes (id=ep-123) + episode_transitions (pending, sequence 1)
       → si duplicate key (source_event_id UNIQUE) → ack sin reinsertar (idempotente)
     envelope.type == "EpisodeClosed" → PATCH /api/v1/episodes/{id} {status:RESOLVED}
   - AlarmEventConsumer → INSERT notification_events + notification_deliveries (+ delivery_events)
   - SceneFactConsumer → INSERT scene_events (event_id UNIQUE)
   - RecorderConsumer → INSERT clip_windows (window_id) + timelines

6. Panel consulta: GET /api/v1/episodes?status=pending o GET /api/v1/wings/{wing}/board → ve episodio WARNING
7. Enfermera → POST /api/v1/episodes/{id}/acknowledge → PATCH /resolve → hub registra episode_transitions + episode_notes
   (opcional) hub publica `hub.episode.lifecycle.v1` para que hive cierre EpisodeLedger (si auto-resolución)
```

**Garantías:** JetStream `deliver=explicit ack`, `maxDeliver=5`, DLQ subject `dlq.hub.v1.>` para poison. Consumidor hub usa `sourceEventId` como clave idempotente; hive ya genera `UUID` per envelope (`PolicyNatsEgress.kt:70`).

## 4. Alternativas Descartadas

| Opción | Pros | Contras | Veredicto |
|--------|------|---------|-----------|
| **HTTP REST push** (hive `RestTemplate POST /internal/v1/*` a hub) | Simple, sin NATS en hub. Ya existe `POST /internal/v1/events` (+ `scene-events` 2026-08-29) | Acoplamiento temporal (hub caído → hive pierde eventos), retry manual, sin buffer, no escala a 100 camas | **Fallback sí**, primario no |
| **CDC / Debezium** (Postgres WAL → Kafka/NATS) | No cambia código hub | Hub → hive solo (policy), no hive → hub (episodios). Complejo infra | Descartado |
| **Shared DB** (hive escribe directo a PostgreSQL) | Sin bus | Rompe pure domain (`ResidentRuntime.kt:40` es puro), hive no debe conocer schema hub, coupling fuerte | Anti-patrón |
| **Solo NATS, sin REST** | Bus único | Paneles no son NATS-aware; `GET /api/v1/board` seguiría necesitando REST | Híbrido es mejor |

## 5. Recomendación Arquitectónica Final

**Fase 0 (ya hecho 2026-08-29):** hub expone `POST /internal/v1/scene-events` + `GET /api/v1/residents/{id}/scene-events`, `POST /internal/v1/care-summaries` corregido. Clientes resilient (`EvidenceContext` fallback, `IdentityContext` fallback). Base para bridge.

**Fase 1 — Hub publica policy (1 semana):**
- Crear `V11__hub_policy_outbox.sql` + `HubPolicyOutboxRelay.kt` en `mana-hub/bootstrap`.
- Copiar `NatsClientConfiguration.kt`, `NatsTopology.kt`, `Subjects.kt`, `NatsObjectMapper.kt` de `mana-hive/platform/messaging` a `mana-hub/hub-messaging` (o publicar como artefacto).
- Implementar `HubPolicyNatsEgress` en hub (similar a `mana-hive/hub/hub-service/src/main/kotlin/com/manahive/hub/nats/PolicyNatsEgress.kt:32`). `AlarmProfileApplicationService.patch()` debe escribir outbox en misma TX.
- Configurar `nats.enabled=true` en `mana-hub/bootstrap/src/main/resources/application.yml` (como hive `hub/hub-service/src/main/resources/application.yml:26`).
- Verificar con `blueprint:jose-301-e2e-pipeline` modificado para leer de NATS.

**Fase 2 — Hub consume episodios (1-2 semanas):**
- Implementar 4 consumidores JetStream durables en `hub-messaging` (pull, `hub-policy` stream ya existe, crear `hub-ingest` streams para `scene/sentinel/alarm/recorder`).
- Cada consumidor llama al `ApplicationService` correspondiente (no a REST interno, directo a repository para TX + idempotencia).
- Añadir testcontainer NATS (`testcontainers` ya en `gradle/libs.versions.toml:8` para postgres; añadir `nats-server`).
- Validar idempotencia: publicar mismo `EpisodeOpened` 2 veces → solo 1 fila `episodes`.

**Fase 3 — Retirar SOR en memoria de hive (cuando Fase 1 estable):**
- `mana-hive/hub/hub-service` pasa a `hub-census-cache`: `PolicyLayerStore` lee de `hub.policy.effective-rules.v1.*` y cachea en `ConcurrentHashMap` (no persiste). `PolicyService.resolve()` sigue igual pero fuente es NATS, no DB.
- Opcional: exponer `GET /api/policies/{residentId}` en hive solo como proxy a `mana-hub GET /api/v1/alarm-presets/{id}` para paneles que aún llaman hive.

**Observabilidad:** `hub_policy_outbox` con `published` + `attempts`, métricas Micrometer `hub.nats.publish.*`, alertas si `lag > 5s`. Audit: `audit_log` en hub + `EventEnvelope.source`.

## 6. Qué NO hacer

- No hacer `DB polling` cada segundo sin outbox (pierde eventos si crash antes de publish).
- No publicar NATS antes de commit DB (inconsistencia si rollback).
- No usar subject sin versión (`hub.policy.change` sin `.v1`).
- No bloquear `PolicyService.resolve()` si NATS caído (`ObjectProvider` `PolicyService.kt:28` ya lo desacopla — mantener).

## 7. Próximos pasos concretos (para tu próximo sprint)

- [ ] `mana-hub: ./gradlew :hub-messaging:compileKotlin` — crear módulo `hub-messaging` (copia `platform/messaging/build.gradle.kts` con `jnats:2.21.4`).
- [ ] `mana-hive: mvn publish` (o `gradle publish`) de `platform:contracts` para compartir `EventEnvelope` + `Subjects` sin copiar.
- [ ] Escribir `V11__hub_policy_outbox.sql` + `OutboxRelay` con `FOR UPDATE SKIP LOCKED`.
- [ ] Blueprint `hive→hub` e2e: `blueprints/jose-301-e2e-pipeline` que publique `sentinel.signal` y verifique `GET /api/v1/episodes` en hub.
- [ ] Documentar en `mana-hub/api.md` nuevos subjects (tabla §3.2) y en `mana-hub/docs/DESTILADO.md` como Fase 4.
