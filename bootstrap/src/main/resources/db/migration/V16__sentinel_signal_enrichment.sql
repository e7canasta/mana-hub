-- V16: SentinelSignal enrichment — desnormaliza payload para reporte sin JSON parse
-- Espejo V15 (twin_snapshot en scene_events): rule/cause/triggerOn/state + fingerprint
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS rule_id TEXT;
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS field TEXT;
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS trigger_on TEXT;
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS cause TEXT;
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS state TEXT;
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS baseline TEXT;
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS rules_fingerprint TEXT;
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS gap_duration TEXT;
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS previous_severity TEXT;
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS original_severity TEXT;
ALTER TABLE sentinel_signals ADD COLUMN IF NOT EXISTS payload_jsonb JSONB;

-- Backfill payload_jsonb from existing payload_json
UPDATE sentinel_signals SET payload_jsonb = payload_json::jsonb WHERE payload_jsonb IS NULL AND payload_json IS NOT NULL;

-- Backfill desnormalizados desde payload_jsonb (toMap() de contracts/sentinel/SentinelSignal.kt)
UPDATE sentinel_signals SET rule_id = payload_jsonb->>'rule' WHERE rule_id IS NULL AND payload_jsonb ? 'rule' AND payload_jsonb->>'rule' <> 'unknown' AND payload_jsonb->>'rule' <> 'none';
UPDATE sentinel_signals SET field = payload_jsonb->>'field' WHERE field IS NULL AND payload_jsonb ? 'field';
UPDATE sentinel_signals SET trigger_on = payload_jsonb->>'triggerOn' WHERE trigger_on IS NULL AND payload_jsonb ? 'triggerOn';
UPDATE sentinel_signals SET cause = payload_jsonb->>'cause' WHERE cause IS NULL AND payload_jsonb ? 'cause';
UPDATE sentinel_signals SET state = payload_jsonb->>'state' WHERE state IS NULL AND payload_jsonb ? 'state' AND payload_jsonb->>'state' <> 'none';
UPDATE sentinel_signals SET baseline = payload_jsonb->>'baseline' WHERE baseline IS NULL AND payload_jsonb ? 'baseline';
UPDATE sentinel_signals SET rules_fingerprint = payload_jsonb->>'rulesFingerprint' WHERE rules_fingerprint IS NULL AND payload_jsonb ? 'rulesFingerprint';
UPDATE sentinel_signals SET gap_duration = payload_jsonb->>'gapDuration' WHERE gap_duration IS NULL AND payload_jsonb ? 'gapDuration';
UPDATE sentinel_signals SET previous_severity = payload_jsonb->>'previousSeverity' WHERE previous_severity IS NULL AND payload_jsonb ? 'previousSeverity';
UPDATE sentinel_signals SET original_severity = payload_jsonb->>'originalSeverity' WHERE original_severity IS NULL AND payload_jsonb ? 'originalSeverity';
-- Si el signal no trae rule pero trigger es StateKind (EPISODE_OPENED viejo), trigger ya está en trigger_type; no sobre-escribir rule_id

CREATE INDEX IF NOT EXISTS idx_sentinel_signals_rule ON sentinel_signals(rule_id);
CREATE INDEX IF NOT EXISTS idx_sentinel_signals_cause ON sentinel_signals(cause);
