-- La columna `version` que dos tablas nunca tuvieron.
--
-- `V2__add_version_columns.sql` se la agregó a todo lo que existía en ese
-- momento. `care_summaries` (V7) y `staff_members` (V6) se crearon **después** y
-- se olvidaron de incluirla, aunque sus entities la mapean para el bloqueo
-- optimista.
--
-- El síntoma era desproporcionado respecto de la causa: `GET .../care` devolvía
-- 500 con `column cse1_0.version does not exist`, y el panel —que pedía las once
-- series de la ficha con un `Promise.all`— perdía la ficha entera y mostraba
-- "No hay ningún residente con el id jose" sobre alguien que existe. Una columna
-- faltante se presentaba como un residente inexistente.
--
-- Estas dos son las únicas: verificado cruzando las 20 entities que declaran
-- `version` contra el esquema real.
ALTER TABLE care_summaries ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE staff_members  ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
