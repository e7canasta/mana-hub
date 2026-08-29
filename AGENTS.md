# mana-hub — System of Record (SOR)

## Arquitectura

Mana-hub es el **System of Record** del dominio de monitoreo de residencias de adultos mayores.

**No somos** el motor que piensa, detecta o decide.
**Somos** la memoria persistente donde todos consultan y registran.

```
┌─────────────────────────────────────────────────────────────────┐
│  EXTERNO (no nosotros)              MANA-HUB (nosotros)        │
│  ─────────────────────              ──────────────────────      │
│  El que PIENSA                      El que RECUERDA             │
│  El que DETECTA                     El que PERSISTE             │
│  El que DECIDE                      El que CONSULTA             │
└─────────────────────────────────────────────────────────────────┘
```

> Stack real: **Kotlin 2.4.20-RC + Spring Boot 4.0.1 + PostgreSQL 17** — ver `gradle/libs.versions.toml:2-3` y `bootstrap/src/main/resources/application.yml:8`. No es Rust/Diesel/SQLite.

### Módulos Gradle (16) — `settings.gradle.kts:22-37`

```
shared-kernel  → Kernel compartido (Identifier, Entity, DomainEvent)
identity       → Usuarios y sesiones
audit          → Trail de auditoría
residence      → Facility → Wing → Room → Bed + planograma + privacidad
population     → Residentes y asignaciones
coverage       → StaffGroups, turnos y cobertura por ala
care           → Rondas, tareas, notas y resúmenes de cuidado
history        → history_episode_detections/reviews/interventions
policy         → alarm_profile_versions + alarm_profile_overrides (DAG)
surveillance   → episodes + transitions + deliveries + escalations
observation    → sensor_events, current_bed_states, scene_events, summaries
evidence       → evidence, timelines, clip_windows
streams        → streams + stream_regions
bootstrap      → App + Flyway V1-V10 + config (integra todos)
clients        → DSL tipado Kotlin (11 contextos)
blueprints     → Escenarios ejecutables (7 blueprints)
```

### Componentes Externos (roles, no implementaciones) — `clients/src/main/kotlin/com/hub/clients/simulation/ExternalRoles.kt`

| Rol | Responsabilidad | DSL que usa |
|-----|----------------|-------------|
| `ObservationEngine` | Detecta cambios de estado en escenas/cámaras | `observation.registerPerception` → `POST /internal/v1/events` |
| `SceneEngine` | Confirma transiciones con hysteresis | `observation.registerSceneChange` → `POST /internal/v1/scene-events` |
| `EpisodeEngine` | Evalúa reglas contra `alarm_profile` y dispara episodios | `surveillance.registerEpisode` → `POST /api/v1/episodes` |
| `NotificationService` | Envía notificaciones (SMS, push, email) | `observation.notifyInformational` → `POST /internal/v1/notifications` |
| `EvidenceCollector` | Recopila evidencia (video, clips, fotos) | `evidence.*` → `POST /api/v1/evidence|timelines|clip-windows` |

Consultores (paneles) solo leen vía `GET /api/v1/...` (board, timeline, reports).

### Lo que SÍ hacemos — persistimos 44 tablas (PostgreSQL, `bootstrap/src/main/resources/db/migration/V1__init.sql:5` y V2-V10)

| Contexto | Tablas | Descripción |
|----------|--------|-------------|
| ctx-identidad | `users`, `auth_sessions` | Usuarios y sesiones |
| ctx-auditoria | `audit_log` | Trail inmutable |
| ctx-residencia | `facilities`, `wings`, `rooms`, `beds`, `planogram_placements`, `room_privacy_regions` | Jerarquía edilicia + layout |
| ctx-poblacion | `residents`, `resident_bed_assignments` | Residentes y camas asignadas |
| ctx-cobertura | `staff_groups`, `facility_shifts`, `unit_shift_coverages`, `staff_members` | Grupos, turnos y cobertura |
| ctx-cuidado | `rounds`, `round_tasks`, `care_notes`, `resident_notes`, `episode_notes`, `shift_notes`, `care_summaries` | Rondas y notas + hallazgos |
| ctx-historia | `history_episode_detections`, `history_episode_reviews`, `history_episode_interventions` | Incidentes normalizados |
| ctx-politica | `alarm_profile_versions`, `alarm_profile_overrides` | Versionado de perfiles + DAG overrides |
| ctx-vigilancia | `episodes`, `episode_transitions`, `notification_deliveries`, `notification_delivery_events`, `episode_escalations` | Episodios y entregas |
| ctx-evidence | `evidence`, `timelines`, `clip_windows` | Evidencia clínica |
| ctx-streams | `streams`, `stream_regions` | Cámara y regiones espaciales |
| mana-observation | `sensor_events`, `current_bed_states` (+`staff_present` V10), `scene_events`, `notification_events`, `sleep_summaries` (+`started_at/ended_at` V8), `mobility_summaries`, `bathroom_summaries` | Percepciones y resúmenes |

