# Sprint 01: Core + Residence + Population

**Duration:** 2 weeks  
**Goal:** Infrastructure base and master data

## User Stories

| ID | Story | Status |
|----|-------|--------|
| US-001 | Admitir un nuevo residente | ✅ Done |
| US-002 | Asignar residente a cama | ✅ Done |
| — | Configurar facility hierarchy | ✅ Done |

## Deliverables

### Core
- [x] `HttpApi` — HTTP transport layer
- [x] `Session` — Authenticated session
- [x] `ManaHubClient` — Entry point
- [x] `@DslMarker` annotations

### Residence
- [x] `Facility` — Domain entity + API
- [x] `Wing` — Domain entity + API
- [x] `Room` — Domain entity + API
- [x] `Bed` — Domain entity + API
- [x] `FacilityTree` — Hierarchical query

### Population
- [x] `Resident` — Domain entity + API
- [x] `Assignment` — Domain entity + API
- [x] `ResidentBedAssignment` — Join table

### BDD Spec
- [ ] `resident-admission.md` — US-001 spec
- [ ] `bed-assignment.md` — US-002 spec

## Definition of Done

- [ ] Unit tests pass
- [ ] API documented (OpenAPI)
- [ ] SQL migration executes
- [ ] Blueprint `ResidentOnboarding` runs

## Notes

- Domain model aligned with vocabulario-unificado.md
- All code in English, aligned to Spanish canonical vocabulary semantically
