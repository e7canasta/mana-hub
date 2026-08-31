# Mana-Hub — Design Blueprint

> **System of Record** del dominio de monitoreo de residencias de adultos mayores.
> No somos el motor que piensa, detecta o decide. Somos la memoria persistente
> donde todos consultan y registran.

**Última actualización:** 2026-08-31
**Autores de la mesa:** Martin Fowler (refactoring), Vaughn Vernon (DDD), Spring experts 2026

---

## 1. Arquitectura en una mirada

```
┌─────────────────────────────────────────────────────────────────┐
│  EXTERNO (no nosotros)              MANA-HUB (nosotros)        │
│  ─────────────────────              ──────────────────────      │
│  El que PIENSA                      El que RECUERDA             │
│  El que DETECTA                     El que PERSISTE             │
│  El que DECIDE                      El que CONSULTA             │
└─────────────────────────────────────────────────────────────────┘
```

**Stack:** Kotlin 2.4.20-RC + Spring Boot 4.0.1 + PostgreSQL 17 + Flyway
**Módulos Gradle:** 16 (`settings.gradle.kts:22-37`)
**Entidades JPA:** 49 — una por cada tabla Flyway. Zero tablas huérfanas.

---

## 2. Bounded Contexts (12 + shared-kernel)

| # | Contexto | Módulo | Responsabilidad | Aggregate Roots |
|---|----------|--------|-----------------|-----------------|
| 1 | **Identity** | `identity/` | Usuarios y sesiones | `User` |
| 2 | **Audit** | `audit/` | Trail inmutable | `AuditLogEntry` (append-only) |
| 3 | **Residence** | `residence/` | Jerarquía edilicia + layout | `Facility`, `Wing`, `Room`, `Bed` |
| 4 | **Population** | `population/` | Residentes y asignaciones | `Resident`, `BedAssignment` |
| 5 | **Coverage** | `coverage/` | Grupos, turnos y cobertura | `StaffGroup`, `FacilityShift`, `UnitShiftCoverage`, `StaffMember` |
| 6 | **Care** | `care/` | Rondas, tareas y notas | `Round`, `RoundTask`, `CareSummary` |
| 7 | **History** | `history/` | Episodios normalizados | `HistoryEpisode`, `HistoryEpisodeReview`, `HistoryEpisodeIntervention` |
| 8 | **Policy** | `policy/` | Perfiles de alarma + overrides | `AlarmProfileVersion`, `PolicyOutboxEntry` |
| 9 | **Surveillance** | `surveillance/` | Episodios y transiciones | `Episode` |
| 10 | **Observation** | `observation/` | Percepciones y resúmenes | `SensorEvent`, `CurrentBedState`, `SceneEvent` |
| 11 | **Evidence** | `evidence/` | Evidencia clínica | `Evidence`, `Timeline`, `ClipWindow` |
| 12 | **Streams** | `streams/` | Cámaras y regiones | `Stream`, `StreamRegion` |

**Otros módulos (no bounded contexts):**

| Módulo | Rol |
|--------|-----|
| `shared-kernel/` | IDs compartidos, Entity base, DomainEvent, DomainEventPublisher |
| `bootstrap/` | Composition root + Flyway + config |
| `panel-api/` | BFF para el panel (lecturas CQRS + delegación de writes) |
| `clients/` | DSL tipado para clientes externos |
| `blueprints/` | Escenarios ejecutables |
| `integration/` | ACL para mana-hive |
| `event-bridge/` | Subsistema NATS (proceso separado) |
| `insights/` | Batch nocturno (proceso separado) |

---

## 3. Mapa de dependencias (DAG sin ciclos)

```
shared-kernel          → (nada)
identity               → shared-kernel
audit                  → shared-kernel, identity
residence              → shared-kernel
population             → shared-kernel, residence
coverage               → shared-kernel, residence
streams                → shared-kernel, residence
care                   → shared-kernel, residence, population
surveillance           → shared-kernel, population, residence
policy                 → shared-kernel, population, audit
evidence               → shared-kernel, residence, population
observation            → shared-kernel, residence, population, evidence, surveillance, policy
history                → shared-kernel, observation, population, residence
bootstrap              → TODOS los domain modules
panel-api              → shared-kernel + 7 domain modules
```

**Regla inquebrantable:** Un bounded context NUNCA importa otro bounded context directamente. La comunicación es por IDs compartidos (shared-kernel) o domain events.

---

## 4. Aggregate Design — Convenciones

### 4.1. Estructura de un Aggregate

