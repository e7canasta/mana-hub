# ADR-003: Bounded Contexts

## Status
Accepted

## Context
System has multiple domains with different languages.

> ⚠️ **Destilado 2026-08-29:** Desactualizado — 10 BCs vs 12 reales (`settings.gradle.kts:22`, `docs/big-picture/context-map.md:5`). Faltan `Coverage` y `Streams`; `shared-kernel` no es BC.

## Decision
Split into 10 bounded contexts (lista vieja, ver destilado):

1. Identity
2. Audit
3. Residence
4. Population
5. Policy
6. Surveillance
7. Observation
8. Evidence
9. Care
10. History

> Reales 12: + `Coverage` (turnos) + `Streams` (cámaras). Ver `context-map.md`.

## Consequences
- Pro: Clear domain boundaries
- Pro: Independent evolution
- Con: Integration complexity
