-- Finding policies: umbrales de evaluación de hallazgos por residente.
--
-- Cada residente puede tener su propia política. Si no tiene,
-- se usa la default (is_default = true).
-- Cada regla se puede prender/apagar individualmente.
--
-- Los JSONB almacenan los value objects SleepPolicy, CarePolicy,
-- BathroomPolicy tal cual se serializan — sin transformación.

CREATE TABLE finding_policies (
    id              TEXT PRIMARY KEY,
    resident_id     TEXT UNIQUE,
    is_default      BOOLEAN NOT NULL DEFAULT false,
    sleep           JSONB NOT NULL DEFAULT '{
        "restlessHighEnabled": true,
        "restlessHighThreshold": 0.25,
        "restlessFragmentedEnabled": true,
        "restlessFragmentedThreshold": 0.35,
        "exitsRisingEnabled": true,
        "exitsRisingFactor": 1.15,
        "exitsRisingMinDelta": 0.3,
        "sleepInRangeEnabled": true,
        "sleepInRangeThreshold": 0.20,
        "dropWoWEnabled": true,
        "dropWoWMinutes": 45,
        "dawnClusterEnabled": true,
        "dawnFrom": "05:00",
        "dawnTo": "06:05",
        "dawnMinCount": 3,
        "dawnRatio": 0.66
    }',
    care            JSONB NOT NULL DEFAULT '{
        "careThinEnabled": true,
        "careThinMinutes": 20.0
    }',
    bathroom        JSONB NOT NULL DEFAULT '{
        "bathroomNightEnabled": true,
        "nightMinAvg": 1.0,
        "nightRiseFactor": 1.5
    }',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_finding_policies_resident ON finding_policies (resident_id);
CREATE UNIQUE INDEX idx_finding_policies_default ON finding_policies (is_default) WHERE is_default = true;
