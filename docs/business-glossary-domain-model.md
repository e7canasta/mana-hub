# mana-hub: Business Glossary + Domain Model

> ⚠️ **Nota 2026-08-29:** Este documento contiene entidades fantasma (`AlarmPreset`, `Threshold`, `resident_attributes` 1.1) que no existen en DDL real. Ver `data-model.md` (44 tablas PostgreSQL) y `docs/big-picture/domain-model.md` (entidades reales) como fuentes de verdad. Pendiente de resincronización completa — ver `docs/README.md`.

## North Star del Proyecto

> "El lenguaje unívoco es la columna vertebral del Domain-Driven Design.
> Si tu código no habla el mismo lenguaje que tu dominio, ya fallaste."
> — Eric Evans

---

## 1. BUSINESS GLOSSARY (Glosario de Negocio)

### 1.1 Definiciones

| Término | Definición | Quién lo usa | Ejemplo |
|---------|------------|--------------|---------|
| **Residencia** | Instalación donde viven residentes de edad avanzada | Director médico, Administrador | "Residencia Sol Naciente" |
| **Ala** | Sección física de una residencia | Director médico, Enfermero | "Ala Norte" |
| **Habitación** | Espacio individual donde vive un residente | Enfermero | "Habitación 101" |
| **Cama** | Unidad de descanso dentro de una habitación | Enfermero | "Cama 1" |
| **Residente** | Persona que vive en la residencia | Todos | "María Fernández" |
| **Asignación** | Relación temporal entre residente y cama | Director médico | "María asignada a Cama 1" |
| **Cámara** | Dispositivo de video que monitorea una habitación | Técnico | "Cámara Room 101" |
| **Región** | Zona específica dentro del campo de una cámara | Técnico | "Zona cama", "Zona baño" |
| **Perfil de Monitoreo** | Configuración de cómo se vigila a un residente | Director médico, Enfermero | "Perfil: riesgo alto" |
| **Protocolo** | Plantilla de reglas de monitoreo | Director médico | "Protocolo: caídas" |
| **Umbral** | Límite que dispara un episodio | Director médico, Enfermero | "5 min fuera de cama" |
| **Episodio** | Acontecimiento clínico que requiere atención | Enfermero, Director médico | "Episodio de desorientación nocturna" |
| **Severidad** | Nivel de gravedad de un episodio | Enfermero | "CRITICAL", "WARNING" |
| **Registro** | Evento crudo capturado por sensores | Técnico | "Registro de movimiento en pasillo" |
| **Resumen Clínico** | Consolidación diaria de datos | Director médico | "Resumen clínico del 15/03" |
| **Historia Clínica** | Línea de tiempo completa del residente | Director médico, Enfermero | "Ver historia de María" |
| **Evidencia Clínica** | Material que documenta el episodio | Director médico | "Clip del episodio #123" |
| **Ronda Médica** | Visita programada al residente | Enfermero | "Ronda de las 08:00" |
| **Tarea de Ronda** | Actividad específica dentro de la ronda | Enfermero | "Control de presión arterial" |
| **Nota Clínica** | Registro escrito del profesional | Enfermero | "Nota: PA 140/90, estable" |
| **Profesional** | Persona que trabaja en la residencia | Director médico | "Enf. Roberto García" |

### 1.2 Relaciones de Dominio

```
RESIDENCIA ──contiene──▶ ALA ──contiene──▶ HABITACIÓN ──contiene──▶ CAMA
                                                              │
                                                              ▼
                                                    ASIGNACIÓN ◀── RESIDENTE
                                                              │
                                                              ▼
                                                    PERFIL DE MONITOREO
                                                              │
                                                              ▼
                                                    PROTOCOLO (template)
                                                              │
                                                              ▼
                                                    EPISODIO ◀── UMBRAL
                                                              │
                                                              ▼
                                                    EVIDENCIA CLÍNICA
                                                              │
                                                              ▼
                                                    HISTORIA CLÍNICA
```

