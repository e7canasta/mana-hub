-- V12 creó policy_recommendations sin patch_autopilot, pero PolicyRecommendationEntity
-- lo mapea. Hibernate ddl-auto:none lo creó silenciosamente en dev, pero en una DB
-- limpia con solo Flyway falla al arrancar.
--
-- Esta migración agrega la columna que falta para que el esquema refleje la entity.

ALTER TABLE policy_recommendations
    ADD COLUMN IF NOT EXISTS patch_autopilot BOOLEAN;
