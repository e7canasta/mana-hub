# Handoff — Fundación y Gobierno de Arquitectura

**Fecha:** 2026-09-01
**Branch:** `dev`
**Último commit:** `079f339`

---

## Estado actual

### Commits de refactorización

```
079f339  refactor: decompose updateResidentProfile + SleepRollup.compute
1244832  refactor: reduce cyclomatic complexity 16→12 methods CC>12
b1a9a5d  refactor: fix cross-context violations + CleanController decomposition
e368677  fix: replace silent runCatching with logged enum parsing
1ad5bfe  refactor(fase4): generic upsert + PanelProjectionService documented
f619c6e  refactor(fase3): port relocation + ProjectionService decomposition + polymorphism
c7d8058  refactor(fase2): bounded context decoupling + performance
7d29ba1  refactor(fase1): 4 bug fixes + 5 code quality improvements
f736639  fix: 14/14 integration tests green
```

### Tests

```
bootstrap:  14/14 pass  (ApiSmokeTest: 10, EpisodeLifecycleTest: 4)
insights:   29/29 pass  (6 spec files)
clients:    0/70 pass   (pre-existing failures, no son nuestros)
```

### Arquitectura — dependencias entre módulos (jdeps bytecodes)

```
shared-kernel   → (leaf)
identity        → shared-kernel
audit           → shared-kernel
residence       → shared-kernel
population      → shared-kernel
coverage        → shared-kernel
care            → shared-kernel
policy          → shared-kernel
surveillance    → shared-kernel
evidence        → shared-kernel
streams         → shared-kernel
observation     → shared-kernel          ✅ (antes dependía de population)
history         → shared-kernel          ✅ (antes dependía de observation)
integration     → shared-kernel, policy
bootstrap       → todos (orchestrator)
```

**Zero cross-context violations.** Todas las dependencias fluyen hacia shared-kernel.

---

## detekt — estado actual

```
ANTES (FASE 1):  842 issues, 17 methods CC>12
AHORA:           838 issues, 10 methods CC>12
```

### Métodos CC>12 restantes (skip intencional)

| Método | CC | Razón |
|--------|-----|-------|
| `CleanController.toMap()` | 21 | buildMap DSL, no lógica |
| `IntegrationService.TwinSnapshot.parse()` | 14 | ya extraído como data class |
| `IntegrationService.persistSignalAudit()` | 13 | mapeo de campos |
| `AlarmProfileApplicationService.typedOverridesToMap()` | 14 | polymorphism FASE 3 |
| `AlarmProfileApplicationService.overridesMapToTyped()` | 12 | borderline |
| `AlarmCatalogService.buildCatalogTemplates()` | 13 | catálogo builder |
| `SceneTimeline.staffVisits()` | 12 | borderline |
| `SleepRollup.compute()` | 1 | ✅ fixed (extracted SleepAccumulator) |
| 3 × clients/dummies | 12-14 | test/simulation code |

### Reglas deshabilitadas intencionalmente

- `MagicNumber` (234) — cosmético, no afecta diseño
- `WildcardImport` (142) — formato, auto-fix con detekt
- `LongParameterList` (139) — artefacto de JPA entities

---

## JaCoCo — cobertura

```
MÓDULO         LINE%   BRANCH%
bootstrap      15.2%       1%     (wiring, no lógica)
insights       39.7%      42%     (business logic)
```

### Insights desglose

| Paquete | LINE% | Qué es | Acción |
|---------|-------|--------|--------|
| `derive` | 87% | Baseline calculation | ✅ |
| `rollup` | 62% | Agregación nocturna | Subir a 80% |
| `find` | 53% | Detección de findings | **P0 — testear** |
| `recommend` | 36% | Recomendaciones clínicas | **P0 — testear** |
| `inbound` | 15% | Recepción de datos | P1 — testear |
| `api` | 0% | Endpoints REST | P2 — controller tests |
| `config` | 0% | Configuración | Skip |

---

## ArchUnit — pendiente

El test `ArquitecturaTest.kt` fue creado pero tiene issues con el classpath multi-módulo. Pendiente de arreglar.

Reglas definidas (11):
1. shared-kernel domain free of Spring annotations
2. shared-kernel domain free of JPA annotations
3. shared-kernel domain free of Jackson annotations
4. observation → population (blocked)
5. history → observation (blocked)
6. history → population (blocked)
7. history → residence (blocked)
8. policy → audit (blocked)
9. Controllers → JPA entities (blocked)
10. Domain → infrastructure (blocked)
11. Domain events CRUD naming (blocked)

---

## Patrones aplicados

| Patrón | Dónde | Fowler/Vernon |
|--------|-------|---------------|
| Port Pattern | BedAssignmentPort, ObservationQueryPort | Vernon ACL |
| Snapshot Port | ObservationQueryPort snapshots | Evans BC |
| Extract Method | updateResidentProfile, CleanController | Fowler |
| Extract Class | SleepAccumulator, TwinSnapshot | Fowler |
| Strategy Map | describeSignal, humanState | Fowler |
| inline reified parseEnum | EventIngestionService, SceneEventAdapter | Kotlin idiom |
| Generic upsert | EventIngestionService | Fowler |
| TimeRange VO | CleanController | Vernon VO |
| publishAndClear | DomainEventPublisher extension | Kotlin idiom |

---

## Próximos pasos

1. **Testear insights/find + insights/recommend** (JaCoCo P0)
2. **Subir insights/rollup a 80%** (JaCoCo P1)
3. **Testear insights/inbound** (JaCoCo P1)
4. **Arreglar ArchUnit** (classpath multi-módulo)
5. **FacilityApplicationService** (25 funcs, TooManyFunctions)
6. **FacilityController** (18 funcs, TooManyFunctions)