---

## 2. DOMAIN MODEL (Modelo de Dominio)

### 2.1 Entidades por Bounded Context

#### Context: RESIDENCE (Residencia)

| Entidad | Atributos | Acciones |
|---------|-----------|----------|
| **Facility** | id, name, timezone | setup, update |
| **Wing** | id, facilityId, name, floor | add, update |
| **Room** | id, wingId, number, type | add, update |
| **Bed** | id, roomId, label, status | add, update, assign |
| **Stream** | id, roomId, streamKey, name, status | assign, defineRegions |
| **StreamRegion** | id, streamId, label, points, regionType | define |

#### Context: POPULATION (Población)

| Entidad | Atributos | Acciones |
|---------|-----------|----------|
| **Resident** | id, fullName, birthDate, admissionDate, status | admit, discharge, update |
| **ResidentBedAssignment** | id, residentId, bedId, assignedAt, releasedAt, isCurrent | assign, release, change |

#### Context: POLICY (Políticas)

| Entidad | Atributos | Acciones |
|---------|-----------|----------|
| **AlarmPreset** | id, name, description, thresholds, isDefault | create, update |
| **AlarmProfile** | id, residentId, validFrom, validTo, mobilityAid, autopilot, mode, templateId, overridesJson, riskLevel, updatedBy | configure, update, history |
| **Threshold** | key, value, unit, description | define, update |

#### Context: MONITOR (Monitoreo)

| Entidad | Atributos | Acciones |
|---------|-----------|----------|
| **Episodio** | id, residentId, bedId, severity, status, title, detail, occurredAt, escalationLevel, evidenceKind, evidenceRef | registrar, reconocer, resolver, escalar |
| **SeveridadEpisodio** | INFO, WARNING, CRITICAL, EMERGENCY | - |
| **EstadoEpisodio** | PENDING, ACKNOWLEDGED, RESOLVED, DISMISSED | - |

#### Context: OBSERVATION (Observación)

| Entidad | Atributos | Acciones |
|---------|-----------|----------|
| **SensorEvent** | id, roomId, streamId, monitorKey, kind, state, sleeping, residentId | ingest |
| **CurrentBedState** | id, bedId, residentId, state, lastTransitionAt | update |
| **SceneEvent** | id, roomId, streamId, monitorKey, fromState, toState, residentId | ingest |
| **SleepSummary** | id, residentId, summaryDate, calmMinutes, restlessMinutes, awakeMinutes, outOfBedMinutes, bedExitCount, wakeCount | compute |
| **MobilitySummary** | id, residentId, summaryDate, walkingMinutes, distanceMeters, transferCount | compute |
| **BathroomSummary** | id, residentId, summaryDate, visitCount, nightVisitCount | compute |

#### Context: EVIDENCE (Evidencia)

| Entidad | Atributos | Acciones |
|---------|-----------|----------|
| **Evidence** | id, bedId, residentId, evidenceKind, source, storagePath | create |
| **Timeline** | id, bedId, residentId, status, startedAt, endedAt | open, close |
| **ClipWindow** | id, bedId, residentId, windowStart, windowEnd, status | open, close |

#### Context: CARE (Cuidado)

| Entidad | Atributos | Acciones |
|---------|-----------|----------|
| **Round** | id, residentId, assignedTo, scheduledAt, status | schedule, complete, skip |
| **RoundTask** | id, roundId, taskType, description, status, note, completedAt | complete, skip |
| **CareNote** | id, residentId, roundId, authorId, noteType, content | create |

#### Context: HISTORY (Historia)

| Entidad | Atributos | Acciones |
|---------|-----------|----------|
| **ClinicalTimeline** | Vista unificada de todos los eventos | query |
| **TimelineEvent** | timestamp, type, description, source, data | - |

#### Context: IDENTITY (Identidad)

| Entidad | Atributos | Acciones |
|---------|-----------|----------|
| **User** | id, username, displayName, role | register, update |

