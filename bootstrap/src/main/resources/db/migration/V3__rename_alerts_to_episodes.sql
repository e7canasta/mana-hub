-- V3__rename_alerts_to_episodes.sql
-- Rename alerts tables to episodes to align with domain vocabulary

-- 1. Rename main table
ALTER TABLE alerts RENAME TO episodes;

-- 2. Rename child tables
ALTER TABLE alert_transitions RENAME TO episode_transitions;
ALTER TABLE alert_escalations RENAME TO episode_escalations;

-- 3. Rename foreign key columns in child tables
-- episode_transitions.alert_id → episode_id
ALTER TABLE episode_transitions RENAME COLUMN alert_id TO episode_id;

-- episode_escalations.alert_id → episode_id
ALTER TABLE episode_escalations RENAME COLUMN alert_id TO episode_id;

-- notification_deliveries.alert_id → episode_id
ALTER TABLE notification_deliveries RENAME COLUMN alert_id TO episode_id;

-- 4. Rename severity column (level → severity)
ALTER TABLE episodes RENAME COLUMN level TO severity;

-- 5. Rename source_alert_id in incident_detections
ALTER TABLE incident_detections RENAME COLUMN source_alert_id TO source_episode_id;

-- 6. Recreate indexes with new names
-- Drop old indexes if they exist (SQLite auto-creates indexes for FKs)
-- Create new indexes for the renamed tables
CREATE INDEX IF NOT EXISTS idx_episodes_status ON episodes(status);
CREATE INDEX IF NOT EXISTS idx_episodes_resident ON episodes(resident_id);
CREATE INDEX IF NOT EXISTS idx_episodes_occurred ON episodes(occurred_at);

CREATE INDEX IF NOT EXISTS idx_episode_transitions_episode ON episode_transitions(episode_id);
CREATE INDEX IF NOT EXISTS idx_episode_escalations_episode ON episode_escalations(episode_id);
CREATE INDEX IF NOT EXISTS idx_notification_deliveries_episode ON notification_deliveries(episode_id);
