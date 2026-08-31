-- La tabla que nadie creaba.
--
-- `V16` y `V17` le agregan columnas a `sentinel_signals`, pero ninguna migración
-- la creaba: nació fuera del control de esquema —Hibernate en algún momento, o a
-- mano— y las migraciones se escribieron dando por hecho que ya estaba.
--
-- Mientras Flyway estuvo apagado eso no se notaba. Al prenderlo, aplicar desde
-- cero falla en V16 con `relation "sentinel_signals" does not exist`, que es la
-- forma en que una base y su código te avisan que hace rato dejaron de coincidir.
--
-- Las columnas base son las del entity `SentinelSignalEntity`; las que agregan
-- V16 y V17 se dejan a esas migraciones, para no duplicar la definición ni
-- adelantarme a lo que ellas ya dicen.
CREATE TABLE IF NOT EXISTS sentinel_signals (
    id           TEXT PRIMARY KEY,
    signal_id    TEXT UNIQUE NOT NULL,
    bed_id       TEXT NOT NULL,
    resident_id  TEXT,
    episode_id   TEXT,
    type         TEXT NOT NULL,
    severity     TEXT,
    trigger_type TEXT,
    timestamp    TIMESTAMP NOT NULL,
    payload_json TEXT DEFAULT '{}',
    received_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sentinel_signals_resident ON sentinel_signals (resident_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_sentinel_signals_episode  ON sentinel_signals (episode_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_sentinel_signals_bed      ON sentinel_signals (bed_id, timestamp);