#### Context: AUDIT (Auditoría)

| Entidad | Atributos | Acciones |
|---------|-----------|----------|
| **AuditEntry** | id, actorId, action, entityType, entityId, payload, createdAt | log |

---

## 3. CONTEXT MAP (Mapa de Contextos)

### 3.1 Capas Arquitectónicas

```
╔══════════════════════════════════════════════════════════════════════════╗
║                    MANA-HUB: CAPAS ARQUITECTÓNICAS                      ║
╠══════════════════════════════════════════════════════════════════════════╣
║                                                                          ║
║  CAPA 0: INFRAESTRUCTURA (Soporte)                                       ║
║  ┌─────────────┐  ┌─────────────┐                                        ║
║  │  Identity    │  │   Audit     │                                        ║
║  │  (Auth/RBAC) │  │  (Log)      │                                        ║
║  └─────────────┘  └─────────────┘                                        ║
║                                                                          ║
║  CAPA 1: SISTEMA DE RECORD (Master Data)                                 ║
║  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                      ║
║  │  Residence   │  │ Population  │  │    Care     │                      ║
║  │  (CRM-like)  │  │ (Users)     │  │  (Rounds)   │                      ║
║  └─────────────┘  └─────────────┘  └─────────────┘                      ║
║                                                                          ║
║  CAPA 2: CORE (Valor)                                                    ║
║  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    ║
║  │   Policy    │  │   Monitor   │  │ Observation │  │  Evidence   │    ║
║  │ (Profiles)  │  │ (Episodes)  │  │  (Events)   │  │  (Clips)    │    ║
║  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    ║
║                       │                   │               │              ║
║                       └───────────────────┼───────────────┘              ║
║                                           ▼                              ║
║                                  ┌─────────────┐                        ║
║                                  │   History   │                        ║
║                                  │ (Timeline)  │                        ║
║                                  └─────────────┘                        ║
║                                                                          ║
╚══════════════════════════════════════════════════════════════════════════╝
```

### 3.2 Relaciones entre Contextos

| Contexto A | Relación | Contexto B | Tipo |
|------------|----------|------------|------|
| Identity | provee usuarios a | Residence | Partner |
| Identity | provee usuarios a | Population | Partner |
| Identity | provee usuarios a | Care | Partner |
| Identity | provee usuarios a | Policy | Partner |
| Residence | contiene | Population | Conformista |
| Residence | monitorea | Observation | Conformista |
| Population | configura | Policy | Customer/Supplier |
| Population | genera | Monitor | Customer/Supplier |
| Population | recibe | Care | Customer/Supplier |
| Policy | feed a | Monitor | Customer/Supplier |
| Monitor | consume | Observation | Customer/Supplier |
| Monitor | genera | Evidence | Customer/Supplier |
| Monitor | feed a | History | Customer/Supplier |
| Observation | feed a | History | Customer/Supplier |
| Evidence | feed a | History | Customer/Supplier |
| Care | feed a | History | Customer/Supplier |

---

## 4. CAPABILITY MAP (Mapa de Capacidades)

### 4.1 Capacidades por Contexto

```
MANA-HUB CAPABILITIES
═══════════════════════════════════════════════════════════════════

RESIDENCE
├── Gestionar Instalación
├── Gestionar Alas
├── Gestionar Habitaciones
├── Gestionar Camas
└── Asignar Cámaras

POPULATION
├── Admitir Residentes
├── Asignar a Cama
├── Cambiar de Cama
└── Dar de Baja

POLICY
├── Gestionar Protocolos
├── Configurar Perfil de Monitoreo
├── Definir Umbrales
└── Historial de Perfiles

MONITOR
├── Registrar Episodio
├── Reconocer Episodio
├── Resolver Episodio
├── Escalar Episodio
└── Consultar Episodios

OBSERVATION
├── Ingestar Eventos
├── Computar Resumen de Sueño
├── Computar Resumen de Movilidad
└── Computar Resumen de Baño

EVIDENCE
├── Crear Evidencia
├── Abrir Timeline
├── Cerrar Timeline
├── Abrir Clip Window
└── Cerrar Clip Window

CARE
├── Programar Ronda
├── Completar Ronda
├── Saltar Ronda
├── Crear Nota Clínica
└── Consultar Notas

HISTORY
├── Ver Historia Clínica
├── Filtrar por Tipo de Evento
└── Filtrar por Rango de Fechas

IDENTITY
├── Registrar Usuario
├── Asignar Rol
└── Autenticar

AUDIT
├── Registrar Acción
└── Consultar Log
```

