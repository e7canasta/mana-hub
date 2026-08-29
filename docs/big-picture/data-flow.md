# Data Flow

## The Canonical Event Chain — `docs/vocabulario-unificado.md:36`

```
┌─────────────┐    ┌───────────────┐    ┌──────────────┐    ┌───────────┐
│ PERCEPCIÓN  │───▶│ CAMBIO DE     │───▶│   EPISODIO   │───▶│ HISTORIA  │
│ (crudo)     │    │ ESCENA        │    │  (si regla)  │    │ CLÍNICA   │
└─────────────┘    └───────────────┘    └──────────────┘    └───────────┘
      │                   │                    │                  ▲
      │                   │                    │                  │
      │                   ├──▶ NOTIFICACIÓN    │    ┌───────────┐ │
      │                   │    (solo visual,   │    │ HALLAZGO  │─┘
      │                   │    sin episodio)   │    │(INSIGHT/  │
      ▼                   ▼                    ▼    │ PATTERN)  │
   sensor_events      scene_events         episodes  └───────────┘
+ current_bed_states                  → resident_notes
```

Vocabulario: `docs/vocabulario-unificado.md:53` — `PERCEPCIÓN` (sensor crudo) → `CAMBIO DE ESCENA` (hysteresis) → `NOTIFICACIÓN` o `EPISODIO` → `EVIDENCIA` → `HISTORIA` → `HALLAZGO` (experto/ML).

## Phase 1: Percepción — `POST /internal/v1/events`

```
Cámara/DL → POST /internal/v1/events (IngestPerceptionRequest) → sensor_events + current_bed_states
```

1. `ObservationEngine` detecta lectura instantánea (`PerceptionKind`: `POSTURE|LOCATION|STAFF_PRESENCE|ACCESSORY_PRESENCE`)
2. `clients/observation/ObservationContext.kt:30` `registerPerception(monitorKey,kind,bedId,residentId,state,sleeping)`
3. `EventIngestionService.kt:26` persiste `SensorEvent` + actualiza `CurrentBedState` (incl. `staff_present` V10 cuando `kind=STAFF_PRESENCE`)
4. Idempotencia por `sensor_events.source_event_id` UNIQUE.

## Phase 2: Cambio de Escena — `POST /internal/v1/scene-events` (implemented 2026-08-29)

```
Sensor → SceneEngine (hysteresis 5s) → POST /internal/v1/scene-events → scene_events
                                    → GET /api/v1/residents/{id}/scene-events (history)
```

1. `SceneEngine` confirma estabilidad temporal (no ruido), aplica `hysteresis_seconds` / `dwell` de `alarm_profile_overrides` (DAG, V9)
2. `clients/observation/ObservationContext.kt:80` `registerSceneChange(bedId,residentId,kind=TRANSITION|PERMANENCE, fromState → toState, triggerType=H YSTERESIS|PERMANENCE|MANUAL)`
3. `EventIngestionController.kt:46` `POST /internal/v1/scene-events` → `SceneEventRepository` → `scene_events` (PK `event_id` UNIQUE)
4. Consultas: `ObservationController.kt:115` `GET /api/v1/residents/{id}/scene-events` (nuevo), `GET /api/v1/beds/{id}/notifications` para `staff_entered/exited`.

> `blueprints/scenarios/CambioDeEscenaFlow.kt` valida contrato end-to-end (antes tenía `NOTE endpoint does not exist yet` — removed).

## Phase 3: Bifurcación — Notificación vs Episodio

```
SceneEvent → if policy == "solo visual"  → POST /internal/v1/notifications → notification_events (board)
           → if policy dispara episodio  → POST /api/v1/episodes → episodes + episode_transitions
```

- `notification_events` (`category`, `eventType`, `riskLevel`) se muestran en board sin requerir acción.
- `episodes` agrupan múltiples `scene_events` relacionados (severidad más alta gana), con `status` `pending→acknowledged→resolved` (`surveillance/domain/model/EpisodeSeverity.kt`).

## Phase 4: Episode Lifecycle — `POST /api/v1/episodes`

```
EpisodeEngine → POST /api/v1/episodes (PENDING) → NotificationService alerta staff
                                          ↓
                               EvidenceCollector → POST /api/v1/evidence|timelines|clip-windows
                                          ↓
                               Nurse → POST /api/v1/episodes/{id}/acknowledge → POST /api/v1/episodes/{id}/notes (ACKNOWLEDGEMENT) → PATCH /api/v1/episodes/{id} (RESOLVED)
```

