# API Design — Convenciones y gaps

> **Fuente real:** `api.md` (101 endpoints `@RequestMapping`).
> **Actualizado:** 2026-08-31 post pre-sprint (dual-write eliminado).

## Convenciones

| Prefix | Audience | Ejemplo |
|--------|----------|---------|
| `/api/v1/*` | Panel humano (consultores) | `GET /api/v1/wings/{id}/board`, `POST /api/v1/episodes` |
| `/internal/v1/*` | M2M motores | `POST /internal/v1/events`, `POST /internal/v1/scene-events` |

## Estado post-pre-sprint (2026-08-31)

### Writes — Un solo camino

Los writes del panel-api **delegan** a los application services del dominio.
Eliminado el dual-write (JdbcTemplate + JPA). Ver `PanelCommandService`.

| Operación | Antes (JDBC directo) | Ahora (delegación) |
|-----------|---------------------|--------------------|
| `POST /panel/episodes/{id}/review` | INSERT history_episode_reviews + UPDATE episodes + INSERT episode_transitions | `HistoryEpisodeApplicationService.reviewHistoryEpisode()` + `NoteApplicationService` |
| `POST /panel/episodes/{id}/notes` | INSERT episode_notes | `NoteApplicationService.createEpisodeNote()` |
| `POST /panel/residents/{id}/notes` | INSERT resident_notes | `NoteApplicationService.createResidentNote()` |
| `POST /panel/preferences/{id}/save` | UPDATE + INSERT alarm_profile_versions | `AlarmProfileApplicationService.updateResidentProfile()` |

### Reads — CQRS legítimo

Los reads del panel usan `JdbcTemplate` para proyecciones cross-context.
Esto es válido en CQRS: el BFF arma vistas purpose-built para la UI.

`PanelProjectionService` — queries optimizadas con `LEFT JOIN LATERAL`, etc.

## Patrones observados en código

- **List vs Retrieve:** `GET /facilities` + `GET /facilities/{id}` + `GET /facilities/{id}/tree`
- **Internal ingestion:** `POST /internal/v1/events`, `POST /internal/v1/scene-events`, `POST /internal/v1/clinical/*`
- **State transitions:** `POST /episodes/{id}/acknowledge`, `PATCH /episodes/{id}`, `PATCH /rounds/{id}`
- **Query por rango:** `GET /residents/{id}/sleep?from=&to=` (default 14 días)

## Gaps conocidos

| # | Gap | Estado |
|---|-----|--------|
| 1 | Sin `DELETE` explícito — solo `retired_at` (soft delete) | Diseño intencional |
| 2 | Sin wrapper `data/total/page` en paginación | Deuda — bajo riesgo con 8 camas |
| 3 | `SecurityConfig` tiene `permitAll` — sin auth real | Deuda — edge de geriátrico, 1-2 admins |
| 4 | Dos rutas GET duplicadas para summaries (`/views/` vs `/residents/`) | Deuda — consolidar futuras |
