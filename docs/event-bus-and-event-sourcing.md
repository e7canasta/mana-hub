# Event Bus and Event Sourcing

## Purpose

This document is the source of truth for the Mana event bus after mana-hub
became the publisher of confirmed System of Record facts.

Mana-hub is not a full event-sourced database yet. PostgreSQL state remains the
queryable source of record, while domain events describe accepted changes and
are published for other components. A transactional outbox is the next step
when durable delivery is required.

## Three Meanings

```text
signal.*  = an engine reports something it observed or derived
command   = an actor requests an action
hub.*     = mana-hub confirms a persisted state change
```

The event bridge is a translator and router. It does not own business state and
does not publish a confirmation merely because it forwarded a request.

## Direction and Ownership

```text
Hive / external engines
  -> signal subjects
  -> event-bridge
  -> mana-hub internal endpoints
  -> PostgreSQL + domain event
  -> hub subjects
  -> Murmur / Cox / CRM UI / other consumers
```

For an operator action:

```text
Murmur or CRM UI
  -> nurse.episode.resolve_requested.v1
  -> event-bridge
  -> mana-hub validates and persists RESOLVED
  -> hub.episode.v1.{bedId}
```

Murmur informs Hub; Hub confirms the resulting state. Hub does not consume NATS
commands in this increment.

## Envelope

Every bus message uses the shared `EventEnvelope` from
`com.manahive:contracts`:

```json
{
  "eventId": "uuid",
  "type": "EpisodeResolved",
  "version": 1,
  "occurredAt": "2026-09-03T20:10:29Z",
  "source": "mana-hub",
  "payloadJson": "{...}"
}
```

`eventId` is the correlation and deduplication key. `occurredAt` is the domain
clock; consumers must keep it separate from their local receipt time.

## Subjects

### Inbound to Hub

| Subject | Meaning | Translator |
|---|---|---|
| `perception.observation.v1.>` | Raw perception | event-bridge |
| `scene.fact.v1.>` | Confirmed scene fact | event-bridge |
| `sentinel.signal.v1.>` | Sentinel episode signal | event-bridge |
| `nurse.>` | Operator action/request | event-bridge |

### Outbound from Hub

| Subject | Meaning | Status |
|---|---|---|
| `hub.episode.v1.{bedId}` | Confirmed episode lifecycle | `EpisodeCreated`, `EpisodeAcknowledged`, `EpisodeEscalated`, `EpisodeResolved` |
| `hub.scene.v1.{bedId}` | Confirmed persisted scene change | Contract and topology prepared; publisher pending scene domain event |
| `hub.policy.v1.>` | Confirmed policy change | Existing direction |

The `hub.*` namespace is authoritative for consumers that render or audit
current SOR state. Raw `signal.*` subjects remain useful for diagnostics and
causal investigation, but are not the final lifecycle source for Murmur.

## Episode Lifecycle

Hub will publish the lifecycle events produced by the surveillance aggregate:

```text
EpisodeCreated
EpisodeAcknowledged
EpisodeEscalated
EpisodeResolved
```

Each event must carry enough correlation data for a consumer to route it without
another synchronous Hub lookup: `episodeId`, `residentId`, `bedId`, actor/source
and the event time. A manual resolution and an automatic resolution are both
`EpisodeResolved`, but retain different provenance and cause.

## Scene Lifecycle

`hub.scene` is separate from `hub.episode`:

- `hub.scene` describes the persisted scene transition or state.
- `hub.episode` describes the persisted clinical episode lifecycle.
- A scene change may exist without creating an episode.
- An episode may be resolved manually without a new scene change.

Hub should publish `hub.scene` only after the scene event has been accepted and
persisted. Murmur can then use it for context while using `hub.episode` for the
card's clinical state.

## Consumer Rules

- Murmur consumes `hub.episode` for card lifecycle and `hub.scene` for context.
- Cox records both subjects while a WorkbenchSession is active.
- CRM UI reads Hub and may later consume the same confirmed facts.
- Consumers deduplicate by `eventId` and tolerate redelivery and reordering.
- No consumer treats a forwarded command as proof that Hub accepted it.

## Delivery Evolution

The first publisher uses a direct NATS adapter behind `nats.enabled`. A failed
publish must not roll back a successfully persisted SOR change. The production
follow-up is:

```text
same PostgreSQL transaction:
  persist state + append outbox event

dispatcher:
  read pending outbox rows
  publish to NATS
  mark delivered
  retry failures
```

This preserves the distinction between state truth in Hub and transport
availability on the bus.
