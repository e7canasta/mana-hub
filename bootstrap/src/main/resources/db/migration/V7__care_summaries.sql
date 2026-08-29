-- Care summaries: agregación diaria de atención por residente
-- Se puede alimentar de round_tasks + care_notes

CREATE TABLE IF NOT EXISTS care_summaries (
    id                 TEXT PRIMARY KEY,
    source_record_id   TEXT UNIQUE NOT NULL,
    resident_id        TEXT NOT NULL,
    observed_on        DATE NOT NULL,
    total_minutes      INTEGER DEFAULT 0,
    proactive_minutes  INTEGER DEFAULT 0,
    rounds_count       INTEGER DEFAULT 0,
    notes_count        INTEGER DEFAULT 0,
    source             TEXT,
    model_version      TEXT,
    confidence         REAL,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_care_summaries_resident_day
    ON care_summaries (resident_id, observed_on);
