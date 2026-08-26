# Sprint 02: Policy + Surveillance

**Duration:** 2 weeks  
**Goal:** Monitoring configuration and episode management

## User Stories

| ID | Story | Status |
|----|-------|--------|
| US-004 | Configurar perfil de monitoreo | 📝 Pending |
| US-005 | Ver catálogo de presets | 📝 Pending |
| US-006 | Cambiar perfil de monitoreo | 📝 Pending |
| US-007 | Ver episodios pendientes | ✅ Done |
| US-008 | Revisar y resolver episodio | ✅ Done |
| US-009 | Ver episodios de un residente | 📝 Pending |

## Deliverables

### Policy
- [ ] `AlarmPreset` — Domain entity + API
- [ ] `AlarmProfile` — Domain entity + API
- [ ] `AlarmPresetCatalog` — Query endpoint
- [ ] `ProfileHistory` — Version history

### Surveillance
- [x] `Episode` — Domain entity + API
- [x] `EpisodeNote` — Domain entity + API
- [ ] `EpisodeFilter` — Filter by severity, resident, date

### BDD Spec
- [ ] `policy-configuration.md` — US-004, US-006 spec
- [ ] `episode-lifecycle.md` — US-007, US-008, US-009 spec

## Definition of Done

- [ ] Unit tests pass
- [ ] API documented
- [ ] SQL migration executes
- [ ] Blueprint `FallDetection` runs
