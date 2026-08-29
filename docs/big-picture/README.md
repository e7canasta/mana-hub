# Big Picture

## What is mana-hub?

mana-hub is the **System of Record** (SOR) for adult residential care monitoring. We are the persistent memory where all components query and register data.

```
┌─────────────────────────────────────────────────────────────────┐
│  EXTERNAL (not us)                 MANA-HUB (us)               │
│  ─────────────────────              ──────────────────────      │
│  The one that THINKS                The one that REMEMBERS      │
│  The one that DETECTS               The one that PERSISTS       │
│  The one that DECIDES               The one that QUERIES        │
└─────────────────────────────────────────────────────────────────┘
```

Stack real: Kotlin 2.4.20-RC + Spring Boot 4.0.1 + PostgreSQL 17 (`gradle/libs.versions.toml:2`, `application.yml:8`).

## Architecture Layers

```
╔══════════════════════════════════════════════════════════════════╗
║  LAYER 0: INFRASTRUCTURE (Support)                              ║
║  ┌─────────────┐  ┌─────────────┐                               ║
║  │  Identity    │  │   Audit     │                               ║
║  │  (Auth/RBAC) │  │  (Log)      │  2 tables                     ║
║  └─────────────┘  └─────────────┘                               ║
╠══════════════════════════════════════════════════════════════════╣
║  LAYER 1: SYSTEM OF RECORD (Master Data)                        ║
║  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌──────────┐║
║  │  Residence   │  │ Population  │  │    Care     │  │ Coverage │║
║  │  (edilicio)  │  │ (Residents) │  │  (Rounds)   │  │ (Turnos) │║
║  │  6 tables    │  │  2 tables   │  │  7 tables   │  │ 4 tables │║
║  └─────────────┘  └─────────────┘  └─────────────┘  └──────────┘║
╠══════════════════════════════════════════════════════════════════╣
║  LAYER 2: CORE (Value)                                          ║
║  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌──────────┐║
║  │   Policy    │  │ Surveillance│  │ Observation │  │ Evidence │║
║  │ 2 tables    │  │ 5 tables    │  │ 8 tables    │  │ 3 tables │║
║  └─────────────┘  └─────────────┘  └─────────────┘  └──────────┘║
║  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐               ║
║  │   History   │  │   Streams   │  │  (44 tables total, PostgreSQL)│
║  │  3 tables   │  │  2 tables   │  └─────────────┘               ║
║  └─────────────┘  └─────────────┘                               ║
╚══════════════════════════════════════════════════════════════════╝
```

See detail: `architecture-overview.md` (16 modules, `settings.gradle.kts:22-37`) and `context-map.md`.

## Data Flow — `data-flow.md`

```
CÁMARA → ObservationEngine → POST /internal/v1/events → sensor_events + current_bed_states
                                    ↓
                        SceneEngine → POST /internal/v1/scene-events → scene_events
                                    ↓
  ┌─────────────────────┬───────────┴───────────┬─────────────────────┐
  │  solo visual        │  requiere atención     │  hallazgo clínico   │
  │  NotificationSvc    │  EpisodeEngine         │  experto/ML         │
  │  POST /internal/v1/ │  POST /api/v1/episodes │  POST /api/v1/residents/{id}/notes (INSIGHT/PATTERN)
  │  notifications      │  → episodes            │  → resident_notes   │
  └─────────────────────┴────────────────────────┴─────────────────────┘
                                    ↓
                        EvidenceCollector → POST /api/v1/evidence|timelines|clip-windows
                                    ↓
                        Consultores → GET /api/v1/wings/{id}/board|residents/{id}/timeline|reports/summary
```

Resúmenes diarios: `POST /internal/v1/clinical/*-summaries` + `POST /internal/v1/care-summaries` (see `api.md`).

## Modules & DSL

16 Gradle modules (`shared-kernel`, `identity`, `audit`, `residence`, `population`, `coverage`, `care`, `history`, `policy`, `surveillance`, `observation`, `evidence`, `streams`, `bootstrap`, `clients`, `blueprints`). DSL in `clients/src/main/kotlin/com/hub/clients/core/ManaHubClient.kt:22` with 11 scopes — see `AGENTS.md` and `api.md`.

## Documents

- [Architecture Overview](architecture-overview.md) — Detailed architecture + external roles (5) + layer deps
- [Context Map](context-map.md) — DDD context map (12 bounded contexts)
- [Domain Model](domain-model.md) — Entities (44 tables) + aggregates + enums
- [Data Flow](data-flow.md) — Event chain: Percepción → Escena → Episodio → Historia (8 phases, vocabulario-unificado.md)

Source of truth for schema: `data-model.md` (PostgreSQL, Flyway V1-V10). For endpoints: `api.md` (100+ Spring MVC mappings).
