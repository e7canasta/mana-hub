-- Vacía todos los datos de negocio. Conserva el schema Flyway.
-- Uso: psql $DATABASE_URL -f scripts/db/reset.sql

DO $$
DECLARE
  tables text;
BEGIN
  SELECT string_agg(format('%I.%I', schemaname, tablename), ', ')
  INTO tables
  FROM pg_tables
  WHERE schemaname = 'public'
    AND tablename <> 'flyway_schema_history';

  IF tables IS NOT NULL THEN
    EXECUTE 'TRUNCATE TABLE ' || tables || ' RESTART IDENTITY CASCADE';
  END IF;
END $$;
