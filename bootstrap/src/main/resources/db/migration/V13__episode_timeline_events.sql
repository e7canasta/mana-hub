CREATE TABLE IF NOT EXISTS episode_timeline_events (
    id              VARCHAR(36) PRIMARY KEY,
    episode_id      VARCHAR(36) NOT NULL,
    resident_id     VARCHAR(36) NOT NULL,
    at              TIMESTAMP NOT NULL,
    type            VARCHAR(30) NOT NULL,
    from_state      VARCHAR(50),
    to_state        VARCHAR(50),
    description     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_ete_episode ON episode_timeline_events (episode_id, at);
CREATE INDEX idx_ete_resident ON episode_timeline_events (resident_id, at);

-- Seed: timeline para María García — episodio de deambulación nocturna
INSERT INTO episode_timeline_events (id, episode_id, resident_id, at, type, from_state, to_state, description)
VALUES
('evt-001', 'ep-seed-001', '8780cd5a-59d6-4757-8229-eee2a5f6979a',
 '2026-08-29 02:15:00', 'OPENED', NULL, 'standing',
 'Sensor detecta movimiento. Residente se levanta de la cama.'),

('evt-002', 'ep-seed-001', '8780cd5a-59d6-4757-8229-eee2a5f6979a',
 '2026-08-29 02:18:00', 'UMBRELLA', 'standing', 'bathroom',
 'Residente ingresa al baño.'),

('evt-003', 'ep-seed-001', '8780cd5a-59d6-4757-8229-eee2a5f6979a',
 '2026-08-29 02:25:00', 'ESCALATED', 'warning', 'critical',
 'Residente permanece en baño más de 10 minutos. Severidad sube a CRITICAL.'),

('evt-004', 'ep-seed-001', '8780cd5a-59d6-4757-8229-eee2a5f6979a',
 '2026-08-29 02:32:00', 'UMBRELLA', 'bathroom', 'standing',
 'Residente sale del baño.'),

('evt-005', 'ep-seed-001', '8780cd5a-59d6-4757-8229-eee2a5f6979a',
 '2026-08-29 02:35:00', 'RECOVERY', 'standing', 'lying',
 'Residente vuelve a la cama y se acuesta.'),

('evt-006', 'ep-seed-001', '8780cd5a-59d6-4757-8229-eee2a5f6979a',
 '2026-08-29 02:36:00', 'CLOSED', 'lying', NULL,
 'Episodio cerrado. Residente seguro, sin intervención de staff.');
