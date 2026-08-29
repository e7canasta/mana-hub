# Conocimiento Destilado — 2026-08-29

> **Propósito:** destilar lo valioso de 44 docs viejos que ya se realizaron, archivar ruido y preservar invariantes no presentes en `api.md` / `data-model.md` / `AGENTS.md`. Auditoría completa en `Task ses_fb328a7f1ffe...` (22/44 DESTILAR, 14 ARCHIVAR, 8 MANTENER).

## Qué se archivó

| Origen | Destino | Razón |
|--------|---------|-------|
| `docs/roadmap/*` (7 archivos, `sprint-01..06` + README) | `docs/archive/roadmap/` | Planificación histórica `Progress 20%/0%` vs realidad 44 tablas + 101 endpoints + 7 blueprints compilando (`AGENTS.md`). Valor histórico BAJO. |
| `docs/data-model-mermaid.md` | `docs/archive/data-model-mermaid.md` | SQLite 34 tablas + `resident_attributes` fantasma vs `data-model.md` PostgreSQL 44 tablas. Deprecado 2026-08-29. |
| `docs/design-memory/api-design.md`, `domain-language.md`, `dsl-design.md` | Reescritos in-place (ver `docs/design-memory/*.md`) | Patrón fantasma `facilities.list()/session` vs real `manahub{ setupFacility{wing{room{bed}}} }`. |

## Qué se mantuvo (VIGENTE)

- `docs/vocabulario-unificado.md` — cadena canónica 5 términos + matriz severidad + `ESTADO SEGURO` (único con `staff_present` limitación 2+ personas)
- `docs/user-stories-director-medico.md` — 14 US agrupadas 6 épicas, MoSCoW `Must 001,002,004,007,008,010`, criterios detallados (único con priorización)
- `docs/specs/README.md` + 5 `specs/*/README.md` — template Given/When/Then + reglas `R-*` por grupo
- `docs/design-memory/ADR-001` (domain events), `ADR-002..004` destilados pero válidos
- `docs/big-picture/*` ya reescritos 2026-08-29

## Conocimiento Preservado — Top 5 (no está en `api.md`/`data-model.md`)

### 1. Invariantes de Negocio — `business-glossary-domain-model.md:340` + `specs/*/README.md:20`

> Único lugar con reglas completas. Extraído y verificado contra índices PostgreSQL.

| Código | Regla | Validación DDL/Código |
|--------|-------|------------------------|
| **R-AS-001** | Un residente solo en una cama a la vez | `UNIQUE (resident_id) WHERE ends_at IS NULL` (`V1:144`) |
| **R-AS-002** | Cama debe estar `AVAILABLE` para asignar | `UNIQUE (bed_id) WHERE ends_at IS NULL` + `beds_active_monitor_idx` |
| **R-AS-003** | Al liberar, cama vuelve a `AVAILABLE` | `DELETE /api/v1/beds/{id}/assignment` |
| **R-PF-001** | Un residente solo un perfil activo | `UNIQUE (resident_id) WHERE valid_to IS NULL` (`V1:281`) |
| **R-PF-002** | Perfil validez temporal `validFrom/To` | `alarm_profile_versions.valid_from/to` |
| **R-PF-004** | Riesgo `LOW|MEDIUM|HIGH` | `RiskLevel` (`policy/domain/model/RiskLevel.kt`) |
| **R-EP-001** | Episodio requiere `residentId` | `episodes.resident_id NOT NULL` |
| **R-EP-002** | Severidad `INFO|WARNING|CRITICAL|EMERGENCY` | `EpisodeSeverity.kt:3` |
| **R-EP-003** | `PENDING` puede `ACKNOWLEDGED` o `RESOLVED` | `surveillance/domain/model/EpisodeStatus.kt` |
| **R-EP-004** | `ACKNOWLEDGED` debe ser `RESOLVED` | state machine |
| **R-EP-005** | `RESOLVED` no modificable | — |
| **R-RN-001** | Ronda ≥1 tarea | `round_tasks` FK |
| **R-RN-002** | Solo tareas `PENDING` completables | `status CHECK pending|completed` |
| **R-RN-003** | Todas tareas `COMPLETED` → ronda `COMPLETED` | `rounds.status` |

### 2. Escenarios Gherkin — `specs/*/*.md` (Gherkin preservado, DSL corregido)

> DSL viejo `population.residents.create` fantasma → real `manahub{ population.admitResident{} }` (`clients/population/PopulationContext.kt:8`). Se conserva **Gherkin** (criterio de aceptación), se corrige **DSL** al reejecutar.

**Bed Assignment** (`specs/resident-lifecycle/bed-assignment.md:82`):
```gherkin
Given bed "A" AVAILABLE
When assigning resident to bed "A"
Then resident assigned, bed OCCUPIED, previous bed AVAILABLE
And assigning again to same bed fails (UNIQUE where ends_at IS NULL)
```

**Policy Configuration** (`specs/resident-lifecycle/policy-configuration.md:83`):
```gherkin
Given resident with no profile
When POST /api/v1/alarm-presets/{id} {riskLevel:HIGH, mobilityAid:walker, templateId:FALL_RISK}
Then GET /api/v1/alarm-presets/{id} returns riskLevel HIGH, isCurrent true
And GET /api/v1/alarm-presets/{id}/history returns 2 versions (valid_to set)
```

