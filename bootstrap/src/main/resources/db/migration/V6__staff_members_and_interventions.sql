-- Staff members: personas que trabajan en la residencia
-- Pueden o no tener cuenta de usuario (userId nullable)

CREATE TABLE IF NOT EXISTS staff_members (
    id          TEXT PRIMARY KEY,
    facility_id TEXT NOT NULL,
    full_name   TEXT NOT NULL,
    role        TEXT NOT NULL CHECK (role IN ('NURSE','DOCTOR','CAREGIVER','PHYSIOTHERAPIST','SOCIAL_WORKER','ADMINISTRATOR','OTHER')),
    user_id     TEXT,
    retired_at  TEXT,
    retired_by  TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_staff_members_facility
    ON staff_members (facility_id)
    WHERE retired_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_staff_members_user
    ON staff_members (user_id)
    WHERE user_id IS NOT NULL AND retired_at IS NULL;

-- History episode interventions: tabla normalizada (antes era interventionsJson)

CREATE TABLE IF NOT EXISTS history_episode_interventions (
    id            TEXT PRIMARY KEY,
    episode_id    TEXT NOT NULL REFERENCES history_episode_detections(id) ON DELETE CASCADE,
    kind          TEXT NOT NULL CHECK (kind IN (
        'FAMILY_NOTIFIED','STAFF_DISPATCHED','BANDAGE_APPLIED',
        'MEDICATION_GIVEN','TRANSFERRED_TO_HOSPITAL','REPOSITIONED',
        'CALLED_FOR_HELP','VITAL_SIGNS_CHECKED','OTHER'
    )),
    performed_at  TIMESTAMP NOT NULL,
    performed_by  TEXT REFERENCES staff_members(id),
    detail        TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_episode_interventions_episode
    ON history_episode_interventions (episode_id);

CREATE INDEX IF NOT EXISTS idx_episode_interventions_kind
    ON history_episode_interventions (kind, performed_at);
