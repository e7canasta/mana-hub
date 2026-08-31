-- Apagar una regla sin borrarla.
--
-- Es el `observeOnly` de `ProfileStateRule` en los contratos de mana-hive: el
-- estado se sigue observando y no genera ningún aviso. Hacía falta una columna
-- propia porque "sin plazo" ya significa otra cosa —seguir al catálogo— así que
-- no había forma de expresar "callá esta regla".
--
-- Y una regla de permanencia no es obligatoria: que el catálogo del motor le
-- ponga un tiempo no quiere decir que esta residencia la quiera.
ALTER TABLE alarm_profile_overrides
    ADD COLUMN IF NOT EXISTS observe_only BOOLEAN;