### 4.2 Capacidades Críticas (Core)

| Capacidad | Contexto | Impacto | Complejidad |
|-----------|----------|---------|-------------|
| Configurar Perfil de Monitoreo | Policy | Alto | Media |
| Registrar Episodio | Monitor | Alto | Baja |
| Ver Historia Clínica | History | Alto | Media |
| Computar Resúmenes | Observation | Alto | Alta |
| Crear Evidencia | Evidence | Medio | Baja |

---

## 5. GLOSARIO TÉCNICO (Mapeo Código ↔ Dominio)

### 5.1 Entidades

| Código (Kotlin) | Dominio (Español) | Tabla (PostgreSQL) |
|-----------------|-------------------|---------------------|
| `Facility` | Residencia | `facilities` |
| `Wing` | Ala | `wings` |
| `Room` | Habitación | `rooms` |
| `Bed` | Cama | `beds` |
| `Resident` | Residente | `residents` |
| `ResidentBedAssignment` | Asignación | `resident_bed_assignments` |
| `Stream` | Cámara | `streams` |
| `StreamRegion` | Región | `stream_regions` |
| `AlarmPreset` | Protocolo | `alarm_presets` |
| `AlarmProfile` | Perfil de Monitoreo | `alarm_profiles` |
| `Alert` | Episodio | `episodes` ✅ |
| `SensorEvent` | Registro | `sensor_events` |
| `CurrentBedState` | Estado Actual | `current_bed_states` |
| `SceneEvent` | Transición de Escena | `scene_events` |
| `SleepSummary` | Resumen de Sueño | `sleep_summaries` |
| `MobilitySummary` | Resumen de Movilidad | `mobility_summaries` |
| `BathroomSummary` | Resumen de Baño | `bathroom_summaries` |
| `Evidence` | Evidencia Clínica | `evidence` |
| `Timeline` | Línea de Tiempo | `timelines` |
| `ClipWindow` | Ventana de Clip | `clip_windows` |
| `Round` | Ronda Médica | `rounds` |
| `RoundTask` | Tarea de Ronda | `round_tasks` |
| `CareNote` | Nota Clínica | `care_notes` |
| `User` | Profesional | `users` |
| `AuditEntry` | Registro de Auditoría | `audit_log` |

### 5.2 Enums

| Código (Kotlin) | Dominio | Valores |
|-----------------|---------|---------|
| `AlertLevel` | Severidad del Episodio | INFO, WARNING, CRITICAL, EMERGENCY |
| `AlertStatus` | Estado del Episodio | PENDING, ACKNOWLEDGED, RESOLVED, DISMISSED |
| `RiskLevel` | Nivel de Riesgo | LOW, MEDIUM, HIGH |
| `BedStatus` | Estado de la Cama | AVAILABLE, OCCUPIED, MAINTENANCE |
| `ResidentStatus` | Estado del Residente | ACTIVE, INACTIVE, DECEASED |
| `UserRole` | Rol del Profesional | OWNER, STAFF, ADMIN |

### 5.3 Acciones