```kotlin
class MyAggregate private constructor(
    override val id: MyAggregateId,
    val businessField: String,
    override var version: Long
) : AggregateRoot<MyAggregateId>() {

    // Domain events internos
    private val _domainEvents = mutableListOf<MyDomainEvent>()
    val domainEvents: List<MyDomainEvent> get() = _domainEvents.toList()
    fun clearEvents() = _domainEvents.clear()

    // Comportamiento de negocio
    fun doSomething(): MyAggregate {
        require(state == EXPECTED) { "Mensaje de error en lenguaje de negocio" }
        val next = reconstitute(...)  // immutable copy
        next._domainEvents.add(MyDomainEvent(...))
        return next
    }

    companion object {
        fun create(...): MyAggregate { /* factory + domain event */ }
        fun reconstitute(...): MyAggregate { /* sin events — para hidratar desde DB */ }
    }
}
```

### 4.2. Reglas de Aggregate Design (Vernon)

| Regla | Implementación |
|-------|---------------|
| **Un aggregate = una unidad de consistencia transaccional** | Un `@Transactional` modifica un solo aggregate root |
| **Referencias cross-aggregate por ID, nunca por referencia** | `residentId: ResidentId` no `resident: Resident` |
| **Aggregate boundaries = transaction boundaries** | No `saveAll()` de aggregates distintos en el mismo transaction |
| **Private constructor + companion factory** | `create()` para nuevos, `reconstitute()` para hidratar |
| **Domain events desde el aggregate, no desde el servicio** | `_domainEvents.add(...)` dentro del método de negocio |
| **Behavior, not anemic data classes** | Si no tiene `require()`, no es un aggregate — es un DTO |

### 4.3. Entities vs Value Objects vs Data Classes

| Tipo | Cuándo usar | Ejemplo |
|------|-------------|---------|
| **AggregateRoot** | Tiene identity, state transitions, invariants | `Episode`, `Round`, `AlarmProfileVersion` |
| **Entity (no root)** | Parte de un aggregate, tiene identity propia | `RoundTask` (dentro de Round) |
| **Value Object** | Sin identity, igualdad por valor | `EpisodeId`, `ResidentId`, `EpisodeSeverity` |
| **Data class** | DTO interno, transport, sin comportamiento de negocio | `SceneEvent`, `CurrentBedState`, `SleepSummary` |
| **@Immutable** | Append-only, escrito una vez, leído muchas veces | `SensorEvent`, `AuditLog`, `Evidence` |

---

## 5. JPA Conventions — Lo que SÍ y lo que NO

### 5.1. Entidad JPA

```kotlin
@Entity
@Immutable  // Solo si es append-only
@Table(name = "tabla_real")
class TablaEntity(
    @Id var id: String = "",
    @Column(name = "campo_snake") var campo: String = "",
    @Version var version: Long = 0  // Solo si tiene state transitions
)
```

| Regla | Detalle |
|-------|---------|
| **Una entity = una tabla** | Cada tabla Flyway tiene exactamente una `@Entity` |
| **`@Column(name = "...")` explícito** | Nunca depender del naming strategy de Hibernate |
| **`@Version` solo donde hay mutable state** | Append-only → `@Immutable`, sin `@Version` |
| **`@Immutable` en append-only** | `sensor_events`, `scene_events`, `audit_log`, `evidence`, `care_notes`, `history_episode_*`, `sentinel_signals`, `notification_events` |
| **Entity en `infrastructure/persistence/`** | Nunca en el package `domain/` |
| **0 relaciones JPA** | FKs como `String`, nunca `@ManyToOne`. querying por repository, no por navegación |

### 5.2. Repository Pattern (Ports & Adapters)

```
domain/repository/FooRepository.kt     → interface (puerto)
infrastructure/persistence/FooJpa.kt   → @Repository JPA + @Repository Adapter
```

```kotlin
// Puerto (domain layer)
interface EpisodeRepository {
    fun findById(id: EpisodeId): Episode?
    fun save(episode: Episode): Episode
}

// Adapter (infrastructure layer)
@Repository
class EpisodeRepositoryAdapter(private val jpa: EpisodeEntityRepository) : EpisodeRepository {
    override fun findById(id: EpisodeId): Episode? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun save(episode: Episode): Episode = jpa.save(episode.toEntity()).toDomain()

    private fun EpisodeEntity.toDomain() = Episode.reconstitute(...)
    private fun Episode.toEntity() = EpisodeEntity(...)
}
```

| Regla | Detalle |
|-------|---------|
| **El adapter traduce, el repository JPA consulta** | `toDomain()` / `toEntity()` en el adapter |
| **El domain layer NUNCA importa `jakarta.persistence`** | Zero JPA annotations en domain models |
| **Naming: `FooEntityRepository` (JPA) vs `FooRepository` (domain)** | Nombres claros para evitar confusión |
| **Queries complejas: `@Query` JPQL o Specification** | Nunca `nativeQuery = true`. Nunca `JdbcTemplate` para writes |

### 5.3. Anti-patrones JPA prohibidos

