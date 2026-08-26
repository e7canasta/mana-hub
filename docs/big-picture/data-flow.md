# Data Flow

## The Canonical Event Chain

```
┌─────────────┐    ┌───────────────┐    ┌──────────────┐    ┌───────────┐
│ PERCEPTION  │───▶│ SCENE CHANGE  │───▶│   EPISODE    │───▶│ HISTORY   │
│ (raw)       │    │ (confirmed)   │    │  (if rule)   │    │           │
└─────────────┘    └───────────────┘    └──────────────┘    └───────────┘
     │                   │                    │                  ▲
     │                   │                    │                  │
     │                   ├──▶ NOTIFICATION    │    ┌───────────┐ │
     │                   │    (visual only,   │    │  FINDING  │─┘
     │                   │    no episode)     │    │ (clinical │
     │                   │                    │    │  insight) │
     ▼                   ▼                    ▼    └───────────┘
  Stored             Engine               Sentinel +
  in mana-hub        decides              Harbor decide
```

## Phase 1: Perception Ingestion

```
Camera/DL → POST /internal/v1/events → SensorEvent → current_bed_states
```

1. PerceptionEngine detects state change
2. Calls `POST /internal/v1/events` with `IngestEventRequest`
3. mana-hub persists `SensorEvent`
4. Updates `CurrentBedState` for the bed

## Phase 2: Scene Change

```
PerceptionEngine → SceneEvent → Notification (if visual only)
                               → Episode (if rule triggers)
```

1. PerceptionEngine confirms state transition (hysteresis applied)
2. Creates `SceneEvent` with from/to states
3. If policy says "visual only" → `POST /internal/v1/notifications`
4. If policy says "open episode" → EpisodeEngine evaluates

## Phase 3: Episode Lifecycle

```
EpisodeEngine → POST /api/v1/episodes → Episode (PENDING)
                                    ↓
              NotificationService → staff alerted
                                    ↓
              EvidenceCollector → clips attached
                                    ↓
              Nurse → acknowledge → resolve
```

1. EpisodeEngine evaluates rules against alarm profile
2. If threshold exceeded → creates Episode via `POST /api/v1/episodes`
3. NotificationService alerts staff
4. EvidenceCollector opens clip window
5. Nurse acknowledges (`POST /api/v1/episodes/{id}/acknowledge`)
6. Nurse resolves (`PATCH /api/v1/episodes/{id}`)

## Phase 4: Clinical Summaries

```
PerceptionEngine → POST /internal/v1/clinical/*-summaries → SleepSummary
                                                           → MobilitySummary
                                                           → BathroomSummary
```

1. PerceptionEngine computes daily summaries
2. Posts to mana-hub internal endpoints
3. Summaries stored for clinical history

## Phase 5: Finding (Hallazgo)

```
Expert/ML → POST /api/v1/residents/{id}/notes → ResidentNote
```

1. Expert reviews data patterns
2. Creates `ResidentNote` with kind INSIGHT or PATTERN
3. Finding stored in resident's clinical history

## Full Day Cycle

```
06:00  Night shift ends, shift note created
08:00  Day shift starts, round created
08:30  Nurse visits Room 101, care note added
12:00  Lunch, resident in common area
14:00  Resident returns to room, scene change
18:00  Evening round, care note added
22:00  Night shift starts
02:00  Resident gets up, perception detected
02:05  Scene change confirmed, episode opened (WARNING)
02:10  Nurse acknowledges, goes to room
02:15  Resident back in bed, episode auto-resolves
06:00  Night shift ends, shift note created
       Daily summaries computed
```
