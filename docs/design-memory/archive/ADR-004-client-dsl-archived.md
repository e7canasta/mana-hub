# ADR-004: Client DSL

## Status
Accepted

## Context
External components need a typed client to interact with mana-hub.

> ⚠️ **Destilado 2026-08-29:** Firma fantasma `ManaHubClient(session).residence(session).facilities.list()` vs real `manahub(url){ residence.setupFacility{wing{room{bed}}} }` (`clients/core/ManaHubClient.kt:66`). Pattern inspiración Stripe/boto3 sí valiosa, firma corregida en `dsl-design.md` reescrito.

## Decision
Use Client → Resource → Action → Result pattern (inspired by Stripe, boto3) — firma vieja:

```kotlin
val hub = ManaHubClient("http://localhost:8080")
val session = hub.identity.login("admin", "admin123")
val facilities = hub.residence(session).facilities.list() // fantasma
```

> Real: `manahub("http://localhost:8080"){ residence.setupFacility{...} }` (`dsl-design.md` reescrito).

## Consequences
- Pro: Type-safe
- Pro: IDE autocomplete
- Pro: Fluent API
- Con: More code to maintain
