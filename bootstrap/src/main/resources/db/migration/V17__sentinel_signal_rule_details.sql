-- V17: regla audit sin JSON repetido — columnas atómicas para reporte sin join
-- Solo lo que ya viene en payload (toMap) — no duplica EffectiveRules entero
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS reversible BOOLEAN;
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS requires_nvr BOOLEAN;
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS confirmation_window TEXT;
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS requires_confirmation BOOLEAN;
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS elapsed TEXT;
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS threshold TEXT;
-- closure_condition se agregará cuando hive lo incluya en toMap (AlertRule.closureCondition)

-- Backfill desde payload_jsonb (V16 ya lo populó desde payload_json)
UPDATE sentinel_signals SET reversible = (payload_jsonb->>'reversible')::BOOLEAN WHERE reversible IS NULL AND payload_jsonb ? 'reversible';
UPDATE sentinel_signals SET requires_nvr = (payload_jsonb->>'requiresNvr')::BOOLEAN WHERE requires_nvr IS NULL AND payload_jsonb ? 'requiresNvr';
UPDATE sentinel_signals SET confirmation_window = payload_jsonb->>'confirmationWindow' WHERE confirmation_window IS NULL AND payload_jsonb ? 'confirmationWindow';
UPDATE sentinel_signals SET requires_confirmation = (payload_jsonb->>'requiresConfirmation')::BOOLEAN WHERE requires_confirmation IS NULL AND payload_jsonb ? 'requiresConfirmation';
UPDATE sentinel_signals SET elapsed = payload_jsonb->>'elapsed' WHERE elapsed IS NULL AND payload_jsonb ? 'elapsed';
UPDATE sentinel_signals SET threshold = payload_jsonb->>'threshold' WHERE threshold IS NULL AND payload_jsonb ? 'threshold';
