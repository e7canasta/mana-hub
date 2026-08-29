# Domain Model

> Source: `*/domain/model/*.kt` + `data-model.md` (PostgreSQL). Last verified 2026-08-29.

## Entities by Bounded Context

### Residence — `residence/domain/model/`

| Entity | Table | Attributes | Actions |
|--------|-------|------------|---------|
| **Facility** | `facilities` | `id`, `name`, `timezone`, `retiredAt/by`, `version` | `setupFacility`, `update`, `tree` |
| **Wing** | `wings` | `id`, `facilityId`, `name`, `floor`, `sortOrder`, `retiredAt/by` | `addWing`, `update` |
| **Room** | `rooms` | `id`, `wingId`, `number`, `roomType`, `streamKey`, `retiredAt/by` | `addRoom`, `update`, `privacyRegions` |
| **Bed** | `beds` | `id`, `roomId`, `label`, `monitorKey` (unique where active, V3), `retiredAt/by` | `addBed`, `update`, `assign` |
| **PlanogramPlacement** | `planogram_placements` | `id`, `wingId`, `roomId`, `x`, `y`, `sortOrder`, `active` | `putPlanogram` |
| **RoomPrivacyRegion** | `room_privacy_regions` | `id`, `roomId`, `x`, `y`, `w`, `h`, `active` | `putPrivacyRegions` |

### Population — `population/domain/model/`

| Entity | Table | Attributes | Actions |
|--------|-------|------------|---------|
| **Resident** | `residents` | `id`, `externalId`, `fullName`, `birthDate`, `admissionDate`, `status` (`ACTIVE|DISCHARGED`), `dischargedAt/by`, `version` | `admit`, `discharge`, `update` |
| **BedAssignment** | `resident_bed_assignments` | `id`, `residentId`, `bedId`, `startsAt`, `endsAt`, `createdBy`, `version` | `assignTo`, `release` (partial unique on `resident_id`/`bed_id` where `ends_at IS NULL`) |

### Coverage — `coverage/domain/model/`

| Entity | Table | Attributes | Actions |
|--------|-------|------------|---------|
| **StaffGroup** | `staff_groups` | `id`, `facilityId`, `name`, `retiredAt/by`, `version` | `createStaffGroup` |
| **FacilityShift** | `facility_shifts` | `id`, `facilityId`, `key`, `label`, `startMinute` (0..1439), `sortOrder`, `retiredAt/by` | `createShift` |
| **UnitShiftCoverage** | `unit_shift_coverages` | `id`, `wingId`, `staffGroupId`, `shiftKey`, `validFrom/To` | (no controller yet — DDL only) |
| **StaffMember** | `staff_members` | `id`, `facilityId`, `fullName`, `role` (`NURSE|DOCTOR|...|OTHER`), `userId?` (unique where not retired), `retiredAt/by` | (via `StaffGroupApplicationService`) |

### Care — `care/domain/model/`

| Entity | Table | Attributes | Actions |
|--------|-------|------------|---------|
| **Round** | `rounds` | `id`, `wingId`, `status` (`IN_PROGRESS|COMPLETED|CANCELLED`), `scheduledFor`, `startedAt/by`, `completedAt/by`, `version` | `startRound`, `completeRound` (partial unique `rounds_wing_in_progress_idx`) |
| **RoundTask** | `round_tasks` | `id`, `roundId`, `residentId`, `bedId?`, `status` (`PENDING|COMPLETED`), `note`, `completedAt/by`, `version` | `completeTask` |
| **CareNote** | `care_notes` | `id`, `residentId`, `authorId`, `kind` (default `general`), `body`, `durationMin`, `version` | `createCareNote` |
| **ResidentNote** | `resident_notes` | `id`, `residentId`, `authorId`, `kind` (`CARE|CLINICAL|INSIGHT|PATTERN|OBSERVATION|SUMMARY`), `body`, `sourceEventId?`, `timestamp` | `addResidentNote`, `registerFinding` (alias for INSIGHT/PATTERN/OBSERVATION) |
| **EpisodeNote** | `episode_notes` | `id`, `episodeId`, `authorId`, `kind` (`ACKNOWLEDGEMENT|RESOLUTION|CLINICAL_NOTE`), `body`, `timestamp` | `addEpisodeNote` |
| **ShiftNote** | `shift_notes` | `id`, `facilityId`, `wingId?`, `shiftKey`, `shiftDate`, `authorId`, `kind` (`SHIFT_SUMMARY|INCIDENT_REPORT|GENERAL`), `body`, `timestamp` | `addShiftNote` |
| **CareSummary** | `care_summaries` | `id`, `sourceRecordId` (unique), `residentId`, `observedOn`, `totalMinutes`, `proactiveMinutes`, `roundsCount`, `notesCount` | `ingestCareSummary` (`POST /internal/v1/care-summaries`) |

