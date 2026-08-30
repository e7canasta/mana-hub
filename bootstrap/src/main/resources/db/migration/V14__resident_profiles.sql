CREATE TABLE IF NOT EXISTS resident_profiles (
    id              TEXT PRIMARY KEY,
    resident_id     TEXT NOT NULL,
    profile_id      TEXT NOT NULL,
    version         INTEGER NOT NULL,
    supersedes      INTEGER,
    valid_from      TIMESTAMP NOT NULL,
    provenance_json TEXT NOT NULL DEFAULT '{}',
    windows_json    TEXT NOT NULL DEFAULT '[]',
    subjects_json   TEXT NOT NULL DEFAULT '{}',
    raw_json        TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_rp_resident_version
    ON resident_profiles (resident_id, version);

CREATE UNIQUE INDEX IF NOT EXISTS idx_rp_resident_current
    ON resident_profiles (resident_id)
    WHERE supersedes IS NULL;
