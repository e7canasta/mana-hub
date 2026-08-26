# Context Group: Clinical Monitoring

## Contexts
- **Observation** — Perception, Scene Changes, Notifications, Summaries
- **Surveillance** — Episodes
- **Evidence** — Evidence, Timelines, Clip Windows

## Flow
```
Perception → Scene Change → Episode → Evidence
```

## Specs

| Spec | User Story | Status |
|------|------------|--------|
| [Perception Ingestion](perception-ingestion.md) | — | 📝 Pending |
| [Scene Change](scene-change.md) | — | 📝 Pending |
| [Episode Lifecycle](episode-lifecycle.md) | US-007, US-008, US-009 | 📝 Pending |
| [Evidence Collection](evidence-collection.md) | US-014 | 📝 Pending |

## Business Rules

| Rule | Description |
|------|-------------|
| R-EP-001 | An episode must have at least one resident |
| R-EP-002 | Severity must be INFO, WARNING, CRITICAL, or EMERGENCY |
| R-EP-003 | A PENDING episode can be acknowledged or resolved |
| R-EP-004 | An ACKNOWLEDGED episode must be resolved |
| R-EP-005 | A RESOLVED episode cannot be modified |
