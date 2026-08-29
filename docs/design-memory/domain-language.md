# Domain Language — Destilado 2026-08-29

> Redirige a fuente canónica `docs/vocabulario-unificado.md:53` (5 términos). Este archivo es cheat-sheet destilado.

## Términos Canónicos — `vocabulario-unificado.md:53`

| Término | Definición | Tabla / Código | Naturaleza |
|---------|------------|----------------|------------|
| **PERCEPCIÓN** | Lectura cruda sensor (postura, ubicación, staff_present) | `sensor_events` (`kind POSTURE|LOCATION|STAFF_PRESENCE|ACCESSORY_PRESENCE`) | Cruda, volátil |
| **CAMBIO DE ESCENA** | Transición confirmada tras hysteresis | `scene_events` (`TRANSITION|PERMANENCE`, `triggerType hysteresis|permanence|manual`) | Validada |
| **NOTIFICACIÓN** | Solo visual, no abre episodio | `notification_events` | Informativa |
| **EPISODIO** | Requiere atención, agrupa scene changes | `episodes` (`INFO|WARNING|CRITICAL|EMERGENCY`) | Accionable |
| **HALLAZGO** | Conclusión experta/ML (INSIGHT/PATTERN) | `resident_notes` (`kind INSIGHT|PATTERN`) | Interpretada |
| **EVIDENCIA** | Clip/timeline material | `evidence|timelines|clip_windows` | Soporte |

Ver matriz severidad × confirmación en `vocabulario-unificado.md:143`: `INFO` virtual, `WARNING` virtual+video, `CRITICAL/EMERGENCY` en sitio+grabación. `ESTADO SEGURO` auto-resolución (deuda).

## Grupos de Contexto — `docs/big-picture/context-map.md:5`

| Grupo | Contextos | Flujo DSL |
|-------|----------|-----------|
| Resident Lifecycle | Population + Policy + Surveillance | `admit → assign → configureAlarmProfile → monitor` |
| Clinical Monitoring | Observation + Surveillance + Evidence | `registerPerception → registerSceneChange → registerEpisode → createEvidence` |
| Care Operations | Care + Coverage + Surveillance | `startRound → addResidentNote/registerFinding → addShiftNote` |
| Facility Management | Residence + Streams + Identity + Coverage | `setupFacility{wing{room{bed}}} → assignStreamToRoom → registerUser` |
| Clinical History | History + Evidence + Care + Observation | `residentHistoryEpisodes → historyEpisodeSequence → sleep/mobility/bathroomSummaries → careSummaries` |

## Reglas de Uso

- ¿Lo dijo el **SENSOR**? → **Percepción** (`POST /internal/v1/events`)
- ¿Lo confirmó **MOTOR DE ESCENA**? → **Cambio de Escena** (`POST /internal/v1/scene-events`)
- ¿Solo informa? → **Notificación** (`POST /internal/v1/notifications`)
- ¿Requiere atención? → **Episodio** (`POST /api/v1/episodes`)
- ¿Lo concluyó **PERSONA/IA** con análisis? → **Hallazgo** (`POST /api/v1/residents/{id}/notes kind=INSIGHT`)

No decir: ~~"observación"~~ ambiguo, ~~"alerta"~~ genérico, ~~"evento"~~ sin calificar.
