# Architecture Overview

## System Boundaries

mana-hub is a **System of Record**, not an execution engine.

### What We Do

| Responsibility | Description | Tables |
|----------------|-------------|--------|
| Persist data | Residents, episodes, evidence, notes, summaries | 44 PostgreSQL tables |
| Maintain state | Who is where, in which bed, staff present | `current_bed_states` (+`staff_present` V10), `resident_bed_assignments` |
| Record events | Every perception, scene change, notification | `sensor_events`, `scene_events`, `notification_events` |
| Query information | Board, timeline, reports, care | `GET /api/v1/...` (see `api.md`) |
| Maintain history | Normalized incidents + reviews | `history_episode_detections/reviews/interventions` |

### What We Don't Do

| Responsibility | Who Does It | Our contract |
|----------------|-------------|--------------|
| Analyze video | `ObservationEngine` | `POST /internal/v1/events` → `sensor_events` |
| Confirm scene (hysteresis) | `SceneEngine` | `POST /internal/v1/scene-events` → `scene_events` |
| Evaluate rules / decide | `EpisodeEngine` (Centinela/Harbor) | `POST /api/v1/episodes` → `episodes` |
| Send notifications | `NotificationService` | `POST /internal/v1/notifications` → `notification_events` |
| Record video | `EvidenceCollector` | `POST /api/v1/evidence|timelines|clip-windows` |

Stack real: Kotlin 2.4.20-RC + Spring Boot 4.0.1 + PostgreSQL 17 (`gradle/libs.versions.toml:2`, `bootstrap/src/main/resources/application.yml:8`). No Rust/Diesel/SQLite.

## Module Structure

```
mana-hub/  (settings.gradle.kts:22-37, 16 modules)
├── bootstrap/          # App + config + Flyway V1-V10 (12 files)
├── shared-kernel/      # Identifier, Entity, DomainEvent, Repository
├── identity/           # users, auth_sessions
├── audit/              # audit_log
├── residence/          # facilities, wings, rooms, beds, planogram_placements, room_privacy_regions
├── population/         # residents, resident_bed_assignments
├── coverage/           # staff_groups, facility_shifts, unit_shift_coverages, staff_members (V6)
├── care/               # rounds, round_tasks, care_notes + resident_notes/episode_notes/shift_notes (V4) + care_summaries (V7)
├── history/            # history_episode_detections/reviews (V5) + interventions (V6)
├── policy/             # alarm_profile_versions + alarm_profile_overrides (V9, DAG)
├── surveillance/       # episodes (ex-alerts V3), episode_transitions, notification_deliveries/events, episode_escalations
├── observation/        # sensor_events, current_bed_states (+staff_present V10), scene_events, notification_events, sleep/mobility/bathroom_summaries (+started/ended V8)
├── evidence/           # evidence, timelines, clip_windows
├── streams/            # streams, stream_regions
├── clients/            # DSL tipado Kotlin — 11 scopes (ManaHubClient.kt:22)
└── blueprints/         # 7 escenarios ejecutables (CambioDeEscenaFlow, EpisodioLifecycle, etc.)
```

## Layer Dependencies

```
┌─────────────────────────────────────────────────────────────────────────┐
│  CAPA 0: INFRAESTRUCTURA                                                │
│  ┌─────────────┐  ┌─────────────┐                                       │
│  │  Identity    │  │   Audit     │  (audit escucha DomainEvents)        │
│  └──────┬──────┘  └──────┬──────┘                                       │
├─────────┼────────────────┼──────────────────────────────────────────────┤
│  CAPA 1: SISTEMA DE RECORD (Master Data)                                │
│  ┌──────┴──────┐  ┌──────┴──────┐  ┌─────────────┐  ┌──────────────┐  │
│  │  Residence   │  │ Population  │  │    Care     │  │   Coverage   │  │
│  │ (edilicio)   │  │ (residentes)│  │  (rondas)   │  │ (turnos)     │  │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬───────┘  │
├─────────┼────────────────┼────────────────┼────────────────┼────────────┤
│  CAPA 2: CORE (Valor)                                                   │
│  ┌──────┴──────┐  ┌──────┴──────┐  ┌──────┴──────┐  ┌──────┴──────┐  │
│  │   Policy    │  │ Surveillance│  │ Observation │  │  Evidence   │  │
│  │ (DAG, perfil)│  │ (episodios) │  │ (percep→esc)│  │ (clips)     │  │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  │
│  ┌──────┴──────┐  ┌──────┴──────┐  ┌──────┴──────┐                        │
│  │   History   │  │   Streams   │  │   (care ──▶ history) │             │
│  │ (timeline)  │  │ (cámaras)   │  └─────────────┘                        │
│  └─────────────┘  └─────────────┘                                         │
└─────────────────────────────────────────────────────────────────────────┘

Arrow direction: Layer 0 → Layer 1 → Layer 2
Cross-cutting: Policy → Surveillance (perfil alimenta reglas), Observation → Surveillance (eventos disparan evaluación), Care/Observation/Evidence → History (convergen en historia clínica)
```

Every entity has `@Version version: Long` (optimistic locking, `V2__add_version_columns.sql`).

## External Component Integration — via DSL

```
                     ┌─────────────────────┐
                     │      mana-hub       │
                     │   (System of Record)│  Kotlin DSL: clients/src/main/kotlin/com/hub/clients/core/ManaHubClient.kt:22
                     └──────────┬──────────┘
                                │
           ┌────────────────────┼────────────────────┬────────────────────┐
           │                    │                    │                    │
           ▼                    ▼                    ▼                    ▼
 ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
 │ ObservationEng. │  │   SceneEngine   │  │  EpisodeEngine  │  │NotificationSvc  │
 │ (Camera/DL)     │  │ (hysteresis)    │  │ (Centinela/     │  │ (SMS/Push)      │
 │ POST /internal/ │  │ POST /internal/ │  │  Harbor rules)  │  │ POST /internal/ │
 │ v1/events       │  │ v1/scene-events │  │ POST /api/v1/   │  │ v1/notifications│
 └─────────────────┘  └─────────────────┘  │ episodes        │  └─────────────────┘
           │                    │          └─────────────────┘           │
           └────────────────────┼────────────────────┘───────────────────┘
                                ▼
                     ┌─────────────────┐        Consultores (GET /api/v1/...):
                     │EvidenceCollector │ ──▶     board, timeline, reports, care
                     │(Video/NVR)      │        Evidence: POST /api/v1/evidence|timelines|clip-windows
                     │ POST /api/v1/   │
                     └─────────────────┘
```

Consultores never write via `/internal/*`; engines never read via `/api/*` (except policy queries). `clients/src/main/kotlin/com/hub/clients/simulation/ExternalRoles.kt` enumerates roles.

## Data Consistency

- PostgreSQL `TEXT` PKs (UUID), `TIMESTAMP DEFAULT NOW()`, `BOOLEAN`, `BYTEA` for `auth_sessions.token_hash`.
- Unique partial indexes: `rooms_active_number_idx` WHERE `retired_at IS NULL`, `beds_active_monitor_idx` WHERE `monitor_key IS NOT NULL` (V3), `residents_open_assignment_idx` WHERE `ends_at IS NULL`.
- Migrations idempotent `CREATE TABLE IF NOT EXISTS` + `ADD COLUMN IF NOT EXISTS` (safe re-run).