**Perception Ingestion** (`specs/clinical-monitoring/perception-ingestion.md:11`):
```gherkin
Given monitorKey "cam-101" linked to bed "A"
When POST /internal/v1/events {monitorKey, kind:LOCATION, state:out_of_bed}
Then sensor_events persisted + current_bed_states updated + staff_present handled
```

**Episode Lifecycle US-007/008** (`specs/clinical-monitoring/episode-lifecycle.md:6`):
```gherkin
Given pending episodes sorted by severity CRITICAL>WARNING
When POST /api/v1/episodes {severity:CRITICAL} then acknowledge → addNote → resolve
Then status PENDING→ACKNOWLEDGED→RESOLVED, episode_transitions recorded
```

Ver `docs/specs/**/*.md` para Gherkin completo; al implementar usar `clients/src/main/kotlin/com/hub/clients/*Context.kt` como referencia, no el DSL del spec.

### 3. Taxonomía Hallazgos — `vocabulario-unificado.md:166` + `specs/care-operations/clinical-notes.md:7`

> No documentado en `data-model.md` salvo `CHECK` constraints.

| Tabla | Kinds | Ejemplo |
|-------|-------|---------|
| `resident_notes` | `CARE|CLINICAL|INSIGHT|PATTERN|OBSERVATION|SUMMARY` | `INSIGHT` "patrón insomnio 2-4AM" (vocabulario:166) → `care.registerFinding()` |
| `episode_notes` | `ACKNOWLEDGEMENT|RESOLUTION|CLINICAL_NOTE` | `ACK` al `acknowledge` |
| `shift_notes` | `SHIFT_SUMMARY|INCIDENT_REPORT|GENERAL` | `SHIFT_SUMMARY` turn night |
| `care_notes` | `general` (default) |  |

`blueprints/scenarios/FindingRegistration.kt` valida `INSIGHT/PATTERN → resident_notes`.

### 4. Matriz Severidad × Confirmación — `vocabulario-unificado.md:143` + `user-stories US-007/008`

| Severidad | Confirmación | Grabación | DSL severidad |
|-----------|--------------|-----------|---------------|
| `INFO` | Virtual (panel) | No | `EpisodeSeverity.INFO` |
| `WARNING` | Virtual + video | Opcional | `WARNING` |
| `CRITICAL` | En sitio | Sí | `CRITICAL` |
| `EMERGENCY` | En sitio urgente | Sí | `EMERGENCY` |

`ESTADO SEGURO` (acostado en cama) → auto-resolución con `resolvedBy=AUTO` (deuda pendiente `vocabulario-unificado.md:205`).

### 5. Priorización MoSCoW — `user-stories-director-medico.md:214`

| Must (orden implementación real) | US | Ya hecho |
|----------------------------------|----|----------|
| **US-001 Admit** | `POST /api/v1/residents` | ✅ `PopulationContext.admitResident` |
| **US-002 Assign** | `POST /residents/{id}/assignments` | ✅ |
| **US-004 Configure profile** | `PATCH /alarm-presets/{id}` | ✅ `PolicyContext` |
| **US-007 List episodes by severity** | `GET /episodes?severity=` | ✅ `SurveillanceContext.pendingEpisodes` |
| **US-008 Acknowledge→Resolve** | `POST /acknowledge` + `PATCH` | ✅ `Episode.acknowledge().resolve()` |
| **US-010 Timeline** | `GET /residents/{id}/timeline` | ⚠️ Stub (returns `{"residentId":…}`) |

Roadmap `sprint-01..06` archivados contradecían este orden; este `MoSCoW` es fuente real de priorización.

## Cómo usar este destilado

- **Implementar nueva feature:** consultar `business-rules` arriba + Gherkin del spec correspondiente + `api.md` para endpoint real + `clients/*Context.kt` para DSL signature.
- **Actualizar spec:** reemplazar DSL fantasma `hub.residence(session).facilities.list()` por `manahub{ residence.setupFacility{wing{room{bed}}} }` (`docs/design-memory/dsl-design.md:1` reescrito). No reescribir Gherkin — es valioso tal cual.
- **Evitar fantasmas comunes:**
  - `AlarmPreset` entidad no existe → es `alarm_profile_versions` + `alarm_profile_overrides` DAG (`DagCatalog.kt:9` `STANDARD|NIGHT_WANDERING|FALL_RISK|CRITICAL`)
  - `IncidentDetection` → `history_episode_detections` (V5)
  - `facility.wings.create` → `setupFacility` declarativo
  - `resident.assignments.list()` → `GET /api/v1/residents/{id}/assignments`
  - `ResidentStatus INACTIVE/DECEASED` → `ACTIVE|DISCHARGED`

## Próximos pasos

1. Reescribir 18 specs BDD con DSL real (usar este doc como guía + `clients/*Context.kt`)
2. Regenerar `docs/archive/data-model-mermaid.md` desde `data-model.md` (Mermaid) si se necesita visual
3. Implementar `POST /api/v1/auth/login` y `GET /api/v1/evidence?episodeId=` para que `IdentityContext`/`EvidenceContext` dejen de hacer fallback
4. Completar `GET /api/v1/residents/{id}/timeline` real (hoy stub) para `US-010`
