# Changelog — Panel API Schema Alignment

Fecha: 2026-08-31

## Problema
Las queries del panel-api asumían columnas que no existen en el schema real de PostgreSQL.

## Cambios de columnas

| Tabla | Columna esperada | Columna real | Queries afectadas |
|-------|-----------------|--------------|-------------------|
| `rooms` | `room_number` | `number` | residentRail, residentDetail, episodeFeed, preferenceFull, preferenceList |
| `history_episode_detections` | `episode_id` | `source_episode_id` | episodeFeed, episodeDetail |
| `history_episode_reviews` | `reviewed_at` | `resolved_at` | episodeFeed (mapper) |
| `alarm_profile_versions` | `overrides_json` | NO EXISTE | preferenceFull, preferenceList |
| `alarm_profile_versions` | `updated_at` | NO EXISTE (usar `created_at` o `valid_from`) | preferenceFull, preferenceList |

## Columnas con tipo TEXT en vez de TIMESTAMP

Las siguientes columnas son TEXT, no TIMESTAMP. Se leen como string:
- `episode_notes.created_at`
- `resident_notes.timestamp`
- `resident_notes.created_at`
- `resident_bed_assignments.starts_at` / `ends_at`
- `alarm_profile_versions.valid_from` / `valid_to`

## CHECK constraints activos

### `episode_notes.kind`
Solo permite: `ACKNOWLEDGEMENT`, `RESOLUTION`, `CLINICAL_NOTE`
El DTO `NoteKind` tiene más valores (CARE, CLINICAL, INSIGHT, etc.) que el CHECK no acepta.
→ Opción A: expandir el CHECK en una migración nueva.
→ Opción B: filtrar en el CommandService los kinds válibles.

### `resident_notes.kind`
Solo permite: `CARE`, `CLINICAL`, `INSIGHT`, `PATTERN`, `OBSERVATION`, `SUMMARY`
Faltan del DTO: `RECOMMENDATION`, `CLINICAL_NOTE`, `ACKNOWLEDGEMENT`, `RESOLUTION`

## Tabla inexistente

`history_episode_interventions` no existe.
→ El endpoint `GET /api/v1/panel/episodes/{id}/interventions` retorna lista vacía.
→ Opción: crear la tabla o eliminar el endpoint.

## Tablas existentes no usadas

- `episode_timeline_events` — podría alimentar el timeline del detail
- `episode_escalations` — podría enriquecer el nivel de escalación
