# ADR-002: Aggregate Roots

## Status
Accepted

## Context
Need to define transaction boundaries.

## Decision
Each bounded context has one or more aggregates with a root entity:

| Aggregate | Root | Children |
|-----------|------|----------|
| Residence | Facility | Wing → Room → Bed |
| Population | Resident | Assignment |
| Surveillance | Episode | EpisodeNote |

## Consequences
- Pro: Clear transaction boundaries
- Pro: Consistency within aggregate
- Con: Cross-aggregate references use IDs only
