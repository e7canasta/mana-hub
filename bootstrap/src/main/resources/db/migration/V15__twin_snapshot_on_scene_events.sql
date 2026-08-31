-- V15: TwinSnapshot on scene_events — snapshot without domicile (bed/night in envelope)
ALTER TABLE scene_events ADD COLUMN IF NOT EXISTS twin_snapshot JSONB NOT NULL DEFAULT '{}';
ALTER TABLE scene_events ADD COLUMN IF NOT EXISTS state_since TIMESTAMP;
ALTER TABLE scene_events ADD COLUMN IF NOT EXISTS scene_since TIMESTAMP;
ALTER TABLE scene_events ADD COLUMN IF NOT EXISTS signal_lost BOOLEAN;
ALTER TABLE scene_events ADD COLUMN IF NOT EXISTS monitor_id TEXT;

-- Backfill already-persisted rows: extract from payload_json if twinSnapshot present
UPDATE scene_events
SET twin_snapshot = payload_json::jsonb -> 'twinSnapshot'
WHERE payload_json::jsonb ? 'twinSnapshot'
  AND (twin_snapshot IS NULL OR twin_snapshot = '{}'::jsonb);

UPDATE scene_events
SET
    state_since = NULLIF((twin_snapshot->>'stateSince'), '')::TIMESTAMP,
    scene_since = NULLIF((twin_snapshot->>'sceneSince'), '')::TIMESTAMP,
    signal_lost = NULLIF((twin_snapshot->>'signalLost'), '')::BOOLEAN,
    monitor_id  = twin_snapshot->>'monitor'
WHERE twin_snapshot IS NOT NULL
  AND twin_snapshot <> '{}'::jsonb;