| Código (Kotlin) | Dominio | Endpoint |
|-----------------|---------|----------|
| `registerUser` | Registrar Profesional | `POST /api/v1/users` |
| `setupFacility` | Configurar Residencia | `POST /api/v1/facilities` |
| `admitResident` | Admitir Residente | `POST /api/v1/residents` |
| `assignToBed` | Asignar a Cama | `POST /api/v1/residents/{id}/bed` |
| `configureAlarmProfile` | Configurar Perfil | `PATCH /api/v1/alarm-presets/{residentId}` |
| `triggerAlert` | Registrar Episodio | `POST /api/v1/episodes` ✅ |
| `acknowledge` | Reconocer Episodio | `POST /api/v1/episodes/{id}/acknowledge` ✅ |
| `resolve` | Resolver Episodio | `PATCH /api/v1/episodes/{id}` ✅ |
| `ingestEvent` | Ingestar Registro | `POST /internal/v1/events` |
| `ingestSleepSummary` | Computar Resumen Sueño | `POST /internal/v1/summaries/sleep` |
| `createEvidence` | Crear Evidencia | `POST /api/v1/evidence` |
| `openTimeline` | Abrir Línea de Tiempo | `POST /api/v1/timelines` |
| `openClipWindow` | Abrir Ventana Clip | `POST /api/v1/clip-windows` |
| `scheduleRound` | Programar Ronda | `POST /api/v1/rounds` |
| `completeRound` | Completar Ronda | `PATCH /api/v1/rounds/{id}` |
| `createCareNote` | Crear Nota Clínica | `POST /api/v1/care-notes` |

---

## 6. REGLAS DE NEGOCIO

### 6.1 Episodios

| Regla | Descripción |
|-------|-------------|
| R-EP-001 | Un episodio debe tener al menos un residente asociado |
| R-EP-002 | La severidad debe ser INFO, WARNING, CRITICAL o EMERGENCY |
| R-EP-003 | Un episodio PENDING puede ser reconocido o resuelto |
| R-EP-004 | Un episodio ACKNOWLEDGED debe ser resuelto |
| R-EP-005 | Un episodio RESOLVED no puede ser modificado |

### 6.2 Perfiles de Monitoreo

| Regla | Descripción |
|-------|-------------|
| R-PF-001 | Un residente puede tener un solo perfil activo |
| R-PF-002 | El perfil tiene validez temporal (validFrom, validTo) |
| R-PF-003 | Los overrides deben ser JSON válido |
| R-PF-004 | El riesgo debe ser LOW, MEDIUM o HIGH |

### 6.3 Asignaciones

| Regla | Descripción |
|-------|-------------|
| R-AS-001 | Un residente solo puede estar en una cama a la vez |
| R-AS-002 | La cama debe estar AVAILABLE para asignar |
| R-AS-003 | Al liberar, la cama vuelve a AVAILABLE |

### 6.4 Rondas

| Regla | Descripción |
|-------|-------------|
| R-RN-001 | Una ronda debe tener al menos una tarea |
| R-RN-002 | Solo se puede completar tareas PENDING |
| R-RN-003 | Al completar todas las tareas, la ronda se marca COMPLETED |

---

## 7. FLUJOS DE NEGOCIO

### 7.1 Flujo: Registrar Episodio

