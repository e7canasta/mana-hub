# Data Model Reference

> Source of truth: `bootstrap/src/main/resources/db/migration/V1__init.sql` + V2-V10 (Flyway, 12 files). Reverse-engineered 2026-08-29.

**Database:** PostgreSQL 17 (`docker-compose.yml:3`, `org.postgresql:42.7.7` in `gradle/libs.versions.toml:26`)
**JDBC:** `jdbc:postgresql://localhost:5432/mana_hub` (`bootstrap/src/main/resources/application.yml:8`)
**Migrations:** Flyway V1-V10 (V3 has 2 files: `V3__rename_alerts_to_episodes.sql` + `V3__unique_monitor.sql`; V10 has 2 files). `flyway.enabled=false` in app, `ddl-auto:none`.
**Tables:** 44 across 12 contexts (37 in V1 + 7 added V4/V6/V7/V9)
**Optimistic locking:** every aggregate root has `version BIGINT NOT NULL DEFAULT 0` (`V2__add_version_columns.sql`) + `@Version` in JPA.

---

## Overview

| Context | Tables | Description | Migration |
|---------|--------|-------------|-----------|
| ctx-identidad | 2 | Users and auth sessions | V1 |
| ctx-auditoria | 1 | Immutable audit trail | V1 |
| ctx-residencia | 6 | Facility → Wing → Room → Bed + planograma + privacidad | V1 |
| ctx-poblacion | 2 | Residents + bed assignments | V1 |
| ctx-cobertura | 4 | Staff groups, shifts, coverages, staff_members | V1 + V6 |
| ctx-cuidado | 7 | Rounds, tasks, care_notes + resident/episode/shift notes + care_summaries | V1 + V4 + V7 |
| ctx-historia | 3 | history_episode_detections/reviews/interventions | V1 + V5 + V6 |
| ctx-politica | 2 | alarm_profile_versions + alarm_profile_overrides (DAG) | V1 + V9 |
| ctx-vigilancia | 5 | episodes + transitions + deliveries + delivery_events + escalations | V1 + V3 |
| ctx-evidence | 3 | evidence, timelines, clip_windows | V1 |
| ctx-streams | 2 | streams + stream_regions | V1 |
| mana-observation | 8 | sensor_events, current_bed_states (+staff_present V10), scene_events, notification_events, sleep/mobility/bathroom_summaries (+started/ended V8), care_summaries* | V1 + V8 + V10 |

\* `care_summaries` lives in `care` module but is ingested via `observation`-style internal endpoint.

> **Fantasmas eliminados vs. docs viejos:** `resident_attributes` y `staff_group_members` nunca existieron en DDL (0 hits en `grep -rn`). No listarlos.

---

## ER Diagram (PostgreSQL)

