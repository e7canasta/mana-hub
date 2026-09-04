# event-bridge — Architecture

## What is this?

event-bridge is a thin NATS-to-HTTP router for external Hive-originated
integration. It does not own care interactions from Murmur; mana-hub consumes
`nurse.>` directly because Hub owns the episode and care context.

**It does NOT:**
- Store anything
- Transform payloads
- Make business decisions
- Have its own database

**It DOES:**
- Subscribe to 7 NATS subject families
- Route each to the correct mana-hub integration endpoint
- Log every forward attempt

## Topology

```
                           NATS JetStream
                    (nats://localhost:4222)
                              │
              ┌───────────────┼───────────────┐
              │               │               │
          mana-hive         mana-hub        murmur
          (engines)         (SOR)          (station)
               │               ▲              │
               │               │              │
               ▼               │              │
          event-bridge         │              │
          Hive -> HTTP         │              │
                               └── nurse.* ───┘
```

## Subject routing

| NATS Subject | Mana-hub endpoint | Purpose |
|---|---|---|
| `scene.fact.v1.>` | `/internal/v1/integration/scene-events` | Scene transitions from SceneEngine |
| `sentinel.signal.v1.>` | `/internal/v1/integration/signal-events` | Episode lifecycle from EpisodeEngine |
| `nurse.>` | (consumed directly by Hub) | Care interactions from Murmur station |
| `perception.observation.v1.>` | (skipped) | Raw sensor data — too high volume for hub |
| `alarm.event.v1.>` | (skipped) | Notification dispatch — handled by harbor |
| `recorder.command.v1.>` | (skipped) | Recording commands — handled by recorder |
| `evidence.record.v1.>` | (skipped) | Evidence records — handled by recorder |

## Key files

| File | Role |
|------|------|
| `src/main/kotlin/.../ingest/NatsIngestService.kt` | NATS subscription setup (7 subjects) |
| `src/main/kotlin/.../ingest/EventRouter.kt` | Routes subject → HTTP endpoint |
| `src/main/kotlin/.../api/BridgeController.kt` | Health check (`GET /api/v1/bridge/health`) |

## Nurse event flow (new)

```
murmur: nurse taps "Ya está"
  → JetStreamPublisher.publish_resolved()
  → NATS subject: nurse.resolved.v1.{episodeId}
  → mana-hub durable care consumer
  → NurseEventService.processNurseEvent()
  → EpisodePort.resolveEpisode()
  → episode status → RESOLVED
  → care interaction + episode timeline + audit trail
```

## Configuration

The bridge reads its NATS connection from Spring config:

```yaml
# application.yml
nats:
  url: nats://localhost:4222
  enabled: true
```

## Adding a new subject

1. Add constant in `NatsIngestService.kt` companion object
2. Add `subscribeTo(SUBJECT, "name")` in `init()`
3. Add routing case in `EventRouter.route()`
4. Create endpoint in `IntegrationController` if needed
