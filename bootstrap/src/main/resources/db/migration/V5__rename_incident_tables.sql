-- Rename incident tables to history_episode tables
-- Matches the HistoryEpisode* class rename in the history module

ALTER TABLE IF EXISTS incident_detections RENAME TO history_episode_detections;
ALTER TABLE IF EXISTS incident_reviews RENAME TO history_episode_reviews;

-- Rename the foreign key column for clarity
ALTER TABLE history_episode_reviews RENAME COLUMN incident_id TO episode_id;