```
ctx-identidad: users 1──N auth_sessions (FK users.id, PK token_hash BYTEA)
ctx-auditoria: audit_log (actor_id opaque → users.id)

ctx-residencia:
  facilities 1──N wings 1──N rooms 1──N beds
                    │         │
                    │         ├── planogram_placements (wing_id, room_id, x,y, sort_order, active BOOLEAN)
                    └── room_privacy_regions (room_id, x/y/w/h, active BOOLEAN)
  rooms_active_number_idx UNIQUE (wing_id, number) WHERE retired_at IS NULL
  beds_active_monitor_idx UNIQUE (monitor_key) WHERE monitor_key IS NOT NULL AND retired_at IS NULL (V3)

ctx-poblacion:
  residents 1──N resident_bed_assignments (resident_id, bed_id, starts_at, ends_at)
  residents_open_assignment_idx UNIQUE (resident_id) WHERE ends_at IS NULL
  beds_open_assignment_idx      UNIQUE (bed_id) WHERE ends_at IS NULL

ctx-cobertura:
  staff_groups (facility_id) ── facility_shifts (facility_id, key, label, start_minute 0..1439)
                             └─ unit_shift_coverages (wing_id, staff_group_id, shift_key, valid_from/valid_to)
  staff_members (facility_id, full_name, role CHECK, user_id UNIQUE WHERE retired_at IS NULL)

ctx-cuidado:
  rounds (wing_id, status CHECK in_progress/completed/cancelled) 1──N round_tasks (round_id, resident_id, bed_id, status pending/completed)
  care_notes (resident_id, author_id, kind, body, duration_min)
  resident_notes  (resident_id, author_id, kind CHECK CARE/CLINICAL/INSIGHT/PATTERN/OBSERVATION/SUMMARY, body, source_event_id, timestamp)
  episode_notes   (episode_id, author_id, kind CHECK ACKNOWLEDGEMENT/RESOLUTION/CLINICAL_NOTE, body, timestamp)
  shift_notes     (facility_id, wing_id?, shift_key, shift_date, author_id, kind CHECK SHIFT_SUMMARY/INCIDENT_REPORT/GENERAL, body, timestamp)
  care_summaries  (source_record_id UNIQUE, resident_id, observed_on DATE, total/proactive_minutes, rounds_count, notes_count) UNIQUE (resident_id, observed_on)

ctx-historia:
  history_episode_detections (source_record_id UNIQUE, resident_id, bed_id, source_episode_id, kind, severity, occurred_at, location/activity/injury_status, self_recovery BOOLEAN, response_seconds, narrative, interventions_json, source, model_version, confidence)
    1──N history_episode_reviews (episode_id → detections.id, status, detection_verdict, review_note, resolved_at, actor_id)
    1──N history_episode_interventions (episode_id → detections.id CASCADE, kind CHECK 9 values, performed_at, performed_by → staff_members.id, detail)

ctx-politica:
  alarm_profile_versions (resident_id, valid_from, valid_to, mobility_aid, autopilot BOOLEAN, mode, template_id, overrides_json DEFAULT '{}', catalog_version, updated_by, risk_level) UNIQUE (resident_id) WHERE valid_to IS NULL
  alarm_profile_overrides (profile_version_id → versions.id, rule_id, override_type, state_kind, transition_key, warning/alert_after_minutes, hysteresis_seconds, baseline_state, severity, closure_condition) UNIQUE (profile_version_id, rule_id)

ctx-vigilancia:
  episodes (resident_id, bed_id, evidence_kind/ref, rule_id, severity, status DEFAULT pending, status_actor_id/at, title, detail, occurred_at, escalation_level, escalated_at/to, version)
    1──N episode_transitions (episode_id, from_status, to_status, actor_id, occurred_at, sequence)
    1──N notification_deliveries (episode_id, recipient_kind/id, channel, escalation_level) 1──N notification_delivery_events (delivery_id, kind, reason, occurred_at)
    1──N episode_escalations (episode_id, level, target_id, occurred_at)

ctx-evidence:
  evidence (bed_id, resident_id, evidence_type, category, scene_event_id/json, rule_id, shift, risk_level, timestamp)
  timelines (bed_id, resident_id, anchor_event_id/json, before/after_events_json, window_start/end, closed_at)
  clip_windows (PK window_id, bed_id, resident_id, started_at, ended_at, timeout_minutes DEFAULT 5, events_json, state DEFAULT open, close_condition_json, closed_at)

ctx-streams:
  streams (room_id, stream_key, name, active BOOLEAN) UNIQUE (room_id, stream_key) WHERE active=TRUE
  stream_regions (stream_id → streams.id, region_type CHECK bathroom/hallway/exit/bed/furniture/person/object, points JSON, label, is_static BOOLEAN)

mana-observation:
  sensor_events (source_event_id UNIQUE, monitor_key, bed_id?, resident_id?, kind, room_state, substate, zone, state, sleeping BOOLEAN, occurred_at, received_at DEFAULT NOW(), payload_json)
    INDEX idx_sensor_events_unresolved ON (monitor_key) WHERE bed_id IS NULL
  current_bed_states (PK bed_id, resident_id?, room_state, state, substate, sleeping BOOLEAN, state_since, updated_at, source, source_event_id, staff_present BOOLEAN V10)
  scene_events (event_id UNIQUE, bed_id, resident_id?, event_type, from_state, to_state, trigger_type, timestamp, payload_json, received_at)
  notification_events (category, bed_id?, resident_id?, event_type, timestamp, rule_id, risk_level, payload_json, received_at)
  sleep_summaries (source_record_id UNIQUE, resident_id, observed_on DATE, calm/restless/awake/out_of_bed_minutes, bed_exit_count, wake_count, started_at/ended_at TIMESTAMP V8, source, model_version, confidence) UNIQUE (resident_id, observed_on)
  mobility_summaries (source_record_id UNIQUE, resident_id, observed_on DATE, in/out_bed/out_of_sight/walking_minutes, distance_meters, transfer_count) UNIQUE (resident_id, observed_on)
  bathroom_summaries (source_record_id UNIQUE, resident_id, observed_on DATE, visit_count, night_visit_count, assisted_count, total_minutes) UNIQUE (resident_id, observed_on)
```

