# Context Group: Resident Lifecycle

## Contexts
- **Population** — Residents, Assignments
- **Policy** — Alarm Profiles, Presets
- **Surveillance** — Episodes

## Flow
```
Admit → Assign → Configure → Monitor
```

## Specs

| Spec | User Story | Status |
|------|------------|--------|
| [Resident Admission](resident-admission.md) | US-001 | 📝 Pending |
| [Bed Assignment](bed-assignment.md) | US-002 | 📝 Pending |
| [Policy Configuration](policy-configuration.md) | US-004, US-006 | 📝 Pending |
| [Resident Discharge](resident-discharge.md) | — | 📝 Pending |

## Business Rules

| Rule | Description |
|------|-------------|
| R-AS-001 | A resident can only be in one bed at a time |
| R-AS-002 | Bed must be AVAILABLE to assign |
| R-AS-003 | On release, bed returns to AVAILABLE |
| R-PF-001 | A resident can have only one active profile |
| R-PF-002 | Profile has temporal validity (validFrom, validTo) |
| R-PF-003 | Overrides must be valid JSON |
| R-PF-004 | Risk must be LOW, MEDIUM, or HIGH |
