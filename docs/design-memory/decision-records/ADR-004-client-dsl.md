# ADR-004: Client DSL

## Status
Accepted

## Context
External components need a typed client to interact with mana-hub.

## Decision
Use Client → Resource → Action → Result pattern (inspired by Stripe, boto3):

```kotlin
val hub = ManaHubClient("http://localhost:8080")
val session = hub.identity.login("admin", "admin123")
val facilities = hub.residence(session).facilities.list()
```

## Consequences
- Pro: Type-safe
- Pro: IDE autocomplete
- Pro: Fluent API
- Con: More code to maintain
