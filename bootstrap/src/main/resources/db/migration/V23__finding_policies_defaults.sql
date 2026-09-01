-- Agrega defaults completos a finding_policies para columnas JSONB.
-- Migración anterior (V22) usaba '{}', ahora los defaults incluyen
-- todos los campos de SleepPolicy, CarePolicy y BathroomPolicy.

ALTER TABLE finding_policies ALTER COLUMN sleep SET DEFAULT '{
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
}'::jsonb;

ALTER TABLE finding_policies ALTER COLUMN care SET DEFAULT '{
    "careThinEnabled": true,
    "careThinMinutes": 20.0
}'::jsonb;

ALTER TABLE finding_policies ALTER COLUMN bathroom SET DEFAULT '{
    "bathroomNightEnabled": true,
    "nightMinAvg": 1.0,
    "nightRiseFactor": 1.5
}'::jsonb;
