-- Initial migration for Mana Hub
-- Creates the schema for all bounded contexts

-- ctx-identidad
CREATE TABLE IF NOT EXISTS users (
    id            TEXT PRIMARY KEY,
    username      TEXT UNIQUE,
    display_name  TEXT,
    role          TEXT CHECK (role IN ('owner', 'supervisor', 'staff')),
    job_title     TEXT,
    password_hash TEXT,
    retired_at    TEXT,
    retired_by    TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS auth_sessions (
    token_hash   BYTEA PRIMARY KEY,
    user_id      TEXT NOT NULL REFERENCES users(id),
    expires_at   TIMESTAMP NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS auth_sessions_user_expiry_idx
    ON auth_sessions (user_id, expires_at);

-- ctx-auditoria
CREATE TABLE IF NOT EXISTS audit_log (
    id            TEXT PRIMARY KEY,
    actor_id      TEXT,
    action        TEXT NOT NULL,
    entity_type   TEXT NOT NULL,
    entity_id     TEXT NOT NULL,
    metadata_json TEXT DEFAULT '{}',
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS audit_log_entity_time_idx
    ON audit_log (entity_type, entity_id, created_at DESC);

CREATE INDEX IF NOT EXISTS audit_log_actor_time_idx
    ON audit_log (actor_id, created_at DESC);

-- ctx-residencia
CREATE TABLE IF NOT EXISTS facilities (
    id         TEXT PRIMARY KEY,
    name       TEXT NOT NULL,
    timezone   TEXT NOT NULL DEFAULT 'UTC',
    retired_at TEXT,
    retired_by TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS wings (
    id          TEXT PRIMARY KEY,
    facility_id TEXT NOT NULL REFERENCES facilities(id),
    name        TEXT NOT NULL,
    floor       TEXT,
    sort_order  INTEGER DEFAULT 0,
    retired_at  TEXT,
    retired_by  TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS rooms (
    id         TEXT PRIMARY KEY,
    wing_id    TEXT NOT NULL REFERENCES wings(id),
    number     TEXT NOT NULL,
    room_type  TEXT,
    stream_key TEXT,
    retired_at TEXT,
    retired_by TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS rooms_active_number_idx
    ON rooms (wing_id, number)
    WHERE retired_at IS NULL;

CREATE TABLE IF NOT EXISTS beds (
    id          TEXT PRIMARY KEY,
    room_id     TEXT NOT NULL REFERENCES rooms(id),
    label       TEXT NOT NULL,
    monitor_key TEXT,
    retired_at  TEXT,
    retired_by  TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS planogram_placements (
    id         TEXT PRIMARY KEY,
    wing_id    TEXT NOT NULL REFERENCES wings(id),
    room_id    TEXT NOT NULL REFERENCES rooms(id),
    x          REAL NOT NULL,
    y          REAL NOT NULL,
    sort_order INTEGER DEFAULT 0,
    active     BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS room_privacy_regions (
    id         TEXT PRIMARY KEY,
    room_id    TEXT NOT NULL REFERENCES rooms(id),
    x          REAL NOT NULL,
    y          REAL NOT NULL,
    w          REAL NOT NULL,
    h          REAL NOT NULL,
    active     BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ctx-poblacion
CREATE TABLE IF NOT EXISTS residents (
    id             TEXT PRIMARY KEY,
    external_id    TEXT UNIQUE,
    full_name      TEXT NOT NULL,
    birth_date     DATE,
    admission_date DATE NOT NULL,
    status         TEXT DEFAULT 'active' CHECK (status IN ('active', 'discharged')),
    discharged_at  TIMESTAMP,
    discharged_by  TEXT,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS resident_bed_assignments (
    id          TEXT PRIMARY KEY,
    resident_id TEXT NOT NULL REFERENCES residents(id),
    bed_id      TEXT NOT NULL,
    starts_at   TIMESTAMP NOT NULL,
    ends_at     TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by  TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS residents_open_assignment_idx
    ON resident_bed_assignments (resident_id)
    WHERE ends_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS beds_open_assignment_idx
    ON resident_bed_assignments (bed_id)
    WHERE ends_at IS NULL;

-- ctx-cobertura
CREATE TABLE IF NOT EXISTS staff_groups (
    id          TEXT PRIMARY KEY,
    facility_id TEXT NOT NULL,
    name        TEXT NOT NULL,
    retired_at  TEXT,
    retired_by  TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS facility_shifts (
    id           TEXT PRIMARY KEY,
    facility_id  TEXT NOT NULL,
    key          TEXT NOT NULL,
    label        TEXT NOT NULL,
    start_minute INTEGER NOT NULL CHECK (start_minute BETWEEN 0 AND 1439),
    sort_order   INTEGER DEFAULT 0,
    retired_at   TEXT,
    retired_by   TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS unit_shift_coverages (
    id             TEXT PRIMARY KEY,
    wing_id        TEXT NOT NULL,
    staff_group_id TEXT NOT NULL,
    shift_key      TEXT NOT NULL,
    valid_from     TIMESTAMP NOT NULL DEFAULT NOW(),
    valid_to       TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by     TEXT
);

-- ctx-cuidado
CREATE TABLE IF NOT EXISTS rounds (
    id            TEXT PRIMARY KEY,
    wing_id       TEXT NOT NULL,
    status        TEXT DEFAULT 'in_progress' CHECK (status IN ('in_progress', 'completed', 'cancelled')),
    scheduled_for TIMESTAMP,
    started_at    TIMESTAMP,
    completed_at  TIMESTAMP,
    started_by    TEXT,
    completed_by  TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS rounds_wing_in_progress_idx
    ON rounds (wing_id)
    WHERE status = 'in_progress';

CREATE TABLE IF NOT EXISTS round_tasks (
    id           TEXT PRIMARY KEY,
    round_id     TEXT NOT NULL REFERENCES rounds(id),
    resident_id  TEXT NOT NULL,
    bed_id       TEXT,
    status       TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'completed')),
    note         TEXT,
    completed_at TIMESTAMP,
    completed_by TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS care_notes (
    id           TEXT PRIMARY KEY,
    resident_id  TEXT NOT NULL,
    author_id    TEXT NOT NULL,
    kind         TEXT DEFAULT 'general',
    body         TEXT NOT NULL,
    duration_min INTEGER,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ctx-historia
CREATE TABLE IF NOT EXISTS incident_detections (
    id                 TEXT PRIMARY KEY,
    source_record_id   TEXT UNIQUE NOT NULL,
    resident_id        TEXT NOT NULL,
    bed_id             TEXT,
    source_alert_id    TEXT,
    kind               TEXT NOT NULL,
    severity           TEXT NOT NULL,
    occurred_at        TIMESTAMP NOT NULL,
    location           TEXT,
    activity           TEXT,
    injury_status      TEXT,
    self_recovery      BOOLEAN DEFAULT FALSE,
    response_seconds   INTEGER,
    narrative          TEXT,
    interventions_json TEXT DEFAULT '[]',
    source             TEXT NOT NULL,
    model_version      TEXT,
    confidence         REAL,
    provenance_json    TEXT DEFAULT '{}',
    created_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS incident_reviews (
    id                TEXT PRIMARY KEY,
    incident_id       TEXT NOT NULL,
    status            TEXT NOT NULL,
    detection_verdict TEXT,
    review_note       TEXT,
    resolved_at       TIMESTAMP,
    actor_id          TEXT NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ctx-politica
CREATE TABLE IF NOT EXISTS alarm_profile_versions (
    id              TEXT PRIMARY KEY,
    resident_id     TEXT NOT NULL,
    valid_from      TIMESTAMP NOT NULL DEFAULT NOW(),
    valid_to        TIMESTAMP,
    mobility_aid    TEXT,
    autopilot       BOOLEAN DEFAULT FALSE,
    mode            TEXT,
    template_id     TEXT,
    overrides_json  TEXT DEFAULT '{}',
    catalog_version TEXT,
    updated_by      TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    risk_level      TEXT DEFAULT 'medium'
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_alarm_profiles_one_current
    ON alarm_profile_versions (resident_id)
    WHERE valid_to IS NULL;

-- ctx-vigilancia
CREATE TABLE IF NOT EXISTS alerts (
    id               TEXT PRIMARY KEY,
    resident_id      TEXT NOT NULL,
    bed_id           TEXT,
    evidence_kind    TEXT,
    evidence_ref     TEXT,
    rule_id          TEXT,
    level            TEXT NOT NULL,
    status           TEXT NOT NULL DEFAULT 'pending',
    status_actor_id  TEXT,
    status_at        TIMESTAMP,
    title            TEXT,
    detail           TEXT,
    occurred_at      TIMESTAMP NOT NULL,
    escalation_level INTEGER DEFAULT 0,
    escalated_at     TIMESTAMP,
    escalated_to     TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS alert_transitions (
    id          TEXT PRIMARY KEY,
    alert_id    TEXT NOT NULL REFERENCES alerts(id),
    from_status TEXT,
    to_status   TEXT NOT NULL,
    actor_id    TEXT NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    sequence    INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS notification_deliveries (
    id               TEXT PRIMARY KEY,
    alert_id         TEXT NOT NULL REFERENCES alerts(id),
    recipient_kind   TEXT NOT NULL,
    recipient_id     TEXT NOT NULL,
    channel          TEXT NOT NULL,
    escalation_level INTEGER DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notification_delivery_events (
    id          TEXT PRIMARY KEY,
    delivery_id TEXT NOT NULL REFERENCES notification_deliveries(id),
    kind        TEXT NOT NULL,
    reason      TEXT,
    occurred_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS alert_escalations (
    id          TEXT PRIMARY KEY,
    alert_id    TEXT NOT NULL REFERENCES alerts(id),
    level       INTEGER NOT NULL,
    target_id   TEXT NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ctx-evidence
CREATE TABLE IF NOT EXISTS evidence (
    id               TEXT PRIMARY KEY,
    bed_id           TEXT NOT NULL,
    resident_id      TEXT NOT NULL,
    evidence_type    TEXT NOT NULL,
    category         TEXT,
    scene_event_id   TEXT,
    scene_event_json TEXT,
    rule_id          TEXT,
    shift            TEXT,
    risk_level       TEXT,
    timestamp        TIMESTAMP NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS timelines (
    id                 TEXT PRIMARY KEY,
    bed_id             TEXT NOT NULL,
    resident_id        TEXT NOT NULL,
    anchor_event_id    TEXT,
    anchor_event_json  TEXT,
    before_events_json TEXT DEFAULT '[]',
    after_events_json  TEXT DEFAULT '[]',
    window_start       TIMESTAMP NOT NULL,
    window_end         TIMESTAMP,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    closed_at          TIMESTAMP
);

CREATE TABLE IF NOT EXISTS clip_windows (
    window_id            TEXT PRIMARY KEY,
    bed_id               TEXT NOT NULL,
    resident_id          TEXT NOT NULL,
    started_at           TIMESTAMP NOT NULL,
    ended_at             TIMESTAMP,
    timeout_minutes      INTEGER DEFAULT 5,
    events_json          TEXT DEFAULT '[]',
    state                TEXT DEFAULT 'open',
    close_condition_json TEXT,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    closed_at            TIMESTAMP
);

-- ctx-streams
CREATE TABLE IF NOT EXISTS streams (
    id         TEXT PRIMARY KEY,
    room_id    TEXT NOT NULL,
    stream_key TEXT NOT NULL,
    name       TEXT,
    active     BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS streams_active_room_key_idx
    ON streams (room_id, stream_key)
    WHERE active = TRUE;

CREATE TABLE IF NOT EXISTS stream_regions (
    id          TEXT PRIMARY KEY,
    stream_id   TEXT NOT NULL REFERENCES streams(id),
    region_type TEXT NOT NULL CHECK (region_type IN (
        'bathroom', 'hallway', 'exit', 'bed', 'furniture', 'person', 'object'
    )),
    points      TEXT NOT NULL,
    label       TEXT,
    is_static   BOOLEAN DEFAULT TRUE,
    updated_by  TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- mana-observation
CREATE TABLE IF NOT EXISTS sensor_events (
    id              TEXT PRIMARY KEY,
    source_event_id TEXT UNIQUE NOT NULL,
    monitor_key     TEXT NOT NULL,
    bed_id          TEXT,
    resident_id     TEXT,
    kind            TEXT NOT NULL,
    room_state      TEXT,
    substate        TEXT,
    zone            TEXT,
    state           TEXT,
    sleeping        BOOLEAN,
    occurred_at     TIMESTAMP NOT NULL,
    received_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    payload_json    TEXT DEFAULT '{}'
);

CREATE INDEX IF NOT EXISTS idx_sensor_events_unresolved
    ON sensor_events (monitor_key)
    WHERE bed_id IS NULL;

CREATE TABLE IF NOT EXISTS current_bed_states (
    bed_id          TEXT PRIMARY KEY,
    resident_id     TEXT,
    room_state      TEXT,
    state           TEXT,
    substate        TEXT,
    sleeping        BOOLEAN,
    state_since     TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    source          TEXT,
    source_event_id TEXT
);

CREATE TABLE IF NOT EXISTS scene_events (
    id           TEXT PRIMARY KEY,
    event_id     TEXT UNIQUE NOT NULL,
    bed_id       TEXT NOT NULL,
    resident_id  TEXT,
    event_type   TEXT NOT NULL,
    from_state   TEXT,
    to_state     TEXT,
    trigger_type TEXT,
    timestamp    TIMESTAMP NOT NULL,
    payload_json TEXT DEFAULT '{}',
    received_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notification_events (
    id           TEXT PRIMARY KEY,
    category     TEXT NOT NULL,
    bed_id       TEXT,
    resident_id  TEXT,
    event_type   TEXT NOT NULL,
    timestamp    TIMESTAMP NOT NULL,
    rule_id      TEXT,
    risk_level   TEXT,
    payload_json TEXT DEFAULT '{}',
    received_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sleep_summaries (
    id                 TEXT PRIMARY KEY,
    source_record_id   TEXT UNIQUE NOT NULL,
    resident_id        TEXT NOT NULL,
    observed_on        DATE NOT NULL,
    calm_minutes       INTEGER DEFAULT 0,
    restless_minutes   INTEGER DEFAULT 0,
    awake_minutes      INTEGER DEFAULT 0,
    out_of_bed_minutes INTEGER DEFAULT 0,
    bed_exit_count     INTEGER DEFAULT 0,
    wake_count         INTEGER DEFAULT 0,
    source             TEXT,
    model_version      TEXT,
    confidence         REAL,
    provenance_json    TEXT DEFAULT '{}',
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_sleep_summaries_resident_day
    ON sleep_summaries (resident_id, observed_on);

CREATE TABLE IF NOT EXISTS mobility_summaries (
    id                   TEXT PRIMARY KEY,
    source_record_id     TEXT UNIQUE NOT NULL,
    resident_id          TEXT NOT NULL,
    observed_on          DATE NOT NULL,
    in_bed_minutes       INTEGER DEFAULT 0,
    out_of_bed_minutes   INTEGER DEFAULT 0,
    out_of_sight_minutes INTEGER DEFAULT 0,
    walking_minutes      INTEGER DEFAULT 0,
    distance_meters      REAL DEFAULT 0,
    transfer_count       INTEGER DEFAULT 0,
    source               TEXT,
    model_version        TEXT,
    confidence           REAL,
    provenance_json      TEXT DEFAULT '{}',
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mobility_summaries_resident_day
    ON mobility_summaries (resident_id, observed_on);

CREATE TABLE IF NOT EXISTS bathroom_summaries (
    id                    TEXT PRIMARY KEY,
    source_record_id      TEXT UNIQUE NOT NULL,
    resident_id           TEXT NOT NULL,
    observed_on           DATE NOT NULL,
    visit_count           INTEGER DEFAULT 0,
    night_visit_count     INTEGER DEFAULT 0,
    assisted_count        INTEGER DEFAULT 0,
    total_minutes         INTEGER DEFAULT 0,
    source                TEXT,
    model_version         TEXT,
    confidence            REAL,
    provenance_json       TEXT DEFAULT '{}',
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_bathroom_summaries_resident_day
    ON bathroom_summaries (resident_id, observed_on);
