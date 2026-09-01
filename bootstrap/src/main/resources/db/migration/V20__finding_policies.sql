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
    sleep           JSONB NOT NULL DEFAULT '{}',
    care            JSONB NOT NULL DEFAULT '{}',
    bathroom        JSONB NOT NULL DEFAULT '{}',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_finding_policies_resident ON finding_policies (resident_id);
CREATE UNIQUE INDEX idx_finding_policies_default ON finding_policies (is_default) WHERE is_default = true;