---

## Consolidated DDL (PostgreSQL, merged V1..V10)

> Renames applied: `alerts`→`episodes` (V3), `incident_detections`→`history_episode_detections` (V5), `level`→`severity`, `source_alert_id`→`source_episode_id`. V2 `version` columns omitted per table for brevity — add `version BIGINT NOT NULL DEFAULT 0` to every table listed in `V2__add_version_columns.sql:3-23`. Types are PostgreSQL (`TIMESTAMP`, `BOOLEAN`, `BYTEA`, `NOW()`), not SQLite `TEXT`/`datetime('now')`.

### ctx-identidad

```sql
CREATE TABLE users (
    id            TEXT PRIMARY KEY,
    username      TEXT UNIQUE,
    display_name  TEXT,
    role          TEXT CHECK (role IN ('owner', 'supervisor', 'staff')),
    job_title     TEXT,
    password_hash TEXT,
    retired_at    TEXT,
    retired_by    TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    version       BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE auth_sessions (
    token_hash   BYTEA PRIMARY KEY,
    user_id      TEXT NOT NULL REFERENCES users(id),
    expires_at   TIMESTAMP NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMP
);
CREATE INDEX auth_sessions_user_expiry_idx ON auth_sessions (user_id, expires_at);
```

### ctx-auditoria

```sql
CREATE TABLE audit_log (
    id            TEXT PRIMARY KEY,
    actor_id      TEXT,
    action        TEXT NOT NULL,
    entity_type   TEXT NOT NULL,
    entity_id     TEXT NOT NULL,
    metadata_json TEXT DEFAULT '{}',
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX audit_log_entity_time_idx ON audit_log (entity_type, entity_id, created_at DESC);
CREATE INDEX audit_log_actor_time_idx ON audit_log (actor_id, created_at DESC);
```

### ctx-residencia

