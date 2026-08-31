-- Cuándo se registró la revisión, además de cuándo ocurrió.
--
-- `resolved_at` lleva el reloj del dominio, que durante un escenario está en
-- manual y puede quedar en el futuro respecto del tiempo real. Ordenar por él
-- para saber cuál es "la última revisión" falla exactamente ahí: una fila
-- estampada el 4 de septiembre gana para siempre sobre todas las que se
-- escriban después en tiempo real, y reclasificar un episodio deja de tener
-- efecto visible.
--
-- Son dos preguntas distintas y hacían falta dos columnas:
--   resolved_at  → cuándo pasó, en la escala del residente. Es lo que se muestra.
--   recorded_at  → cuándo se guardó la fila. Monótono, del reloj de la base,
--                  y sirve para ordenar.
--
-- `DEFAULT now()` a propósito: lo pone Postgres, no la aplicación, así que no
-- hay forma de que un reloj simulado se cuele acá.
ALTER TABLE history_episode_reviews
    ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMP NOT NULL DEFAULT now();

-- Las filas que ya existen se ordenan por lo único que tenían. No es exacto
-- para las que quedaron con hora simulada, y no puede serlo: ese dato se
-- perdió. De acá en adelante sí.
UPDATE history_episode_reviews
SET recorded_at = resolved_at
WHERE recorded_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_history_episode_reviews_recorded
    ON history_episode_reviews (episode_id, recorded_at DESC);
