# mana-hub BDD Specs

## Context Groups

Each context group is a superset of DDD contexts sharing a domain language. BDD specs validate user stories within each group.

| # | Context Group | Contexts | Specs |
|---|---------------|----------|-------|
| 1 | [Resident Lifecycle](resident-lifecycle/README.md) | Population + Policy + Surveillance | 4 |
| 2 | [Clinical Monitoring](clinical-monitoring/README.md) | Observation + Surveillance + Evidence | 4 |
| 3 | [Care Operations](care-operations/README.md) | Care + Surveillance + Audit | 4 |
| 4 | [Facility Management](facility-management/README.md) | Residence + Streams + Identity | 4 |
| 5 | [Clinical History](clinical-history/README.md) | History + Evidence + Care | 3 |

## Spec Format

Each spec follows this structure:

```markdown
# Spec: [Name] (US-XXX)

## Context Group
**[Group Name]** — Context1 + Context2

## User Story
[As a ... I want ... So that ...]

## Acceptance Criteria
- [ ] Criterion 1
- [ ] Criterion 2

## DSL Spec (Kotlin Test)
[class ... : DescribeSpec({ ... })]

## Scenarios
### Scenario 1: [Name]
[Given/When/Then]

## API Endpoints
- [endpoint list]

## Domain Objects
- [entity list]

## Status
[emoji] Status
```
