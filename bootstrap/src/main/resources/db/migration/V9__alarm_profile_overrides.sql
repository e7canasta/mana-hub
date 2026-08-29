CREATE TABLE alarm_profile_overrides (
    id TEXT PRIMARY KEY,
    profile_version_id TEXT NOT NULL REFERENCES alarm_profile_versions(id),
    rule_id TEXT NOT NULL,
    override_type TEXT NOT NULL,
    state_kind TEXT,
    transition_key TEXT,
    warning_after_minutes INTEGER,
    alert_after_minutes INTEGER,
    hysteresis_seconds INTEGER,
    baseline_state TEXT,
    severity TEXT,
    closure_condition TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(profile_version_id, rule_id)
);
