# Design Memory — mana-hub

> **Fuente de verdad arquitectónica.** Última actualización: 2026-08-31.
>
> Si no está acá, no existe como decisión tomada. Si está acá pero está
> desactualizado, arrancá una discusión antes de codear.

---

## Blueprint Central

**→ [DESIGN-BLUEPRINT.md](DESIGN-BLUEPRINT.md)** — El documento. Arquitectura, bounded contexts, JPA conventions, domain events, panel CQRS, DSL, guardrails. Todo en un solo lugar.

---

## Documentos vivos (vigentes, verificados contra código)

| Documento | Qué cubre | Actualizado |
|-----------|-----------|-------------|
| [DESIGN-BLUEPRINT.md](DESIGN-BLUEPRINT.md) | Blueprint arquitectónico completo (14 secciones) | 2026-08-31 |
| [domain-language.md](domain-language.md) | Términos canónicos (5) + reglas de uso | 2026-08-29 ✓ |
| [dsl-design.md](dsl-design.md) | DSL 11 scopes tipados — firma real, no fantasma | 2026-08-29 ✓ |
| [api-design.md](api-design.md) | Convenciones API, gaps, dual-write eliminado | 2026-08-31 actualizado |
| [summaries-resident-chart-cube.md](summaries-resident-chart-cube.md) | Summaries, ficha residente, cubo OLAP | 2026-08-31 ✓ |
| [insights-module.md](insights-module.md) | Módulo insights: rollups, KPIs, hallazgos, informes | 2026-08-31 ✓ |

---

## Archivo (decisiones históricas — obsoletas)

Los ADRs 001-004 fueron escritos contra código que ya no refleja la realidad.
Su contenido fue subsumido por `DESIGN-BLUEPRINT.md`. Conservados como
referencia histórica.

| Archivo | Por qué se archivó |
|---------|-------------------|
| [archive/ADR-001-domain-events-archived.md](archive/ADR-001-domain-events-archived.md) | Lista 3 events vs los 12+ reales. Blueprint los cubre §6. |
| [archive/ADR-002-aggregate-roots-archived.md](archive/ADR-002-aggregate-roots-archived.md) | Lista 3 aggregates vs 21 reales. Blueprint los cubre §4. |
| [archive/ADR-003-bounded-contexts-archived.md](archive/ADR-003-bounded-contexts-archived.md) | Lista 10 BCs vs 12 reales. Blueprint los cubre §2. |
| [archive/ADR-004-client-dsl-archived.md](archive/ADR-004-client-dsl-archived.md) | Firma fantasma `ManaHubClient(session)`. Blueprint los cubre §8. |

---

## Regla de uso

1. **Antes de crear un ADR nuevo**, leer `DESIGN-BLUEPRINT.md` — probablemente ya esté cubierto.
2. **Si el blueprint no lo cubre**, crear `ADR-005-*.md` con status `Proposed` y actualizar este índice.
3. **Si un documento se desactualiza**, marcarlo con ⚠️ y actualizarlo antes de merge.
4. **El blueprint es el único documento que puede declarar "regla"**. Los demás son referencia.
