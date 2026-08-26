-- V4__create_notes_tables.sql
-- Create ResidentNote, EpisodeNote, and ShiftNote tables
-- ResidentNote kinds: CARE, CLINICAL, INSIGHT, PATTERN, OBSERVATION, SUMMARY

-- 1. ResidentNote: ALL notes about a resident
CREATE TABLE resident_notes (
    id              TEXT PRIMARY KEY,
    resident_id     TEXT NOT NULL,
    author_id       TEXT NOT NULL,
    kind            TEXT NOT NULL CHECK (kind IN ('CARE', 'CLINICAL', 'INSIGHT', 'PATTERN', 'OBSERVATION', 'SUMMARY')),
    body            TEXT NOT NULL,
    source_event_id TEXT,
    timestamp       TEXT NOT NULL,
    created_at      TEXT NOT NULL DEFAULT (now()),
    updated_at      TEXT NOT NULL DEFAULT (now())
);

CREATE INDEX idx_resident_notes_resident ON resident_notes(resident_id);
CREATE INDEX idx_resident_notes_timestamp ON resident_notes(timestamp);

-- 2. EpisodeNote: Notes about a specific episode
CREATE TABLE episode_notes (
    id              TEXT PRIMARY KEY,
    episode_id      TEXT NOT NULL,
    author_id       TEXT NOT NULL,
    kind            TEXT NOT NULL CHECK (kind IN ('ACKNOWLEDGEMENT', 'RESOLUTION', 'CLINICAL_NOTE')),
    body            TEXT NOT NULL,
    timestamp       TEXT NOT NULL,
    created_at      TEXT NOT NULL DEFAULT (now())
);

CREATE INDEX idx_episode_notes_episode ON episode_notes(episode_id);

-- 3. ShiftNote: Notes about a shift/turno
CREATE TABLE shift_notes (
    id              TEXT PRIMARY KEY,
    facility_id     TEXT NOT NULL,
    wing_id         TEXT,
    shift_key       TEXT NOT NULL,
    shift_date      TEXT NOT NULL,
    author_id       TEXT NOT NULL,
    kind            TEXT NOT NULL CHECK (kind IN ('SHIFT_SUMMARY', 'INCIDENT_REPORT', 'GENERAL')),
    body            TEXT NOT NULL,
    timestamp       TEXT NOT NULL,
    created_at      TEXT NOT NULL DEFAULT (now())
);

CREATE INDEX idx_shift_notes_facility ON shift_notes(facility_id, shift_date);
CREATE INDEX idx_shift_notes_wing ON shift_notes(wing_id, shift_date) WHERE wing_id IS NOT NULL;
