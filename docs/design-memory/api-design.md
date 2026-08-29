# API Design — Destilado 2026-08-29

> **Archivado parcialmente:** convenciones genéricas. Fuente real: `api.md` (101 endpoints `@RequestMapping`).

## Convenciones Reales

| Prefix | Audience | Ejemplo |
|--------|----------|---------|
| `/api/v1/*` | Panel humano (consultores) | `GET /api/v1/wings/{id}/board`, `POST /api/v1/episodes` |
| `/internal/v1/*` | M2M motores | `POST /internal/v1/events`, `POST /internal/v1/scene-events`, `POST /internal/v1/notifications`, `POST /internal/v1/clinical/*`, `POST /internal/v1/care-summaries` |

Ver tabla completa en `api.md:12`.

## Patrones Observados en Código

- **List vs Retrieve:** `GET /api/v1/facilities` + `GET /api/v1/facilities/{id}` + `GET /api/v1/facilities/{id}/tree` (jerárquico)
- **Internal ingestion:** `POST /internal/v1/events` (`IngestEventRequest`), `POST /internal/v1/scene-events` (`IngestSceneEventRequest`), `POST /internal/v1/clinical/sleep|mobility|bathroom-summaries`
- **State transitions:** `POST /api/v1/episodes/{id}/acknowledge`, `PATCH /api/v1/episodes/{id}`, `PATCH /api/v1/rounds/{id}`
- **Query por rango:** `GET /api/v1/residents/{id}/sleep?from=&to=` (default 14 días, `ObservationController.kt:29`)

## Gaps

- `api-design.md` viejo listaba `DELETE /api/v1/{resources}/{id}` pero código no tiene deletes salvo `DELETE /api/v1/beds/{id}/assignment` y retire lógico `retired_at`.
- Error/pagination JSON del doc viejo no existe: código usa `ResponseEntity` sin wrapper `data/total/page`.
- Ver `bootstrap/src/main/kotlin/com/hub/config/SecurityConfig.kt:16` `permitAll` — sin auth real aún.

Mantener como referencia aspiracional; implementar wrapper y RBAC es deuda.