| Anti-patrón | Por qué | Alternativa |
|-------------|---------|-------------|
| `@ManyToOne` / `@OneToMany` | Lazy loading, N+1, cascading issues | FK como `String` + repository query |
| `JdbcTemplate` para writes | Rompe la fuente de verdad | Application service del bounded context |
| Entity como DTO | Leaky domain model | DTOs separados en `application/dto/` |
| `findAll()` sin límite | Memory explosion en producción | `findAll(Spec)` o paginación |
| `open-in-view: true` | OSIV anti-pattern | Ya está `false` en `application.yml:22` |

---

## 6. Domain Events — Patrón

### 6.1. Definición

```kotlin
// shared-kernel
interface DomainEvent {
    val eventId: String get() = UUID.randomUUID().toString()
    val occurredAt: Instant get() = Instant.now()
    val eventType: String
}

// En cada bounded context
sealed interface EpisodeEvent : DomainEvent {
    data class Created(...) : EpisodeEvent { override val eventType = "EpisodeCreated" }
    data class Acknowledged(...) : EpisodeEvent { override val eventType = "EpisodeAcknowledged" }
    data class Resolved(...) : EpisodeEvent { override val eventType = "EpisodeResolved" }
}
```

### 6.2. Publicación desde el Aggregate

```kotlin
class Episode ... {
    private val _domainEvents = mutableListOf<EpisodeEvent>()
    val domainEvents: List<EpisodeEvent> get() = _domainEvents.toList()

    fun acknowledge(actorId: String): Episode {
        require(isPending) { "Episode is not pending" }
        val next = reconstitute(...)
        next._domainEvents.add(EpisodeEvent.Acknowledged(episodeId = id, actorId = actorId))
        return next
    }
}
```

### 6.3. Consumo por el Application Service

```kotlin
@Transactional
fun acknowledgeEpisode(episodeId: String, actorId: String): EpisodeResponse {
    val episode = episodeRepository.findById(EpisodeId(episodeId))
    val updated = episode.acknowledge(actorId)
    val saved = episodeRepository.save(updated)
    updated.domainEvents.forEach { eventPublisher.publish(it) }  // After commit
    updated.clearEvents()
    return saved.toResponse()
}
```

### 6.4. Consumo externo (Spring Events)

```kotlin
// Audit escucha todos los eventos
@Component
class AuditEventListener(private val auditService: AuditService) {
    @EventListener
    fun on(event: DomainEvent) { auditService.recordFromEvent(event) }
}

// Bridge escucha eventos específicos
@TransactionalEventListener(phase = AFTER_COMMIT)
fun on(event: AlarmProfileChangedEvent) { /* publicar a NATS */ }
```

---

## 7. Panel-API — CQRS Legítimo

### 7.1. Reads (CQRS puro)

`PanelProjectionService` usa `JdbcTemplate` para reads optimizados que cruzan bounded contexts. **Esto es legítimo en CQRS:** el panel es un BFF que arma proyecciones purpose-built para la UI.

```kotlin
// Reads: JDBC directo para JOINs complejos (CQRS)
fun residentRail(): List<ResidentRailDto> = jdbc.query("""
    SELECT r.id, r.full_name, ...
    FROM residents r
    LEFT JOIN ... 
""", rowMapper)
```

### 7.2. Writes (delegación a domain services)

`PanelCommandService` **delega** a los application services del dominio. **Nunca escribe directo.**

```kotlin
// Writes: delega al bounded context dueño
fun reviewEpisode(episodeId, verdict, note, actorId) {
    historyService.reviewHistoryEpisode(...)
    noteService.createEpisodeNote(...)
}
```

### 7.3. Regla

> **El panel lee como quiere (CQRS), pero escribe como el dominio manda.**

---

## 8. Fluent DSL — Contrato con Externos

```kotlin
manahub("http://localhost:8080") {
    identity { createUser / listUsers }
    residence { setupFacility / wings / rooms / beds }
    population { admitResident / assignTo / discharge }
    streams { setupStream / defineRegions }
    policy { catalog / configureAlarmProfile / alarmProfileHistory }
    surveillance { registerEpisode / acknowledge / resolve }
    observation { registerPerception / registerSceneChange / ingest*Summary / wingBoard }
    evidence { createEvidence / openTimeline / openClipWindow }
    care { startRound / addResidentNote / registerFinding / addShiftNote }
    history { residentHistoryEpisodes / reviewHistoryEpisode }
    audit { queryLog }
}
```

Cada scope valida contra endpoints reales (`api.md`). Vocabulario canónico en `vocabulario-unificado.md`.

---

## 9. Lo que NO hacemos (guardrails)