> Nota: `resident_attributes` y `staff_group_members` que aparecen en docs viejos nunca existieron en migraciones — eran fantasmas. Ver `data-model.md`.

### Lo que NO hacemos

- Analizar video o imágenes
- Ejecutar reglas de negocio (lo hace `EpisodeEngine` externo)
- Decidir si un episodio se dispara
- Enviar notificaciones
- Procesar clips de video

### El DSL como Contrato — `clients/src/main/kotlin/com/hub/clients/core/ManaHubClient.kt:22`

El DSL es la **interfaz de contrato** que los componentes externos usan. 11 scopes tipados:

```
manahub("http://localhost:8080") {
  identity { createUser / listUsers }
  residence { setupFacility / wings / rooms / beds }
  population { admitResident / assignTo / discharge }
  streams { setupStream / defineRegions }
  policy { catalog / configureAlarmProfile / alarmProfileHistory }
  surveillance { registerEpisode / acknowledge / resolve }
  observation { registerPerception / registerSceneChange / ingest*Summary / wingBoard }
  evidence { createEvidence / openTimeline / openClipWindow }
  care { startRound / addResidentNote / registerFinding / addShiftNote }
  history { residentHistoryEpisodes / reviewHistoryEpisode }
  audit { queryLog }
}
```

Cada scope valida contra endpoints reales — ver `api.md`. Vocabulario canónico en `docs/vocabulario-unificado.md` (percepción → cambio de escena → notificación/episodio → hallazgo).

### Flujo de Datos — `docs/big-picture/data-flow.md`

```
CÁMARA → ObservationEngine → POST /internal/v1/events → sensor_events + current_bed_states
                                    ↓
                        SceneEngine → POST /internal/v1/scene-events → scene_events
                                    ↓
  ┌─────────────────────┬───────────┴───────────┬─────────────────────┐
  │  solo visual        │  requiere atención     │  hallazgo clínico   │
  │  NotificationSvc    │  EpisodeEngine         │  experto/ML         │
  │  POST /internal/v1/ │  POST /api/v1/episodes │  POST /api/v1/residents/{id}/notes (INSIGHT/PATTERN)
  │  notifications      │  → episodes            │  → resident_notes   │
  └─────────────────────┴────────────────────────┴─────────────────────┘
                                    ↓
                        EvidenceCollector → POST /api/v1/evidence|timelines|clip-windows
                                    ↓
                        Consultores → GET /api/v1/wings/{id}/board|residents/{id}/timeline|reports/summary
```

Capa observación también ingesta resúmenes diarios `POST /internal/v1/clinical/*-summaries` y `POST /internal/v1/care-summaries` (ver `data-model.md` y `api.md:internal`).

### Migraciones

- Ubicación: `bootstrap/src/main/resources/db/migration/V1__init.sql:1` y `V2__add_version_columns.sql:1` .. `V10__staff_present_in_bed_state.sql:1` (12 archivos, Flyway).
- Todas las entidades JPA llevan `@Version version: Long` (optimistic locking, V2).
- Base: PostgreSQL — tipos `TIMESTAMP`, `BOOLEAN`, `BYTEA`, `NOW()` (no SQLite).

### Convenciones

- Endpoints: `/api/v1/*` para clientes panel, `/internal/v1/*` para M2M (motores). Ver `api.md`.
- DSL primero: todo caso de uso real debe tener blueprint en `blueprints/src/main/kotlin/com/hub/blueprints/scenarios/` y spec en `docs/specs/`.
- Docs vivos: `docs/big-picture/*` es la vista de 30k pies; `data-model.md` es espejo del DDL; `api.md` es espejo de los `@RequestMapping` reales.
