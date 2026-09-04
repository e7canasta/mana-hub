# Sprint: Care Event Ownership — murmur ↔ mana-hub

**Fecha:** 2026-09-03
**Objetivo:** Hacer que mana-hub, como dueño del episodio y del contexto de care,
consuma directamente las interacciones de Murmur, las persista una sola vez y las
proyecte junto con la timeline del episodio.

---

## Contexto

Murmur (companion del enfermero) ya escucha episodios vía NATS (`sentinel.signal.v1.>`). Pero cuando la enfermera toca "Recibido", "Ya está" o escribe una nota, esos eventos solo se loguean localmente (`LogPublisher`). No llegan a mana-hub.

**Endpoints que ya existen en mana-hub** (el director ya los usa desde el panel):

| Acción | Endpoint | Módulo |
|--------|----------|--------|
| Acknowledge | `POST /api/v1/episodes/{id}/acknowledge` | surveillance |
| Resolve | `POST /api/v1/episodes/{id}/resolved` | surveillance |
| Episode note | `POST /api/v1/episodes/{id}/notes` | care |

**MVP boundary:**

- `POST /api/v1/episodes/{id}/acknowledge`
- `POST /api/v1/episodes/{id}/resolved`
- `POST /api/v1/episodes/{id}/notes`

No se agrega todavía un consumer Hub para `nurse.*`. Esos subjects quedan
disponibles para una futura auditoría/event distribution, pero no se usa una
segunda ruta para las mismas acciones.

**Diseño:** Murmur solicita por API, Hub decide y persiste. Para `resolved`, Hub
aplica el cierre manual existente y publica `hub.episode.*`. Para una nota,
Hub guarda `episode_notes` sin cambiar el estado del episodio.

---

## Arquitectura del flujo

```
┌─────────────┐       NATS        ┌───────────┐
│   murmur    │──────────────────→│ mana-hub  │
│  publisher  │  nurse.*.v1       │ care      │
└─────────────┘                   │ consumer  │
                                  └─────┬─────┘
                                        │
                         ┌──────────────┼──────────────┐
                         ▼              ▼              ▼
                    episode state   care facts    hub.episode.*
                    + notes         + timeline    confirmation
```

**Flujo detallado:**

1. Murmur consulta Hub antes de mostrar un episodio recibido.
2. Murmur llama los endpoints existentes para acknowledge y nota, y el nuevo
   endpoint semántico para resolución manual.
3. Hub persiste y publica el hecho confirmado del episodio.

---

## Trabajo por proyecto

### 1. mana-hub episode APIs y timeline

**Responsabilidad:** exponer la acción semántica de resolución sobre el cierre
manual existente y mantener notas/timeline bajo ownership de Hub.

**Archivos a modificar:**
- `integration/src/main/kotlin/com/hub/integration/application/service/NurseEventService.kt`
- consumer NATS del módulo/bootstrap de Hub
- `history/.../EpisodeTimelineDeriver.kt` y su puerto de consulta
- `history/.../timeline/EventType.kt`

**Timeline:** agregar `CARE_ACKNOWLEDGED`, `CARE_RESOLVED`,
`CARE_NOTE_ADDED` y `CARE_CAMERA_REVEALED` como hechos de interacción.
`hub.episode:EpisodeResolved` sigue siendo el evento canónico del episodio.
`episode_notes` conserva el contenido clínico; la timeline referencia la nota,
no crea otra fuente de verdad. Camera registra duración y nivel de detalle como
auditoría, pero el acceso visual sigue siendo responsabilidad de Murmur.

**Dependencias:** Los módulos `surveillance` y `care` ya están como dependencias de `integration` (ver `integration/build.gradle.kts`).

### 2. event-bridge (retirada de nurse.*)

**Archivos a modificar:**
- `event-bridge/src/main/kotlin/com/hub/bridge/ingest/EventRouter.kt` — quitar el forward de `nurse.`
- `event-bridge/src/main/kotlin/com/hub/bridge/ingest/NatsIngestService.kt` — quitar la suscripción de `nurse.>`

