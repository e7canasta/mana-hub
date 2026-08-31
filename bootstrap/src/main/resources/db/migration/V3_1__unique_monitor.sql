-- Garantiza unicidad de monitor por cama activa a nivel base (red de seguridad definitiva).
CREATE UNIQUE INDEX IF NOT EXISTS beds_active_monitor_idx ON beds (monitor_key) WHERE monitor_key IS NOT NULL AND retired_at IS NULL;
