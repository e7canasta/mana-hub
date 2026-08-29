# API Reference

> Source of truth: `@RequestMapping` in Kotlin Spring MVC (`bootstrap/src/main/resources/application.yml:8`, `springdoc` at `/api-docs`). Generated 2026-08-29 by `grep -rn "@.*Mapping" --include="*.kt"`.

| Prefix | Audience | Auth |
|--------|----------|------|
| `/api/v1/*` | Panel / console (human) | `permitAll()` today (`bootstrap/src/main/kotlin/com/hub/config/SecurityConfig.kt:16`) — TODO RBAC |
| `/internal/v1/*` | M2M (motores) | `permitAll()` — TODO mTLS/api-key |

Stack: **Kotlin 2.4.20-RC + Spring Boot 4.0.1 + Spring Web MVC** (no `rutas.toml`, no Rust). Ver `gradle/libs.versions.toml:2-3`.

---

## 1. Identity (4) — `identity/api/rest/UserController.kt:16`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/users` | Create user |
| GET | `/api/v1/users` | List users |
| GET | `/api/v1/users/{userId}` | Get user |
| PATCH | `/api/v1/users/{userId}` | Update user (role, displayName, retire) |

> **No auth endpoints yet:** `POST /api/v1/auth/login|logout`, `GET /api/v1/auth/me` no existen — login es stub. Ver gap en `docs/big-picture/data-flow.md`.

## 2. Audit (1) — `audit/api/rest/AuditController.kt:9`

| Method | Path |
|--------|------|
| GET | `/api/v1/audit-log` |

## 3. Residence (18) — `residence/api/rest/FacilityController.kt:14`

| Method | Path |
|--------|------|
| POST | `/api/v1/facilities` |
| GET | `/api/v1/facilities` |
| GET | `/api/v1/facilities/{facilityId}` |
| PATCH | `/api/v1/facilities/{facilityId}` |
| GET | `/api/v1/facilities/{facilityId}/tree` |
| POST | `/api/v1/facilities/{facilityId}/wings` |
| POST | `/api/v1/wings/{wingId}/rooms` |
| PATCH | `/api/v1/wings/{wingId}` |
| GET | `/api/v1/wings/{wingId}/rooms` |
| GET | `/api/v1/wings/{wingId}/planogram` |
| PUT | `/api/v1/wings/{wingId}/planogram` |
| POST | `/api/v1/rooms/{roomId}/beds` |
| PATCH | `/api/v1/rooms/{roomId}` |
| GET | `/api/v1/rooms/{roomId}/beds` |
| GET | `/api/v1/rooms/{roomId}/privacy-regions` |
| PUT | `/api/v1/rooms/{roomId}/privacy-regions` |
| PATCH | `/api/v1/beds/{bedId}` |
| GET | `/api/v1/beds` |

## 4. Population (9) — `population/api/rest/ResidentController.kt:13`

| Method | Path |
|--------|------|
| POST | `/api/v1/residents` |
| GET | `/api/v1/residents` |
| GET | `/api/v1/residents/{residentId}` |
| PATCH | `/api/v1/residents/{residentId}` |
| POST | `/api/v1/residents/{residentId}/discharge` |
| POST | `/api/v1/residents/{residentId}/assignments` |
| GET | `/api/v1/residents/{residentId}/assignments` |
| GET | `/api/v1/assignments/open` |
| DELETE | `/api/v1/beds/{bedId}/assignment` |

## 5. Coverage (5) — `coverage/api/rest/StaffGroupController.kt:13`

| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/v1/facilities/{facilityId}/staff-groups` | Create staff group |
| GET | `/api/v1/staff-groups?facilityId=` | List (requires facilityId) |
| GET | `/api/v1/staff-groups/{groupId}` | Detail |
| POST | `/api/v1/facilities/{facilityId}/shifts` | Create shift |
| GET | `/api/v1/facilities/{facilityId}/shifts` | List shifts |

> **Gaps vs. diseño viejo (no implementado aún):** `PUT /facilities/{id}/shifts`, `GET|PUT /wings/{id}/coverage`, `PUT /staff-groups/{id}/members`, `PATCH /staff-groups/{id}`. Tabla `unit_shift_coverages` existe en DDL pero sin controller.

## 6. Care (16) — `care/api/rest/*`

### Rounds — `RoundController.kt:11`

| Method | Path |
|--------|------|
| POST | `/api/v1/rounds` |
| GET | `/api/v1/rounds/current?wingId=` |
| GET | `/api/v1/rounds?wingId=` |
| GET | `/api/v1/rounds/{roundId}` |
| PATCH | `/api/v1/rounds/{roundId}` |
| PATCH | `/api/v1/round-tasks/{taskId}` |

### Notes — `NoteController.kt:11`

| Method | Path | Kind check |
|--------|------|------------|
| POST | `/api/v1/residents/{residentId}/notes` | `CARE,CLINICAL,INSIGHT,PATTERN,OBSERVATION,SUMMARY` |
| GET | `/api/v1/residents/{residentId}/notes` | — |
| POST | `/api/v1/episodes/{episodeId}/notes` | `ACKNOWLEDGEMENT,RESOLUTION,CLINICAL_NOTE` |
| GET | `/api/v1/episodes/{episodeId}/notes` | — |
| POST | `/api/v1/shift-notes` | `SHIFT_SUMMARY,INCIDENT_REPORT,GENERAL` |
| GET | `/api/v1/facilities/{facilityId}/shift-notes?shiftDate=` | — |
| GET | `/api/v1/wings/{wingId}/shift-notes?shiftDate=` | — |

### Care Summaries — `CareSummaryController.kt:10`

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/residents/{residentId}/care?from=&to=` | 14-day window default |
| POST | `/internal/v1/care-summaries` | **Canonical** (fixed 2026-08-29, was `/api/v1/internal/care-summaries`) |
| POST | `/api/v1/internal/care-summaries` | Legacy alias, keep for BC |

## 7. History (5) — `history/api/rest/HistoryEpisodeController.kt:11`

| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/v1/history-episodes` | Ingest (was `POST /internal/v1/clinical/incidents` stub) |
| GET | `/api/v1/residents/{residentId}/history-episodes` | List |
| GET | `/api/v1/history-episodes/{episodeId}/sequence` | Reviews |
| PATCH | `/api/v1/history-episodes/{episodeId}` | Review (status, verdict, note) |
| GET | `/api/v1/residents/{residentId}/falls?months=12` | Falls summary |

> **Renombrado:** `incidents` → `history-episodes` en V5. `POST /internal/v1/clinical/incidents` sigue existiendo como stub `201` sin persistencia (`EventIngestionController.kt:41`).

## 8. Policy (5) — `policy/api/rest/AlarmProfileController.kt:10` (`/api/v1/alarm-presets`)

| Method | Path                                         |
| ------ | -------------------------------------------- |
| GET    | `/api/v1/alarm-presets/catalog`              |
| GET    | `/api/v1/alarm-presets`                      |
| GET    | `/api/v1/alarm-presets/{residentId}`         |
| PATCH  | `/api/v1/alarm-presets/{residentId}`         |
| GET    | `/api/v1/alarm-presets/{residentId}/history` |

> Gaps: `POST /alarm-presets/apply-recommendations`, `POST /autopilot`, `POST /{id}/apply-recommendation` no existen aún (DAG overrides vía `alarm_profile_overrides` tabla V9).

## 9. Surveillance (5) — `surveillance/api/rest/EpisodeController.kt:11` (`/api/v1/episodes`)

| Method | Path |
|--------|------|
| POST | `/api/v1/episodes` |
| GET | `/api/v1/episodes` |
| GET | `/api/v1/episodes/{episodeId}` |
| POST | `/api/v1/episodes/{episodeId}/acknowledge` |
| PATCH | `/api/v1/episodes/{episodeId}` |

> Tablas `notification_deliveries` / `episode_escalations` huérfanas sin controller (no `GET /episodes/{id}/deliveries`).

## 10. Evidence (6) — `evidence/api/rest/EvidenceController.kt:11`

| Method | Path |
|--------|------|
| POST | `/api/v1/evidence` |
| POST | `/api/v1/timelines` |
| POST | `/api/v1/timelines/{timelineId}/close` |
| POST | `/api/v1/clip-windows` |
| POST | `/api/v1/clip-windows/{windowId}/close` |
| GET | `/api/v1/clip-windows/{bedId}/open` |

> Antes estaban bajo `/internal/v1` — migradas a `/api/v1` en código. `GET /internal/v1/evidence|timelines` no existe.

## 11. Streams (6) — `streams/api/rest/StreamController.kt:11`

| Method | Path |
|--------|------|
| POST | `/api/v1/rooms/{roomId}/streams` |
| GET | `/api/v1/rooms/{roomId}/streams` |
| GET | `/api/v1/streams/{streamId}` |
| GET | `/api/v1/streams/{streamId}/regions` |
| PUT | `/api/v1/streams/{streamId}/regions` |
| PATCH | `/api/v1/streams/{streamId}/regions/{regionId}` |

## 12. Observation — Internal (7) — `observation/api/internal/EventIngestionController.kt:11` (`/internal/v1`)

| Method | Path | DTO |
|--------|------|-----|
| POST | `/internal/v1/events` | `IngestEventRequest` / `IngestPerceptionRequest` (kind `POSTURE|LOCATION|STAFF_PRESENCE|ACCESSORY_PRESENCE`) |
| POST | `/internal/v1/scene-events` | `IngestSceneEventRequest` (added 2026-08-29, persists `scene_events` + `SceneEventRepository`) |
| POST | `/internal/v1/notifications` | `IngestNotificationRequest` |
| POST | `/internal/v1/clinical/sleep-summaries` | `IngestSummaryRequest<SleepSummaryData>` |
| POST | `/internal/v1/clinical/mobility-summaries` | `IngestSummaryRequest<MobilitySummaryData>` |
| POST | `/internal/v1/clinical/bathroom-summaries` | `IngestSummaryRequest<BathroomSummaryData>` |
| POST | `/internal/v1/clinical/incidents` | Stub `201` (use `/api/v1/history-episodes` para persistir) |

## 13. Observation — REST (14) — `observation/api/rest/ObservationController.kt:12` (`/api/v1`)

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/wings/{wingId}/board` | Board (stub emptyList hoy) |
| GET | `/api/v1/rooms/{roomId}/peek` | **GET** (no POST) |
| GET | `/api/v1/residents/{residentId}/sleep?date=&from=&to=` | 14-day default |
| GET | `/api/v1/residents/{residentId}/mobility?date=&from=&to=` | — |
| GET | `/api/v1/residents/{residentId}/bathroom?date=&from=&to=` | — |
| GET | `/api/v1/residents/{residentId}/current-state` | `BedStateService` |
| GET | `/api/v1/residents/{residentId}/timeline` | Stub `{"residentId":…}` |
| GET | `/api/v1/residents/{residentId}/events` | Stub |
| GET | `/api/v1/residents/{residentId}/scene-events` | **New 2026-08-29** — persisted `scene_events` |
| GET | `/api/v1/residents/{residentId}/notifications` | Persisted `notification_events` |
| GET | `/api/v1/beds/{bedId}/notifications` | — |
| GET | `/api/v1/companion/rooms` | Stub emptyList |
| GET | `/api/v1/reports/summary` | Stub `{"summary":"ok"}` |
| GET | `/api/v1/catalog/states` | Static catalog (10 states + 3 roomStates) |

## 14. Platform / Ops

| Method | Path | Source |
|--------|------|--------|
| GET | `/actuator/health` | Spring Boot Actuator |
| GET | `/api-docs` | `springdoc-openapi` (`application.yml:37`) |
| GET | `/swagger-ui.html` | Swagger UI |
| OPTIONS | `*` | CORS preflight |

---

**Total: ~101 endpoints** (100 canonical + 1 legacy alias). Distribución distinta al doc viejo que afirmaba `96 Rust via rutas.toml` — no existe `rutas.toml` en repo.

### DSL → Endpoint map (contrato)

| DSL | Endpoint |
|-----|----------|
| `observation.registerPerception` | `POST /internal/v1/events` |
| `observation.registerSceneChange` | `POST /internal/v1/scene-events` (implemented 2026-08-29) |
| `observation.ingestSleepSummary` | `POST /internal/v1/clinical/sleep-summaries` |
| `observation.notifyInformational` | `POST /internal/v1/notifications` |
| `observation.sceneChanges` | `GET /api/v1/residents/{id}/scene-events` |
| `surveillance.registerEpisode` | `POST /api/v1/episodes` |
| `care.addResidentNote / registerFinding` | `POST /api/v1/residents/{id}/notes` |
| `care.addShiftNote` | `POST /api/v1/shift-notes` |
| `history.residentHistoryEpisodes` | `GET /api/v1/residents/{id}/history-episodes` |
| `evidence.createEvidence` | `POST /api/v1/evidence` |

### Legacy / deprecated

- `observation.ingestEvent(monitorKey,kind…)` → use `registerPerception`
- `surveillance.triggerEpisode` → use `registerEpisode`
- `care.addResidentNote` with `kind=INSIGHT|PATTERN` → use `registerFinding` alias
- `POST /api/v1/internal/care-summaries` → use `POST /internal/v1/care-summaries`
