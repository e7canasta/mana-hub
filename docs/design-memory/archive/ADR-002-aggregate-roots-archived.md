# ADR-002: Aggregate Roots

> ⚠️ **Destilado 2026-08-29:** Incompleto — lista 3 aggregates vs 12 reales (`docs/big-picture/domain-model.md:120`). Falta `Care Round→RoundTask`, `Policy AlarmProfileVersions→Overrides`, `History HistoryEpisode→Reviews`, `Coverage StaffGroup`, `Streams Stream→Region`, `Observation SensorEvent`, etc. Ver fuente real.

## Status
Accepted

## Context
Need to define transaction boundaries.

## Decision
Each bounded context has one or more aggregates with a root entity (lista parcial vieja):

| Aggregate | Root | Children |
|-----------|------|----------|
| Residence | Facility | Wing → Room → Bed |
| Population | Resident | Assignment |
| Surveillance | Episode | EpisodeNote |

> Ver tabla completa 12 roots en `docs/big-picture/domain-model.md:120`.

## Consequences
- Pro: Clear transaction boundaries
- Pro: Consistency within aggregate
- Con: Cross-aggregate references use IDs only
