# mana-hub Documentation

> System of Record for adult residential care monitoring.

## Quick Start

| Document | Description |
|----------|-------------|
| [Big Picture](big-picture/README.md) | System overview, architecture, data flow |
| [Roadmap](roadmap/README.md) | Sprint-by-sprint plan |
| [Domain Model](big-picture/domain-model.md) | Entities, aggregates, relationships |
| [Vocabulary](../vocabulario-unificado.md) | Unified domain language |

## Context Groups (BDD Specs)

Each context group is a superset of DDD contexts sharing a domain language.

| Context Group | Contexts | Flow |
|---------------|----------|------|
| [Resident Lifecycle](specs/resident-lifecycle/README.md) | Population + Policy + Surveillance | Admit → Assign → Configure → Monitor |
| [Clinical Monitoring](specs/clinical-monitoring/README.md) | Observation + Surveillance + Evidence | Perception → Scene → Episode → Evidence |
| [Care Operations](specs/care-operations/README.md) | Care + Surveillance + Audit | Round → Notes → Episode → Audit |
| [Facility Management](specs/facility-management/README.md) | Residence + Streams + Identity | Facility → Wing → Room → Bed → Camera |
| [Clinical History](specs/clinical-history/README.md) | History + Evidence + Care | Timeline → Evidence → Notes → Incidents |

## Technical References

| Document | Description |
|----------|-------------|
| [Technical Memory](technical-memory/README.md) | Stack, patterns, testing, security |
| [Design Memory](design-memory/README.md) | DSL design, API design, ADRs |
| [Business Glossary](../business-glossary-domain-model.md) | Domain model + glossary |
| [Data Model](../data-model-mermaid.md) | ER diagrams (Mermaid) |
| [User Stories](../user-stories-director-medico.md) | Product requirements |