Bridge continúa para eventos cuyo productor externo es Hive. No contiene
ownership ni lógica del contexto care.

### 3. murmur (JetStreamPublisher)

**Archivos a crear:**
- `src-tauri/src/adapters/nurse/jetstream_publisher.rs`

**Archivos a modificar:**
- `src-tauri/src/adapters/nurse/mod.rs` — agregar módulo y re-exportar
- `src-tauri/src/bootstrap.rs` — swap `LogPublisher` → `JetStreamPublisher` (línea 357)
- `src-tauri/Cargo.toml` — agregar dependencia `async-nats`

**Nota:** murmur actualmente no tiene conexión NATS real. El `JetStreamPublisher` será el primer adapter NATS. La conexión se configura vía settings (URL, credentials).

### 4. Documentación

**Archivos a actualizar:**
- `murmur/docs/04-INTEGRATION.md` — sección §4.7 actualizar estado de publisher
- `mana-hub/event-bridge/docs/architecture.md` — nuevo, documentar rol del bridge

---

## Orden de ejecución

```
1. Congelar contrato `EventEnvelope` y corregir RFC3339 en Murmur.
2. Implementar consumer durable `nurse.>` en Hub siguiendo el patrón de Cox,
   pero con persistencia/idempotencia de SOR.
3. Persistir care interactions y proyectarlas en `EpisodeTimeline`.
4. Probar Hub directo con Murmur y verificar `hub.episode:EpisodeResolved`.
5. Retirar `nurse.>` del Bridge y redeployar los tres servicios.
6. Ejecutar Cox y documentar evidencia final.
```

**Razón:** El contrato y el dueño se fijan antes del transporte. Así evitamos
probar una ruta provisional y luego duplicar el mapeo.

---

## Criterios de aceptación

- [ ] murmur publica `nurse.ack.v1` a NATS (no solo tracing)
- [ ] murmur publica `nurse.resolved.v1` a NATS
- [ ] murmur publica `nurse.episode_note.v1` a NATS
- [ ] murmur publica `nurse.camera.v1` a NATS
- [ ] Hub consume directamente los cuatro `nurse.*` con durable + idempotencia
- [ ] Hub procesa y persiste en `episodes` + `episode_notes` + care timeline
- [ ] La acción queda en la timeline del episodio (visible desde panel)
- [ ] Los endpoints existentes del panel siguen funcionando igual
- [ ] event-bridge no consume ni enruta `nurse.*`

---

## Riesgos

| Riesgo | Mitigación |
|--------|------------|
| murmur no tiene `async-nats` aún | Agregar como dependencia; si hay conflictos con Tauri, usar `tokio-tungstenite` como alternativa |
| event-bridge no tiene `nats.enabled` para nurse | Ya tiene NATS; solo agregar suscripción |
| Race condition: murmur publica antes que mana-hub esté listo | JetStream con durable consumer; reintento automático |
| El engine (mana-hive) no escucha `nurse.*` aún | murmur informa, mana-hub persiste. El engine se conecta después |

---

## Handoff: Murmur Episode Reconciliation

The episode read endpoint already exists at
`GET /api/v1/episodes/{episodeId}` in
`surveillance/api/rest/EpisodeController.kt`. It returns the authoritative
episode state used to reject stale JetStream deliveries before Murmur shows a
card. A missing episode now returns HTTP `404` rather than `200` with an empty
body.

Murmur must call this endpoint from Rust when it first sees an episode, restores
one from SQLite, or reconnects. `PENDING`/`OPEN` may reach the glass;
`RESOLVED`/`CLOSED` and `404` must not. `hub.episode:EpisodeResolved` remains
the final event that retires the card.

The episode note is a separate operation. Murmur publishes
`nurse.episode_note.v1`; event-bridge forwards it to the existing Hub nurse
integration, which persists the note without changing episode status.

Next implementation owner: Murmur Rust adapter and reconciliation tests. Do not
create a second Hub episode endpoint and do not delete the NATS stream to clear
old station state.