### Policy — `policy/domain/model/`

| Entity | Table | Attributes | Actions |
|--------|-------|------------|---------|
| **AlarmProfileVersion** | `alarm_profile_versions` | `id`, `residentId`, `validFrom/To`, `mobilityAid`, `autopilot`, `mode`, `templateId`, `overridesJson` (default `'{}'`), `catalogVersion`, `updatedBy`, `riskLevel` (`low|medium|high`), `version` | `configureAlarmProfile`, `history` (partial unique `valid_to IS NULL`) |
| **PolicyOverride** | `alarm_profile_overrides` | `id`, `profileVersionId`, `ruleId` (unique together), `overrideType`, `stateKind`, `transitionKey`, `warningAfterMinutes`, `alertAfterMinutes`, `hysteresisSeconds`, `baselineState`, `severity`, `closureCondition` | DAG overrides (V9) |
| **DagCatalog** | (code, no table) | `STANDARD|NIGHT_WANDERING|FALL_RISK|CRITICAL` | `GET /api/v1/alarm-presets/catalog` |

Enums: `MobilityAid`, `PolicyMode(PRESET|CUSTOM)`, `RiskLevel(LOW|MEDIUM|HIGH)`, `WatchLevel`, `StateKind`, `Severity`, `ClosureCondition` (`policy/domain/model/DagCatalog.kt:9`).

### Surveillance — `surveillance/domain/model/`

| Entity | Table | Attributes | Actions |
|--------|-------|------------|---------|
| **Episode** | `episodes` | `id`, `residentId`, `bedId?`, `severity` (`INFO|WARNING|CRITICAL|EMERGENCY`), `status` (`pending|acknowledged|resolved`), `title`, `detail`, `occurredAt`, `escalationLevel`, `evidenceKind/Ref`, `ruleId`, `version` | `registerEpisode`, `acknowledge`, `resolve` |

Child tables (no domain model, pure DDL): `episode_transitions`, `notification_deliveries` → `notification_delivery_events`, `episode_escalations`.

### Observation — `observation/domain/model/`

| Entity | Table | Attributes | Actions |
|--------|-------|------------|---------|
| **SensorEvent** | `sensor_events` | `id`, `sourceEventId` (unique), `monitorKey`, `bedId?`, `residentId?`, `kind` (`POSTURE|LOCATION|STAFF_PRESENCE|ACCESSORY_PRESENCE`), `roomState`, `state`, `sleeping`, `occurredAt`, `payloadJson` | `registerPerception` → `POST /internal/v1/events` |
| **CurrentBedState** | `current_bed_states` | `bedId` (PK), `residentId?`, `roomState`, `state`, `substate`, `sleeping`, `stateSince`, `staffPresent` (V10) | updated on each `ingestEvent` |
| **SceneEvent** | `scene_events` | `id`, `eventId` (unique), `bedId`, `residentId?`, `eventType` (`TRANSITION|PERMANENCE`), `fromState`, `toState`, `triggerType` (`hysteresis|permanence|manual`), `timestamp`, `payloadJson` | `registerSceneChange` → `POST /internal/v1/scene-events` (implemented 2026-08-29) |
| **NotificationEvent** | `notification_events` | `id`, `category`, `bedId?`, `residentId?`, `eventType`, `timestamp`, `ruleId?`, `riskLevel?`, `payloadJson` | `notifyInformational` → `POST /internal/v1/notifications` |
| **SleepSummary** | `sleep_summaries` | `id`, `sourceRecordId` (unique), `residentId`, `observedOn`, `calm|restless|awake|outOfBedMinutes`, `bedExitCount`, `wakeCount`, `startedAt/endedAt` (V8), `source`, `modelVersion` | `POST /internal/v1/clinical/sleep-summaries` |
| **MobilitySummary** | `mobility_summaries` | `id`, `sourceRecordId` (unique), `residentId`, `observedOn`, `in|outBed|outOfSight|walkingMinutes`, `distanceMeters`, `transferCount` | `POST /internal/v1/clinical/mobility-summaries` |
| **BathroomSummary** | `bathroom_summaries` | `id`, `sourceRecordId` (unique), `residentId`, `observedOn`, `visitCount`, `nightVisitCount`, `assistedCount`, `totalMinutes` | `POST /internal/v1/clinical/bathroom-summaries` |

### Evidence — `evidence/domain/model/`