```sql
CREATE TABLE facilities (
    id         TEXT PRIMARY KEY, name TEXT NOT NULL, timezone TEXT NOT NULL DEFAULT 'UTC',
    retired_at TEXT, retired_by TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(), version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE wings (
    id TEXT PRIMARY KEY, facility_id TEXT NOT NULL REFERENCES facilities(id), name TEXT NOT NULL,
    floor TEXT, sort_order INTEGER DEFAULT 0, retired_at TEXT, retired_by TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(), version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE rooms (
    id TEXT PRIMARY KEY, wing_id TEXT NOT NULL REFERENCES wings(id), number TEXT NOT NULL,
    room_type TEXT, stream_key TEXT, retired_at TEXT, retired_by TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(), version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX rooms_active_number_idx ON rooms (wing_id, number) WHERE retired_at IS NULL;
CREATE TABLE beds (
    id TEXT PRIMARY KEY, room_id TEXT NOT NULL REFERENCES rooms(id), label TEXT NOT NULL,
    monitor_key TEXT, retired_at TEXT, retired_by TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(), version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX beds_active_monitor_idx ON beds (monitor_key) WHERE monitor_key IS NOT NULL AND retired_at IS NULL; -- V3
CREATE TABLE planogram_placements (
    id TEXT PRIMARY KEY, wing_id TEXT NOT NULL REFERENCES wings(id), room_id TEXT NOT NULL REFERENCES rooms(id),
    x REAL NOT NULL, y REAL NOT NULL, sort_order INTEGER DEFAULT 0, active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE TABLE room_privacy_regions (
    id TEXT PRIMARY KEY, room_id TEXT NOT NULL REFERENCES rooms(id),
    x REAL NOT NULL, y REAL NOT NULL, w REAL NOT NULL, h REAL NOT NULL, active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### ctx-poblacion

```sql
CREATE TABLE residents (
    id TEXT PRIMARY KEY, external_id TEXT UNIQUE, full_name TEXT NOT NULL, birth_date DATE, admission_date DATE NOT NULL,
    status TEXT DEFAULT 'active' CHECK (status IN ('active', 'discharged')),
    discharged_at TIMESTAMP, discharged_by TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(), version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE resident_bed_assignments (
    id TEXT PRIMARY KEY, resident_id TEXT NOT NULL REFERENCES residents(id), bed_id TEXT NOT NULL,
    starts_at TIMESTAMP NOT NULL, ends_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), created_by TEXT, version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX residents_open_assignment_idx ON resident_bed_assignments (resident_id) WHERE ends_at IS NULL;
CREATE UNIQUE INDEX beds_open_assignment_idx ON resident_bed_assignments (bed_id) WHERE ends_at IS NULL;
```

### ctx-cobertura

```sql
CREATE TABLE staff_groups (
    id TEXT PRIMARY KEY, facility_id TEXT NOT NULL, name TEXT NOT NULL,
    retired_at TEXT, retired_by TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(), version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE facility_shifts (
    id TEXT PRIMARY KEY, facility_id TEXT NOT NULL, key TEXT NOT NULL, label TEXT NOT NULL,
    start_minute INTEGER NOT NULL CHECK (start_minute BETWEEN 0 AND 1439), sort_order INTEGER DEFAULT 0,
    retired_at TEXT, retired_by TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(), version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE unit_shift_coverages (
    id TEXT PRIMARY KEY, wing_id TEXT NOT NULL, staff_group_id TEXT NOT NULL, shift_key TEXT NOT NULL,
    valid_from TIMESTAMP NOT NULL DEFAULT NOW(), valid_to TIMESTAMP, created_at TIMESTAMP NOT NULL DEFAULT NOW(), created_by TEXT
);
-- V6
CREATE TABLE staff_members (
    id TEXT PRIMARY KEY, facility_id TEXT NOT NULL, full_name TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('NURSE','DOCTOR','CAREGIVER','PHYSIOTHERAPIST','SOCIAL_WORKER','ADMINISTRATOR','OTHER')),
    user_id TEXT, retired_at TEXT, retired_by TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_staff_members_user ON staff_members (user_id) WHERE user_id IS NOT NULL AND retired_at IS NULL;
```

### ctx-cuidado

```sql
CREATE TABLE rounds (
    id TEXT PRIMARY KEY, wing_id TEXT NOT NULL, status TEXT DEFAULT 'in_progress' CHECK (status IN ('in_progress', 'completed', 'cancelled')),
    scheduled_for TIMESTAMP, started_at TIMESTAMP, completed_at TIMESTAMP, started_by TEXT, completed_by TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(), version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX rounds_wing_in_progress_idx ON rounds (wing_id) WHERE status = 'in_progress';
CREATE TABLE round_tasks (
    id TEXT PRIMARY KEY, round_id TEXT NOT NULL REFERENCES rounds(id), resident_id TEXT NOT NULL, bed_id TEXT,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'completed')), note TEXT,
    completed_at TIMESTAMP, completed_by TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(), version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE care_notes (
    id TEXT PRIMARY KEY, resident_id TEXT NOT NULL, author_id TEXT NOT NULL, kind TEXT DEFAULT 'general', body TEXT NOT NULL,
    duration_min INTEGER, created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(), version BIGINT NOT NULL DEFAULT 0
);
-- V4
CREATE TABLE resident_notes (
    id TEXT PRIMARY KEY, resident_id TEXT NOT NULL, author_id TEXT NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('CARE','CLINICAL','INSIGHT','PATTERN','OBSERVATION','SUMMARY')),
    body TEXT NOT NULL, source_event_id TEXT, timestamp TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (now()), updated_at TEXT NOT NULL DEFAULT (now())
);
CREATE TABLE episode_notes (
    id TEXT PRIMARY KEY, episode_id TEXT NOT NULL, author_id TEXT NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('ACKNOWLEDGEMENT','RESOLUTION','CLINICAL_NOTE')), body TEXT NOT NULL,
    timestamp TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT (now())
);
CREATE TABLE shift_notes (
    id TEXT PRIMARY KEY, facility_id TEXT NOT NULL, wing_id TEXT, shift_key TEXT NOT NULL, shift_date TEXT NOT NULL,
    author_id TEXT NOT NULL, kind TEXT NOT NULL CHECK (kind IN ('SHIFT_SUMMARY','INCIDENT_REPORT','GENERAL')),
    body TEXT NOT NULL, timestamp TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT (now())
);
-- V7
CREATE TABLE care_summaries (
    id TEXT PRIMARY KEY, source_record_id TEXT UNIQUE NOT NULL, resident_id TEXT NOT NULL, observed_on DATE NOT NULL,
    total_minutes INTEGER DEFAULT 0, proactive_minutes INTEGER DEFAULT 0, rounds_count INTEGER DEFAULT 0, notes_count INTEGER DEFAULT 0,
    source TEXT, model_version TEXT, confidence REAL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_care_summaries_resident_day ON care_summaries (resident_id, observed_on);
```

### ctx-historia

```sql
-- V1 = incident_detections, V5 renamed to history_episode_detections
CREATE TABLE history_episode_detections (
    id TEXT PRIMARY KEY, source_record_id TEXT UNIQUE NOT NULL, resident_id TEXT NOT NULL, bed_id TEXT,
    source_episode_id TEXT, kind TEXT NOT NULL, severity TEXT NOT NULL, occurred_at TIMESTAMP NOT NULL,
    location TEXT, activity TEXT, injury_status TEXT, self_recovery BOOLEAN DEFAULT FALSE,
    response_seconds INTEGER, narrative TEXT, interventions_json TEXT DEFAULT '[]',
    source TEXT NOT NULL, model_version TEXT, confidence REAL, provenance_json TEXT DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE history_episode_reviews (
    id TEXT PRIMARY KEY, episode_id TEXT NOT NULL, status TEXT NOT NULL,
    detection_verdict TEXT, review_note TEXT, resolved_at TIMESTAMP, actor_id TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), version BIGINT NOT NULL DEFAULT 0
);
-- V6
CREATE TABLE history_episode_interventions (
    id TEXT PRIMARY KEY, episode_id TEXT NOT NULL REFERENCES history_episode_detections(id) ON DELETE CASCADE,
    kind TEXT NOT NULL CHECK (kind IN ('FAMILY_NOTIFIED','STAFF_DISPATCHED','BANDAGE_APPLIED','MEDICATION_GIVEN','TRANSFERRED_TO_HOSPITAL','REPOSITIONED','CALLED_FOR_HELP','VITAL_SIGNS_CHECKED','OTHER')),
    performed_at TIMESTAMP NOT NULL, performed_by TEXT REFERENCES staff_members(id), detail TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### ctx-politica

```sql
CREATE TABLE alarm_profile_versions (
    id TEXT PRIMARY KEY, resident_id TEXT NOT NULL, valid_from TIMESTAMP NOT NULL DEFAULT NOW(), valid_to TIMESTAMP,
    mobility_aid TEXT, autopilot BOOLEAN DEFAULT FALSE, mode TEXT, template_id TEXT,
    overrides_json TEXT DEFAULT '{}', catalog_version TEXT, updated_by TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), risk_level TEXT DEFAULT 'medium', version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX idx_alarm_profiles_one_current ON alarm_profile_versions (resident_id) WHERE valid_to IS NULL;
-- V9
CREATE TABLE alarm_profile_overrides (
    id TEXT PRIMARY KEY, profile_version_id TEXT NOT NULL REFERENCES alarm_profile_versions(id),
    rule_id TEXT NOT NULL, override_type TEXT NOT NULL, state_kind TEXT, transition_key TEXT,
    warning_after_minutes INTEGER, alert_after_minutes INTEGER, hysteresis_seconds INTEGER,
    baseline_state TEXT, severity TEXT, closure_condition TEXT, created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(profile_version_id, rule_id)
);
```

### ctx-vigilancia

```sql
-- V1 = alerts, V3 renamed to episodes + level->severity
CREATE TABLE episodes (
    id TEXT PRIMARY KEY, resident_id TEXT NOT NULL, bed_id TEXT,
    evidence_kind TEXT, evidence_ref TEXT, rule_id TEXT, severity TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending', status_actor_id TEXT, status_at TIMESTAMP,
    title TEXT, detail TEXT, occurred_at TIMESTAMP NOT NULL,
    escalation_level INTEGER DEFAULT 0, escalated_at TIMESTAMP, escalated_to TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(), version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE episode_transitions (
    id TEXT PRIMARY KEY, episode_id TEXT NOT NULL REFERENCES episodes(id),
    from_status TEXT, to_status TEXT NOT NULL, actor_id TEXT NOT NULL, occurred_at TIMESTAMP NOT NULL, sequence INTEGER NOT NULL
);
CREATE TABLE notification_deliveries (
    id TEXT PRIMARY KEY, episode_id TEXT NOT NULL REFERENCES episodes(id),
    recipient_kind TEXT NOT NULL, recipient_id TEXT NOT NULL, channel TEXT NOT NULL,
    escalation_level INTEGER DEFAULT 0, created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE TABLE notification_delivery_events (
    id TEXT PRIMARY KEY, delivery_id TEXT NOT NULL REFERENCES notification_deliveries(id),
    kind TEXT NOT NULL, reason TEXT, occurred_at TIMESTAMP NOT NULL
);
CREATE TABLE episode_escalations (
    id TEXT PRIMARY KEY, episode_id TEXT NOT NULL REFERENCES episodes(id),
    level INTEGER NOT NULL, target_id TEXT NOT NULL, occurred_at TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### ctx-evidence

```sql
CREATE TABLE evidence (
    id TEXT PRIMARY KEY, bed_id TEXT NOT NULL, resident_id TEXT NOT NULL, evidence_type TEXT NOT NULL, category TEXT,
    scene_event_id TEXT, scene_event_json TEXT, rule_id TEXT, shift TEXT, risk_level TEXT,
    timestamp TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW(), version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE timelines (
    id TEXT PRIMARY KEY, bed_id TEXT NOT NULL, resident_id TEXT NOT NULL, anchor_event_id TEXT, anchor_event_json TEXT,
    before_events_json TEXT DEFAULT '[]', after_events_json TEXT DEFAULT '[]',
    window_start TIMESTAMP NOT NULL, window_end TIMESTAMP, created_at TIMESTAMP NOT NULL DEFAULT NOW(), closed_at TIMESTAMP, version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE clip_windows (
    window_id TEXT PRIMARY KEY, bed_id TEXT NOT NULL, resident_id TEXT NOT NULL,
    started_at TIMESTAMP NOT NULL, ended_at TIMESTAMP, timeout_minutes INTEGER DEFAULT 5,
    events_json TEXT DEFAULT '[]', state TEXT DEFAULT 'open', close_condition_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), closed_at TIMESTAMP, version BIGINT NOT NULL DEFAULT 0
);
```

### ctx-streams

```sql
CREATE TABLE streams (
    id TEXT PRIMARY KEY, room_id TEXT NOT NULL, stream_key TEXT NOT NULL, name TEXT, active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(), version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX streams_active_room_key_idx ON streams (room_id, stream_key) WHERE active = TRUE;
CREATE TABLE stream_regions (
    id TEXT PRIMARY KEY, stream_id TEXT NOT NULL REFERENCES streams(id),
    region_type TEXT NOT NULL CHECK (region_type IN ('bathroom','hallway','exit','bed','furniture','person','object')),
    points TEXT NOT NULL, label TEXT, is_static BOOLEAN DEFAULT TRUE, updated_by TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(), version BIGINT NOT NULL DEFAULT 0
);
```

### mana-observation

```sql
CREATE TABLE sensor_events (
    id TEXT PRIMARY KEY, source_event_id TEXT UNIQUE NOT NULL, monitor_key TEXT NOT NULL, bed_id TEXT, resident_id TEXT,
    kind TEXT NOT NULL, room_state TEXT, substate TEXT, zone TEXT, state TEXT, sleeping BOOLEAN,
    occurred_at TIMESTAMP NOT NULL, received_at TIMESTAMP NOT NULL DEFAULT NOW(), payload_json TEXT DEFAULT '{}'
);
CREATE INDEX idx_sensor_events_unresolved ON sensor_events (monitor_key) WHERE bed_id IS NULL;
CREATE TABLE current_bed_states (
    bed_id TEXT PRIMARY KEY, resident_id TEXT, room_state TEXT, state TEXT, substate TEXT, sleeping BOOLEAN,
    state_since TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    source TEXT, source_event_id TEXT, staff_present BOOLEAN -- V10
);
CREATE TABLE scene_events (
    id TEXT PRIMARY KEY, event_id TEXT UNIQUE NOT NULL, bed_id TEXT NOT NULL, resident_id TEXT,
    event_type TEXT NOT NULL, from_state TEXT, to_state TEXT, trigger_type TEXT,
    timestamp TIMESTAMP NOT NULL, payload_json TEXT DEFAULT '{}', received_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE TABLE notification_events (
    id TEXT PRIMARY KEY, category TEXT NOT NULL, bed_id TEXT, resident_id TEXT, event_type TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL, rule_id TEXT, risk_level TEXT, payload_json TEXT DEFAULT '{}', received_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE TABLE sleep_summaries (
    id TEXT PRIMARY KEY, source_record_id TEXT UNIQUE NOT NULL, resident_id TEXT NOT NULL, observed_on DATE NOT NULL,
    calm_minutes INTEGER DEFAULT 0, restless_minutes INTEGER DEFAULT 0, awake_minutes INTEGER DEFAULT 0, out_of_bed_minutes INTEGER DEFAULT 0,
    bed_exit_count INTEGER DEFAULT 0, wake_count INTEGER DEFAULT 0,
    started_at TIMESTAMP, ended_at TIMESTAMP, -- V8
    source TEXT, model_version TEXT, confidence REAL, provenance_json TEXT DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_sleep_summaries_resident_day ON sleep_summaries (resident_id, observed_on);
CREATE TABLE mobility_summaries (
    id TEXT PRIMARY KEY, source_record_id TEXT UNIQUE NOT NULL, resident_id TEXT NOT NULL, observed_on DATE NOT NULL,
    in_bed_minutes INTEGER DEFAULT 0, out_of_bed_minutes INTEGER DEFAULT 0, out_of_sight_minutes INTEGER DEFAULT 0,
    walking_minutes INTEGER DEFAULT 0, distance_meters REAL DEFAULT 0, transfer_count INTEGER DEFAULT 0,
    source TEXT, model_version TEXT, confidence REAL, provenance_json TEXT DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_mobility_summaries_resident_day ON mobility_summaries (resident_id, observed_on);
CREATE TABLE bathroom_summaries (
    id TEXT PRIMARY KEY, source_record_id TEXT UNIQUE NOT NULL, resident_id TEXT NOT NULL, observed_on DATE NOT NULL,
    visit_count INTEGER DEFAULT 0, night_visit_count INTEGER DEFAULT 0, assisted_count INTEGER DEFAULT 0, total_minutes INTEGER DEFAULT 0,
    source TEXT, model_version TEXT, confidence REAL, provenance_json TEXT DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_bathroom_summaries_resident_day ON bathroom_summaries (resident_id, observed_on);
```

---

## Changelog vs. docs viejos (2026-08-29)

- DB: `SQLite` → `PostgreSQL 17` (usa `TIMESTAMP`/`BOOLEAN`/`BYTEA`/`NOW()`)
- Tablas: `34` → `44` (añadidas `resident_notes`, `episode_notes`, `shift_notes`, `care_summaries`, `staff_members`, `history_episode_interventions`, `alarm_profile_overrides`; renombres `alerts→episodes`, `incident_detections→history_episode_detections`)
- Eliminadas del ER: `resident_attributes`, `staff_group_members` (nunca migradas)
- Nuevas columnas: `current_bed_states.staff_present` (V10), `sleep_summaries.started_at/ended_at` (V8), `version` en 21 tablas (V2)
- Tipos corregidos: `BYTEA` para `auth_sessions.token_hash`, `BOOLEAN` para flags, `DATE` para `birth_date/admission_date/observed_on`