| # | Regla | Razón |
|---|-------|-------|
| 1 | **No analizamos video/imágenes** | Somos SOR, no motor de ML |
| 2 | **No ejecutamos reglas de negocio** | Lo hace `EpisodeEngine` externo |
| 3 | **No decidimos si un episodio se dispara** | Esa es decisión del motor |
| 4 | **No enviamos notificaciones** | Lo hace `NotificationService` externo |
| 5 | **No usamos `JdbcTemplate` para writes** | Fuente de verdad = application services |
| 6 | **No exponemos JPA entities en controllers** | Siempre DTOs de respuesta |
| 7 | **No usamos `@ManyToOne`/`@OneToMany`** | FKs como String, querying por repository |
| 8 | **No ponemos `@Version` en append-only** | Solo en entities con state transitions |
| 9 | **No importamos bounded contexts entre sí** | IDs compartidos o domain events |
| 10 | **No hacemos `findAll()` sin límite** | Memory bounds en un edge de 8 camas |

---

## 10. Flyway — Regla de migraciones

| Regla | Detalle |
|-------|---------|
| **Una tabla = una entity** | Si Flyway crea una tabla, JPA la mapea |
| **Migración nueva para cada cambio de schema** | `V22__descripción.sql` |
| **Nunca modificar migraciones pasadas** | Flyway las marcó como aplicadas |
| **`ddl-auto: none`** | Schema management 100% Flyway |
| **`@Version` en la migración** | Si la tabla necesita optimistic locking |

---

## 11. Context Map — Comunicación entre Contextos

```
                    ┌─────────────┐
                    │ mana-hive   │ (externo)
                    │ motor       │
                    └──────┬──────┘
                           │ NATS: perception.*, scene.*, sentinel.*
                           ▼
                    ┌─────────────┐
                    │ integration │ (ACL)
                    └──────┬──────┘
                           │ HTTP: /internal/v1/*
                           ▼
┌──────────┐  IDs   ┌──────────────┐  domain events  ┌──────────┐
│residence │◄──────►│  observation │───────────────►│surveillance│
│population│        └──────┬───────┘                 └─────┬────┘
└──────────┘               │                               │
                    ┌──────▼───────┐                ┌──────▼──────┐
                    │   evidence   │                │   history   │
                    └──────────────┘                └─────────────┘
                           │                               │
                    ┌──────▼───────────────────────────────▼──────┐
                    │              bootstrap (composition root)    │
                    └──────────────────────┬──────────────────────┘
                                           │
                    ┌──────────────────────▼──────────────────────┐
                    │         panel-api (BFF — CQRS reads)        │
                    │         reads: JdbcTemplate                  │
                    │         writes: delega a app services        │
                    └─────────────────────────────────────────────┘
```

---

## 12. Context Map — Cómo los bounded contexts se hablan

| From → To | Mecanismo | Ejemplo |
|-----------|-----------|---------|
| observation → surveillance | Domain event + application service call | `IntegrationService` → `EpisodeApplicationService` |
| observation → population | Repository query por ID | `BedAssignmentRepository.findByBedId()` |
| surveillance → history | Application service (cross-context) | `HistoryEpisodeApplicationService.ingestHistoryEpisode()` |
| policy → audit | Domain event listener | `AuditEventListener` escucha `AlarmProfileChangedEvent` |
| policy → NATS | Transactional outbox | `PolicyOutboxEntry.markPublished()` → `HubPolicyBridgeListener` |
| panel-api → todos | Delegación a app services | `PanelCommandService` → history, care, policy |
| panel-api reads → todos | JdbcTemplate (CQRS) | `PanelProjectionService` |

---

## 13. Archivos clave de referencia

| Archivo | Qué contiene |
|---------|-------------|
| `AGENTS.md` | Reglas del sistema (stack, modules, flows) |
| `docs/big-picture/domain-model.md` | Modelo de dominio completo |
| `docs/big-picture/data-flow.md` | Flujo de datos CÁMARA → SOR |
| `docs/big-picture/context-map.md` | Mapa de contextos |
| `docs/big-picture/architecture-overview.md` | Overview arquitectónico |
| `docs/api.md` | 101 endpoints (fuente real) |
| `docs/data-model.md` | Espejo del DDL |
| `docs/vocabulario-unificado.md` | 5 términos canónicos |
| `docs/design-memory/README.md` | Design memory index |

---

## 14. Decisiones pendientes

| # | Decisión | Estado | Notas |
|---|----------|--------|-------|
| D1 | Test de integración con Testcontainers | Pendiente | P1 del pre-sprint |
| D2 | Composite aggregate Facility (F6) | Deferred | Bajo valor para 8 camas |
| D3 | Domain events en Round, Resident, AlarmProfile | Futuro | Solo Episode tiene events hoy |
| D4 | Anti-Corruption Layer formal para integration | Futuro | Hoy es ad-hoc en `IntegrationService` |
| D5 | `@EntityGraph` para N+1 en ProjectionService | Futuro | Bajo riesgo con 8 camas |