| Entity | Table | Attributes | Actions |
|--------|-------|------------|---------|
| **Evidence** | `evidence` | `id`, `bedId`, `residentId`, `evidenceType`, `category`, `sceneEventId/json`, `ruleId`, `shift`, `riskLevel`, `timestamp`, `version` | `POST /api/v1/evidence` |
| **Timeline** | `timelines` | `id`, `bedId`, `residentId`, `anchorEventId/json`, `before/afterEventsJson`, `windowStart/End`, `closedAt`, `version` | `POST /api/v1/timelines` + `close` |
| **ClipWindow** | `clip_windows` | `windowId` (PK), `bedId`, `residentId`, `startedAt`, `endedAt`, `timeoutMinutes` (5), `eventsJson`, `state` (`open|closed`), `closeConditionJson`, `closedAt`, `version` | `POST /api/v1/clip-windows` + `close` + `open?bedId` |

### History — `history/domain/model/`

| Entity | Table | Attributes | Actions |
|--------|-------|------------|---------|
| **HistoryEpisode** | `history_episode_detections` | `id`, `sourceRecordId` (unique), `residentId`, `bedId?`, `sourceEpisodeId`, `kind`, `severity` (`LOW|MEDIUM|HIGH|CRITICAL`), `occurredAt`, `location/activity/injuryStatus`, `selfRecovery`, `responseSeconds`, `narrative`, `interventionsJson`, `source`, `modelVersion`, `confidence`, `version` | `POST /api/v1/history-episodes`, `GET /residents/{id}/history-episodes` |
| **HistoryEpisodeReview** | `history_episode_reviews` | `id`, `episodeId`, `status`, `detectionVerdict`, `reviewNote`, `resolvedAt`, `actorId`, `version` | `PATCH /history-episodes/{id}` |
| **HistoryEpisodeIntervention** | `history_episode_interventions` | `id`, `episodeId` (FK CASCADE), `kind` (9 values), `performedAt`, `performedBy` → `staff_members.id`, `detail` | normalized V6 |

Enums: `EpisodeKind(FALL|...)`, `HistoryEpisodeSeverity`, `InterventionKind`, `EventSource` (`history/domain/model/`).

### Identity — `identity/domain/model/`

| Entity | Table | Attributes | Actions |
|--------|-------|------------|---------|
| **User** | `users` | `id`, `username` (unique), `displayName`, `role` (`OWNER|SUPERVISOR|STAFF`), `jobTitle`, `passwordHash`, `retiredAt/by`, `version` | `POST /api/v1/users`, `PATCH /users/{id}` |
| **AuthSession** | `auth_sessions` | `tokenHash` (BYTEA PK), `userId`, `expiresAt`, `lastSeenAt` | (no auth endpoints yet) |

### Audit — `audit/domain/model/`

| Entity | Table | Attributes | Actions |
|--------|-------|------------|---------|
| **AuditLogEntry** | `audit_log` | `id`, `actorId` (opaque), `action`, `entityType`, `entityId`, `metadataJson`, `createdAt` | `GET /api/v1/audit-log` |

### Streams — `streams/domain/model/`

| Entity | Table | Attributes | Actions |
|--------|-------|------------|---------|
| **Stream** | `streams` | `id`, `roomId`, `streamKey`, `name`, `active`, `version` | `POST /api/v1/rooms/{id}/streams` (unique `room_id, stream_key` where `active=TRUE`) |
| **StreamRegion** | `stream_regions` | `id`, `streamId`, `regionType` (`bathroom|hallway|exit|bed|furniture|person|object`), `points` (JSON), `label`, `isStatic`, `updatedBy`, `version` | `PUT /streams/{id}/regions` |

## Aggregate Roots

| Aggregate | Root | Children | Versioned |
|-----------|------|----------|-----------|
| Residence | Facility | Wing → Room → Bed → (Streams, PrivacyRegions, Planogram) | yes |
| Population | Resident | BedAssignment | yes |
| Coverage | StaffGroup | FacilityShift, UnitShiftCoverage | yes (except Coverage) |
| Care | Round | RoundTask | yes |
| Policy | AlarmProfileVersion | AlarmProfileOverrides | yes |
| Surveillance | Episode | (transitions/deliveries/escalations as DDL children) | yes |
| Observation | SensorEvent / SceneEvent / NotificationEvent | CurrentBedState (materialized view) | no |
| Evidence | Evidence / Timeline / ClipWindow | — | yes |
| History | HistoryEpisode | Reviews, Interventions | yes |
| Identity | User | AuthSession | yes |
| Audit | AuditLogEntry | — | no |
| Streams | Stream | StreamRegion | yes |
