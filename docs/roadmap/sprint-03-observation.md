# Sprint 03: Observation + Evidence

**Duration:** 2 weeks  
**Goal:** Event ingestion and evidence management

## User Stories

| ID | Story | Status |
|----|-------|--------|
| — | Ingest sensor events | ✅ Done |
| — | Compute clinical summaries | 📝 Pending |
| US-014 | Ver evidencia de un evento | 📝 Pending |

## Deliverables

### Observation
- [x] `SensorEvent` — Domain entity + API
- [x] `CurrentBedState` — Domain entity + API
- [x] `SceneEvent` — Domain entity + API
- [ ] `SleepSummary` — Domain entity + API
- [ ] `MobilitySummary` — Domain entity + API
- [ ] `BathroomSummary` — Domain entity + API
- [x] `NotificationEvent` — Domain entity + API

### Evidence
- [ ] `Evidence` — Domain entity + API
- [ ] `Timeline` — Domain entity + API
- [ ] `ClipWindow` — Domain entity + API

### BDD Spec
- [ ] `perception-ingestion.md` — Sensor event spec
- [ ] `scene-change.md` — Scene change spec
- [ ] `evidence-collection.md` — US-014 spec

## Definition of Done

- [ ] Unit tests pass
- [ ] API documented
- [ ] SQL migration executes
- [ ] Blueprint `NightShift` runs
