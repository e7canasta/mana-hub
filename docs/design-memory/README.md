# Design Memory — Destilado 2026-08-29

## Client DSL
- [DSL Design](dsl-design.md) — **Reescrito** `manahub { scope { action } }` 11 scopes (`AGENTS.md`). Reemplaza patrón fantasma `Resource → Action`.

## API Design
- [API Design](api-design.md) — **Destilado** `api.md` es fuente real (101 endpoints). Este archivo es aspiracional (wrapper/pagination no existe).

## Domain Language
- [Domain Language](domain-language.md) — **Destilado** cheat-sheet de `vocabulario-unificado.md` (5 términos canónicos).

## Summaries & Cubo
- [Summaries, ficha del residente e hidratador del cubo](summaries-resident-chart-cube.md) — memoria técnica: tablas `*_summaries`, proyecciones `/views/resident-chart`, relación con `scene_events`, pipeline cubo.

## Insights (compute)
- [Módulo insights — rollups, KPIs, hallazgos e informes](insights-module.md) — subproyecto separado del SOR: batch nocturno + hallazgos (tendencia/cluster/política) + JSON de report.

## Architecture Decision Records
- [ADR-001: Domain Events](decision-records/ADR-001-domain-events.md)
- [ADR-002: Aggregate Roots](decision-records/ADR-002-aggregate-roots.md)
- [ADR-003: Bounded Contexts](decision-records/ADR-003-bounded-contexts.md)
- [ADR-004: Client DSL](decision-records/ADR-004-client-dsl.md)