```
┌─────────────────────────────────────────────────────────────┐
│  1. Cámara detecta evento                                    │
│     └─▶ ObservationEngine.ingestEvent()                      │
│                                                              │
│  2. Motor de reglas evalúa                                   │
│     └─▶ Consulta Policy.alarmProfile(residentId)             │
│     └─▶ Consulta Policy.presetById(templateId)               │
│     └─▶ Compara evento vs umbrales                           │
│                                                              │
│  3. Se crea el episodio                                      │
│     └─▶ Monitor.registrarEpisodio(residentId, bedId, ...)    │
│                                                              │
│  4. Se notifica al personal                                  │
│     └─▶ NotificationService.notifyStaff(episodeId, ...)      │
│                                                              │
│  5. Se recopila evidencia                                    │
│     └─▶ EvidenceCollector.collectVideoClip(...)               │
│                                                              │
│  6. El profesional reconoce y resuelve                       │
│     └─▶ Monitor.reconocerEpisodio(alertId, actorId)          │
│     └─▶ Monitor.resolverEpisodio(alertId, status)            │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 Flujo: Configurar Perfil de Monitoreo

```
┌─────────────────────────────────────────────────────────────┐
│  1. Director médico consulta catálogo                        │
│     └─▶ Policy.catalog()                                     │
│     └─▶ Muestra protocolos disponibles con umbrales          │
│                                                              │
│  2. Director selecciona protocolo                            │
│     └─▶ Policy.presetById("fall_risk")                       │
│                                                              │
│  3. Director configura perfil                                │
│     └─▶ Policy.configureAlarmProfile(residentId) {           │
│           templateId = "fall_risk"                           │
│           riskLevel = HIGH                                   │
│           mobilityAid = "walker"                             │
│         }                                                    │
│                                                              │
│  4. Perfil se activa inmediatamente                          │
│     └─▶ Motor de reglas usa nuevo perfil                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 8. NUESTRO ROL: SISTEMA DE RECORD

> "No somos el motor que piensa, detecta o decide.
> Somos la memoria persistente donde todos consultan y registran."

```
EXTERNO (no nosotros)              MANA-HUB (nosotros)
─────────────────────              ──────────────────────
El que PIENSA                      El que RECUERDA
El que DETECTA                     El que PERSISTE
El que DECIDE                      El que CONSULTA
```

### Qué SÍ hacemos

| Responsabilidad | Descripción |
|-----------------|-------------|
| Persistir datos | Guardamos residentes, episodios, evidencia |
| Mantener estado | Sabemos quién está dónde, en qué cama |
| Registrar eventos | Guardamos todo lo que pasa |
| Consultar información | Respondemos preguntas |
| Mantener historial | Guardamos el pasado |

### Qué NO hacemos

| Responsabilidad | Quién lo hace |
|-----------------|---------------|
| Analizar video | ObservationEngine |
| Evaluar reglas | EpisodeEngine |
| Decidir si alertar | EpisodeEngine |
| Enviar notificaciones | NotificationService |
| Grabar video | EvidenceCollector |
| Detectar movimiento | ObservationEngine |
| Calcular resúmenes | ObservationEngine |

### Para el Usuario Final

> "Para el usuario, nosotros somos la CARA del sistema.
> Ellos nos hablan a nosotros, y nosotros delegamos."

```
USUARIO (Director/Enfermero)
         │
         ▼
    ┌─────────┐
    │mana-hub │  ← Ellos nos ven a nosotros
    └─────────┘
         │
         ├────▶ ObservationEngine
         ├────▶ AlertEngine
         ├────▶ NotificationService
         └────▶ EvidenceCollector
```

---

## 9. ARTEFACTOS DE PROYECTO

| Artefacto | Propósito | Estado |
|-----------|-----------|--------|
| **Business Glossary** (este documento) | Definir lenguaje unívoco | ✅ |
| **Domain Model** | Definir entidades y relaciones | ✅ |
| **Context Map** | Mostrar relaciones entre contextos | ✅ |
| **Capability Map** | Mostrar capacidades del sistema | ✅ |
| **Data Model (Mermaid)** | Modelo de datos visual | ✅ |
| **User Stories** | Requerimientos desde usuario | ✅ |
| **API Contract** | Contrato de la API | ✅ |
| **Architecture Decision Records** | Decisiones de diseño | 📝 Pendiente |

---

## 10. PRÓXIMOS PASOS

1. **Validar** este glossario con el director médico
2. **Refinar** las user stories con los hallazgos
3. **Actualizar** el data model si hay cambios
4. **Implementar** los cambios en código
5. **Documentar** decisiones arquitectónicas

---

*Documento creado: 2026-08-24*
*Versión: 1.0*
*Autor: mana-hub team*
