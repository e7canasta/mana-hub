-- SOURCE OF TRUTH KEYWORDS: monitoring_episode, trigger_type, posture, V26
-- WHAT: Keeps the structured monitoring trigger on the persisted episode.
-- WHY:  A trigger is a domain value used for notification context and must not
--       be reduced to free-form detail text before Hub publishes the episode.
-- WHERE: Read by surveillance EpisodeEntity and emitted in EpisodeCreated.

ALTER TABLE episodes ADD COLUMN trigger_type TEXT;
