CREATE TABLE IF NOT EXISTS policy_recommendations (
    id              VARCHAR(36) PRIMARY KEY,
    episode_id      VARCHAR(36) NOT NULL,
    resident_id     VARCHAR(36) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT NOT NULL,
    origin          VARCHAR(20) NOT NULL,
    state           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- preset patch (json parcial)
    patch_template_id VARCHAR(50),
    patch_mode        VARCHAR(50),
    patch_risk_level  VARCHAR(20),
    patch_mobility_aid VARCHAR(50),
    --
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMP,
    applied_at      TIMESTAMP
);

CREATE INDEX idx_pr_resident_state ON policy_recommendations (resident_id, state);

-- Seed: 2 recomendaciones pendientes para María García
INSERT INTO policy_recommendations (id, episode_id, resident_id, title, description, origin, state, patch_template_id, created_at)
VALUES
('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'ep-seed-001', '8780cd5a-59d6-4757-8229-eee2a5f6979a',
 'Aumentar monitoreo nocturno', 'Se detectaron 3 episodios de deambulación esta semana. Se sugiere cambiar a template intensivo para el turno nocturno.',
 'AUTOMATIC', 'PENDING', 'intensivo', now()),

('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'ep-seed-002', '8780cd5a-59d6-4757-8229-eee2a5f6979a',
 'Agregar silla de ruedas como ayuda de movilidad', 'El residente ha sido visto usando silla de ruedas en 2 de los últimos 5 eventos. Se recomienda actualizar su perfil de movilidad.',
 'COPILOT', 'PENDING', NULL, now());
