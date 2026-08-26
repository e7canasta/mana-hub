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

## Architecture Layers

```
╔══════════════════════════════════════════════════════════════════╗
║  LAYER 0: INFRASTRUCTURE (Support)                              ║
║  ┌─────────────┐  ┌─────────────┐                               ║
║  │  Identity    │  │   Audit     │                               ║
║  │  (Auth/RBAC) │  │  (Log)      │                               ║
║  └─────────────┘  └─────────────┘                               ║
╠══════════════════════════════════════════════════════════════════╣
║  LAYER 1: SYSTEM OF RECORD (Master Data)                        ║
║  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             ║
║  │  Residence   │  │ Population  │  │    Care     │             ║
║  │  (CRM-like)  │  │ (Residents) │  │  (Rounds)   │             ║
║  └─────────────┘  └─────────────┘  └─────────────┘             ║
╠══════════════════════════════════════════════════════════════════╣
║  LAYER 2: CORE (Value)                                          ║
║  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             ║
║  │   Policy    │  │ Surveillance│  │ Observation │             ║
║  │ (Profiles)  │  │ (Episodes)  │  │  (Events)   │             ║
║  └─────────────┘  └─────────────┘  └─────────────┘             ║
║  ┌─────────────┐  ┌─────────────┐                               ║
║  │  Evidence   │  │   History   │                               ║
║  │  (Clips)    │  │ (Timeline)  │                               ║
║  └─────────────┘  └─────────────┘                               ║
╚══════════════════════════════════════════════════════════════════╝
```

## Data Flow

```
CAMERA → PerceptionEngine → mana-hub (persists) → Consultants
                                    ↑
                      EpisodeEngine → mana-hub (episodes)
                                    ↑
                   NotificationService → mana-hub (notifications)
                                    ↑
                  EvidenceCollector → mana-hub (evidence)
```

## Context Groups

See [specs/README.md](../specs/README.md) for the complete BDD specification index.

## Documents

- [Architecture Overview](architecture-overview.md) — Detailed architecture
- [Context Map](context-map.md) — DDD context map
- [Domain Model](domain-model.md) — Entities and relationships
- [Data Flow](data-flow.md) — Event chain: Perception → Scene → Episode → History
