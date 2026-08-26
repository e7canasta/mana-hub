# Architecture Overview

## System Boundaries

mana-hub is a **System of Record**, not an execution engine.

### What We Do

| Responsibility | Description |
|----------------|-------------|
| Persist data | We store residents, episodes, evidence |
| Maintain state | We know who is where, in which bed |
| Record events | We save everything that happens |
| Query information | We answer questions |
| Maintain history | We save the past |

### What We Don't Do

| Responsibility | Who Does It |
|----------------|-------------|
| Analyze video | PerceptionEngine (external) |
| Evaluate rules | EpisodeEngine (external) |
| Decide to alert | EpisodeEngine (external) |
| Send notifications | NotificationService (external) |
| Record video | EvidenceCollector (external) |

## Module Structure

```
mana-hub/
├── bootstrap/          # Application entry point, DB migrations
├── shared-kernel/      # Shared types, Identifier, DomainEvent
├── identity/           # Users, authentication, RBAC
├── residence/          # Facility, Wing, Room, Bed
├── population/         # Resident, Assignment
├── policy/             # AlarmProfile, AlarmPreset
├── surveillance/       # Episode, EpisodeNote
├── observation/        # SensorEvent, SceneEvent, Summaries
├── evidence/           # Evidence, Timeline, ClipWindow
├── care/               # Round, RoundTask, CareNote, ResidentNote, ShiftNote
├── history/            # ClinicalTimeline, IncidentDetection
├── audit/              # AuditLog
├── streams/            # Stream, StreamRegion
└── clients/            # Client DSL library
```

## Layer Dependencies

```
┌─────────────────────────────────────────────────────────────┐
│  CAPA 0: INFRAESTRUCTURA                                    │
│  ┌─────────────┐  ┌─────────────┐                           │
│  │  Identity    │  │   Audit     │                           │
│  └──────┬──────┘  └──────┬──────┘                           │
├─────────┼────────────────┼──────────────────────────────────┤
│  CAPA 1: SISTEMA DE RECORD                                  │
│  ┌──────┴──────┐  ┌──────┴──────┐  ┌─────────────┐         │
│  │  Residence   │  │ Population  │  │    Care     │         │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘         │
├─────────┼────────────────┼────────────────┼─────────────────┤
│  CAPA 2: CORE                                               │
│  ┌──────┴──────┐  ┌──────┴──────┐  ┌──────┴──────┐         │
│  │   Policy    │  │ Surveillance│  │ Observation │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│  ┌─────────────┐  ┌─────────────┐                           │
│  │  Evidence   │  │   History   │                           │
│  └─────────────┘  └─────────────┘                           │
└─────────────────────────────────────────────────────────────┘

Arrow direction: Layer 0 → Layer 1 → Layer 2
Cross-module: Policy → Surveillance, Observation → Surveillance
```

## External Component Integration

```
                    ┌─────────────────────┐
                    │      mana-hub       │
                    │   (System of Record)│
                    └──────────┬──────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ PerceptionEngine│  │ EpisodeEngine   │  │ NotificationSvc │
│ (Camera/DL)     │  │ (Rules)         │  │ (SMS/Push)      │
└─────────────────┘  └─────────────────┘  └─────────────────┘
          │                    │                    │
          └────────────────────┼────────────────────┘
                               ▼
                    ┌─────────────────┐
                    │EvidenceCollector │
                    │(Video/NVR)       │
                    └─────────────────┘
```
