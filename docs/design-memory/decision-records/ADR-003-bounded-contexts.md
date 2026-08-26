# ADR-003: Bounded Contexts

## Status
Accepted

## Context
System has multiple domains with different languages.

## Decision
Split into 10 bounded contexts:

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

## Consequences
- Pro: Clear domain boundaries
- Pro: Independent evolution
- Con: Integration complexity
