# DSL Design — Destilado 2026-08-29

> **Reescrito sobre línea base real.** Reemplaza patrón fantasma `Client → Resource → Action` (no existe `facilities.list()` ni `session` param). Fuente: `clients/src/main/kotlin/com/hub/clients/core/ManaHubClient.kt:22` y `api.md`.

## Patrón Real: `manahub { scope { action } }` — 11 Scopes Tipados

```kotlin
manahub("http://localhost:8080") {
  // Residence — declarativo, no anidado
  val facility = residence.setupFacility("Residencia Esperanza") {
    timezone = "America/Mexico_City"
    wing("Norte") {
      floor = "1"
      room("101") { bed("A"); bed("B") }
    }
  }
  val bed = facility.firstBed() // via facility.tree()

  // Population
  val resident = population.admitResident(
    fullName = "María García López",
    birthDate = LocalDate.of(1935, 3, 15),
    admissionDate = LocalDate.now()
  )
  resident.assignTo(bed.id) // → POST /api/v1/residents/{id}/assignments
  resident.discharge(actorId = "director_1")

  // Policy — DAG catalog + overrides
  val catalog = policy.catalog() // GET /api/v1/alarm-presets/catalog
  val profile = policy.configureAlarmProfile(resident.id) {
    riskLevel = RiskLevel.HIGH
    mobilityAid = "walker"
    templateId = "FALL_RISK" // DagCatalog: STANDARD|NIGHT_WANDERING|FALL_RISK|CRITICAL
  }

  // Observation — Percepción → Escena
  observation.registerPerception(
    monitorKey = "cam-101",
    kind = PerceptionKind.LOCATION, // POSTURE|LOCATION|STAFF_PRESENCE|ACCESSORY_PRESENCE
    bedId = bed.id, residentId = resident.id, state = "out_of_bed"
  )
  observation.registerSceneChange(
    bedId = bed.id, residentId = resident.id,
    kind = SceneChangeKind.TRANSITION, fromState = "in_bed", toState = "out_of_bed",
    triggerType = TriggerType.HYSTERESIS
  )
  val changes = observation.sceneChanges(resident.id) // GET /api/v1/residents/{id}/scene-events
  val board = observation.wingBoard(wing.id)

  // Surveillance — Episodios
  val episode = surveillance.registerEpisode(resident.id) {
    severity = EpisodeSeverity.CRITICAL
    title = "Caída detectada"
    bedId = bed.id
  }
  episode.acknowledge(actorId = "nurse_1") // POST /api/v1/episodes/{id}/acknowledge
  episode.resolve(EpisodeStatus.RESOLVED)  // PATCH /api/v1/episodes/{id}
  episode.addNote("nurse_1", EpisodeNoteKind.CLINICAL_NOTE, "Sin lesiones")

  // Care — Notas y hallazgos
  care.addResidentNote(resident.id, "nurse_1", ResidentNoteType.CARE, "PA 140/90")
  care.registerFinding(resident.id, "dr_1", ResidentNoteType.INSIGHT, "Patrón insomnio 2-4AM")
  care.addEpisodeNote(episode.id, "nurse_1", EpisodeNoteKind.ACKNOWLEDGEMENT, "Acudido")
  care.addShiftNote(facility.id, wing.id, "night", "2026-08-29", "nurse_1", ShiftNoteKind.SHIFT_SUMMARY, "Sin incidencias")
  care.ingestCareSummary("rec-1", resident.id, LocalDate.now(), totalMinutes = 45)

  // Evidence
  evidence.createEvidence(bed.id, resident.id, "clip")
  evidence.openTimeline(bed.id, resident.id)
  evidence.openClipWindow(bed.id, resident.id)

  // Streams
  val stream = streams.assignStreamToRoom(room.id) { streamKey = "cam-101" }
  stream.defineRegions { bed("0,0 1,0 1,1 0,1"); bathroom("...") }

  // History
  val episodes = history.residentHistoryEpisodes(resident.id) // GET /api/v1/residents/{id}/history-episodes
  history.reviewHistoryEpisode(episodes.first().id, status = "REVIEWED", actorId = "dr_1")

  // Audit & Identity
  audit.byEntity("resident", resident.id)
  identity.registerUser { username = "nurse_1"; displayName = "Nurse"; role = Role.STAFF; password = "secret" }
}
```

## Qué cambió vs. doc viejo

| Antes (fantasma) | Ahora (real) |
|------------------|--------------|
| `ManaHubClient(url)` + `hub.residence(session).facilities.list()` | `manahub(url) { residence.setupFacility {} }` |
| `facility.wings.list()` / `wing.rooms.list()` | `facility.tree().wings / rooms / beds` |
| `population.residents.create(...)` | `population.admitResident(...)` |
| `resident.assignments.list()` | `population` no tiene list; vía `GET /api/v1/residents/{id}/assignments` |
| `episode.notes.create` | `care.addEpisodeNote(episodeId, ...)` (care, no surveillance) |
| `EpisodeSeverity` + `ResidentStatus {ACTIVE,INACTIVE}` | `EpisodeSeverity {INFO,WARNING,CRITICAL,EMERGENCY}` + `ResidentStatus {ACTIVE,DISCHARGED}` (`population/domain/model/Resident.kt:10`) |

## Principios Preservados (destilado)

- **Type safety:** enums `PerceptionKind`, `EpisodeSeverity`, `RiskLevel`, `ResidentNoteType` (`clients/*/ *Models.kt`)
- **Fluent:** `episode.acknowledge().resolve()` sí existe (`surveillance/SurveillanceContext.kt:100`)
- **DSL como contrato:** cada método valida contra `api.md` endpoint real
- **11 scopes:** `identity|residence|population|streams|surveillance|policy|audit|care|history|evidence|observation` (`ManaHubClient.kt:26`)
