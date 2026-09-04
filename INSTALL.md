# Instalacion local

## Requisitos

- Docker
- Java 25
- Git

## PostgreSQL

Desde la raiz de este repositorio:

```bash
docker compose up -d postgres
```

La base queda disponible en `localhost:5432` con estos datos:

```text
database: mana_hub
user: postgres
password: postgres
```

## Levantar mana-hub

```bash
./gradlew :bootstrap:bootRun
```

La API queda en `http://localhost:8080`. Swagger esta disponible en
`http://localhost:8080/swagger-ui.html`.

Flyway crea y actualiza el esquema al iniciar la aplicacion.

## Cargar el dump demo

Iniciar `mana-hub` al menos una vez para que Flyway cree las tablas. Luego
cargar el escenario sanitizado de Jose:

```bash
tar -xOzf scripts/db/dumps/jose-demo.sql.tar.gz jose-demo.sql \
  | docker exec -i mana-hub-pg psql -U postgres -d mana_hub
```

Para comenzar desde una base completamente nueva:

```bash
docker compose down -v
docker compose up -d postgres
./gradlew :bootstrap:bootRun
```

Detener la aplicacion despues del primer arranque y ejecutar el restore
anterior.

## Verificacion

```bash
curl http://localhost:8080/actuator/health
```

El frontend `mana-ui` usa esta API por defecto desde `http://localhost:5173`.
