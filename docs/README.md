# mana-hub Documentation

> System of Record for adult residential care monitoring. **Stack real: Kotlin 2.4.20-RC + Spring Boot 4.0.1 + PostgreSQL 17** (`gradle/libs.versions.toml:2`, `bootstrap/src/main/resources/application.yml:8`).

## Quick Start

| Document | Description | Source of Truth |
|----------|-------------|-----------------|
| [Big Picture](big-picture/README.md) | System overview, architecture, data flow | `docs/big-picture/*` |
| [API Reference](../api.md) | 101 endpoints (Spring MVC) | `@RequestMapping` in `*/api/rest/*.kt` |
| [Data Model](../data-model.md) | 44 tables, PostgreSQL DDL, ER | `bootstrap/src/main/resources/db/migration/V1..V10` |
| [Domain Model](big-picture/domain-model.md) | Entities, aggregates, enums | `*/domain/model/*.kt` |
| [Vocabulary](../docs/vocabulario-unificado.md) | Percepción → Escena → Episodio → Hallazgo | `docs/vocabulario-unificado.md` |
| [Event Bus and Event Sourcing](event-bus-and-event-sourcing.md) | Ownership, envelope, subjects, Hub confirmed facts | `DomainEvent` + NATS adapters |

## Context Groups (BDD Specs)

Each context group is a superset of DDD contexts sharing a domain language.

| Context Group | Contexts | Flow |
|---------------|----------|------|
| [Resident Lifecycle](specs/resident-lifecycle/README.md) | Population + Policy + Surveillance | Admit → Assign → Configure → Monitor |
| [Clinical Monitoring](specs/clinical-monitoring/README.md) | Observation + Surveillance + Evidence | Percepción → Escena → Episodio → Evidencia |
| [Care Operations](specs/care-operations/README.md) | Care + Coverage + Surveillance + Audit | Round → Notes → Episode → Audit |
| [Facility Management](specs/facility-management/README.md) | Residence + Streams + Identity + Coverage | Facility → Wing → Room → Bed → Camera |
| [Clinical History](specs/clinical-history/README.md) | History + Evidence + Care + Observation | Timeline → Evidence → Notes → Summaries |

## Destilado 2026-08-29

> **44 docs auditados:** 8 vigentes, 22 destilados, 14 archivados. Ver [`DESTILADO.md`](DESTILADO.md) — preserva invariantes `R-*`, Gherkin, taxonomía hallazgos, matriz severidad y MoSCoW que no están en `api.md`/`data-model.md`. **Roadmap archivado** en `archive/roadmap/`, **Mermaid** en `archive/data-model-mermaid.md`.

## Technical References

| Document | Description | Status |
|----------|-------------|--------|
| [AGENTS.md](../AGENTS.md) | SOR overview, 44 tablas, DSL 11 scopes, flujo | ✅ Updated 2026-08-29 |
| [API Reference](../api.md) | 101 Spring MVC endpoints, DSL map | ✅ Generated 2026-08-29 |
| [Data Model](../data-model.md) | PostgreSQL 17, 44 tables, consolidated DDL | ✅ Rewritten 2026-08-29 |
| [Domain Model](big-picture/domain-model.md) | Entities + aggregates (real) | ✅ Updated 2026-08-29 |
| [Data Flow](big-picture/data-flow.md) | 8 phases, vocabulario canónico | ✅ Updated 2026-08-29 |
| [Vocabulary](vocabulario-unificado.md) | Unified language (5 términos) | ✅ Validated |
| [Destilado](DESTILADO.md) | Invariantes, Gherkin, hallazgos, severidad, MoSCoW | ✅ New 2026-08-29 |
| [Business Glossary](business-glossary-domain-model.md) | Glosario + capability map | ⚠️ Partial — banner destilado, ver `DESTILADO.md:1` |
| [Data Model Mermaid](archive/data-model-mermaid.md) | ER Mermaid deprecated | 📦 Archived — use `data-model.md` |
| [Architecture Overview](big-picture/architecture-overview.md) | Modules, layers, integration | ✅ Updated 2026-08-29 |
| [Context Map](big-picture/context-map.md) | 12 bounded contexts | ✅ Updated 2026-08-29 |
| [Event Bus and Event Sourcing](event-bus-and-event-sourcing.md) | NATS directions and confirmed Hub facts | ✅ Sprint 2026 |

## Clients & Blueprints — DSL Contract

- **Clients:** `clients/src/main/kotlin/com/hub/clients/core/ManaHubClient.kt:22` — 11 scopes (`identity|residence|population|streams|surveillance|policy|audit|care|history|evidence|observation`). Each validates against `api.md`.
- **Blueprints:** `blueprints/src/main/kotlin/com/hub/blueprints/scenarios/` — 7 scenarios (`ResidentOnboarding`, `PercepcionIngestion`, `CambioDeEscenaFlow`, `EpisodioLifecycle`, `RondaDeCuidados`, `FindingRegistration`, `NocturnoTurno`) + `clients/blueprint/scenarios` duplicates — keep `blueprints/` as canonical.

## How to Verify

```bash
# Build & check contracts
./gradlew :clients:compileKotlin :blueprints:compileKotlin :bootstrap:compileKotlin
# Scan real endpoints
grep -rn "@.*Mapping" --include="*.kt" | wc -l   # ~101
# Count real tables
grep -h "CREATE TABLE" bootstrap/src/main/resources/db/migration/*.sql | sort | uniq | wc -l  # 44
```