- Matriz severidad: `INFO`(virtual), `WARNING`(virtual+video), `CRITICAL`/`EMERGENCY`(en sitio + grabación) — ver `vocabulario-unificado.md:143`.
- Auto-resolución pendiente (deuda `AUTO`): si residente vuelve a estado seguro, episodio se cierra automático (modelar `resolvedBy=AUTO`).

## Phase 5: Evidencia — `POST /api/v1/evidence|timelines|clip-windows`

```
EvidenceCollector → POST /api/v1/timelines (window_start) → POST /api/v1/clip-windows (started_at)
                              ↓                                    ↓
                      POST /api/v1/evidence (scene_event_id)   POST /api/v1/clip-windows/{id}/close (ended_at)
```

## Phase 6: Resúmenes Clínicos — `POST /internal/v1/clinical/*` + `POST /internal/v1/care-summaries`

```
ObservationEngine → POST /internal/v1/clinical/sleep-summaries    → sleep_summaries (+startedAt/endedAt V8)
                  → POST /internal/v1/clinical/mobility-summaries → mobility_summaries
                  → POST /internal/v1/clinical/bathroom-summaries → bathroom_summaries
CareEngine        → POST /internal/v1/care-summaries               → care_summaries (V7)  [+ legacy POST /api/v1/internal/care-summaries]
                  (fixed 2026-08-29: was POST /api/v1/internal/care-summaries)
```

Queries: `GET /api/v1/residents/{id}/sleep|mobility|bathroom|care?from=&to=` (14-day default, `ObservationController.kt:29`).

## Phase 7: Hallazgo — `POST /api/v1/residents/{id}/notes` (kind `INSIGHT|PATTERN`)

```
Experto/ML → POST /api/v1/residents/{id}/notes (kind=INSIGHT/PATTERN, body, sourceEventId?) → resident_notes
          → GET /api/v1/residents/{id}/notes + filter findingKinds (care/CareContext.kt:49)
```

También: `POST /api/v1/episodes/{id}/notes` (ACKNOWLEDGEMENT|RESOLUTION|CLINICAL_NOTE) y `POST /api/v1/shift-notes` (SHIFT_SUMMARY).

## Phase 8: Historia Clínica — `POST /api/v1/history-episodes`

```
EpisodeEngine|ML → POST /api/v1/history-episodes (sourceRecordId, kind, severity LOW|MEDIUM|HIGH|CRITICAL) → history_episode_detections
                  → POST interventions → history_episode_interventions (performed_by → staff_members.id)
                  → PATCH /api/v1/history-episodes/{id} (review: status, verdict, note) → history_episode_reviews
                  → GET /api/v1/history-episodes/{id}/sequence
                  → GET /api/v1/residents/{id}/falls?months=12
```

> `POST /internal/v1/clinical/incidents` es stub 201 sin persistencia — usar `POST /api/v1/history-episodes`.

## Full Day Cycle (con nuevas entidades)

```
06:00  Night shift ends, ShiftNote (SHIFT_SUMMARY) → POST /api/v1/shift-notes
08:00  Day shift starts, Round → POST /api/v1/rounds (wingId) + tasks
08:30  Nurse visits Room 101, RoundTask completed → PATCH /api/v1/round-tasks/{id} + CareNote
12:00  Lunch, resident in common area → sensor_events (out_of_bed)
14:00  Resident returns, SceneEvent TRANSITION in_bed→out_of_bed → POST /internal/v1/scene-events
18:00  Evening round, ResidentNote (OBSERVATION)
22:00  Night shift starts
02:00  Resident gets up, Perception LOCATION out_of_bed → POST /internal/v1/events
02:05  SceneEngine confirma PERMANENCE (10min) → POST /internal/v1/scene-events (PERMANENCE)
02:06  EpisodeEngine evalúa profile (HIGH risk, walker) → POST /api/v1/episodes (WARNING)
02:10  Nurse acknowledges → POST /api/v1/episodes/{id}/acknowledge + EpisodeNote
02:15  Resident back in bed, auto-resolve (future) || nurse resolves → PATCH /api/v1/episodes/{id}
06:00  Night shift ends, ShiftNote + daily summaries: sleep/mobility/bathroom/care → POST /internal/v1/clinical/* + /internal/v1/care-summaries
       History reviewed → PATCH /api/v1/history-episodes/{id} (review)
```

## Observability & Audit

Every write emits `DomainEvent` → `audit_log` (`audit/infrastructure/event/AuditEventListener.kt`). Query via `GET /api/v1/audit-log?entityType=&entityId=`.
