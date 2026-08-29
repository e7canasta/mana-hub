-- V11: Outbox for hub → hive policy propagation (Transactional Outbox pattern)
-- Published to NATS hub.policy.change.v1 / hub.policy.effective-rules.v1.<resident>
CREATE TABLE IF NOT EXISTS hub_policy_outbox (
    id            TEXT PRIMARY KEY,
    aggregate_id  TEXT NOT NULL, -- residentId
    type          TEXT NOT NULL, -- PolicyChangeDetected | EffectiveRules
    payload_json  TEXT NOT NULL,
    occurred_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    published     BOOLEAN NOT NULL DEFAULT FALSE,
    attempts      INTEGER NOT NULL DEFAULT 0,
    last_error    TEXT
);
CREATE INDEX IF NOT EXISTS idx_hub_outbox_unpublished ON hub_policy_outbox (published, occurred_at) WHERE published = FALSE;
CREATE INDEX IF NOT EXISTS idx_hub_outbox_aggregate ON hub_policy_outbox (aggregate_id, occurred_at DESC);
