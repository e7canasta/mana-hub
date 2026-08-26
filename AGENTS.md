# mana-hub — System of Record (SOR)

## Arquitectura

Mana-hub es el **System of Record** del dominio de monitoreo de residencias de adultos mayores.

**No somos** el motor que piensa, detecta o decide.
**Somos** la memoria persistente donde todos consultan y registran.

### Roles del Sistema

```
┌─────────────────────────────────────────────────────────────────┐
│  EXTERNO (no nosotros)              MANA-HUB (nosotros)        │
│  ─────────────────────              ──────────────────────      │
│  El que PIENSA                      El que RECUERDA             │
│  El que DETECTA                     El que PERSISTE             │
│  El que DECIDE                      El que CONSULTA             │
└─────────────────────────────────────────────────────────────────┘
```

### Componentes Externos (roles, no implementaciones)

| Rol | Responsabilidad | Ejemplo |
|-----|----------------|---------|
| `ObservationEngine` | Detecta cambios de estado en escenas/cámaras | Servicio de visión por computadora |
| `EpisodeEngine` | Evalúa reglas y decide si dispara episodios | Motor de reglas de negocio |
| `NotificationService` | Envía notificaciones (SMS, push, email) | Servicio de mensajería |
| `EvidenceCollector` | Recopila evidencia (video, clips, fotos) | Servicio de grabación |

### Lo que SÍ hacemos

- Persistir `sensor_events` (percepciones crudas)
- Mantener `current_bed_states` (estado actual)
- Registrar `scene_events` (cambios de escena)
- Gestionar `episodes` (episodios que requieren atención)
- Almacenar `evidence` (evidencia clínica)
- Registrar `incident_detections` y `incident_reviews` (incidentes)
- Mantener `audit_log` (traza de auditoría)

### Lo que NO hacemos

- Analizar video o imágenes
- Ejecutar reglas de negocio
- Decidir si un episodio se dispara
- Enviar notificaciones
- Procesar clips de video

### El DSL como Contrato

El DSL de mana-hub es la **interfaz de contrato** que los componentes externos usan para interactuar con nuestro SOR.

```
External Engine (Observation)  → usa nuestro DSL → mana-hub SOR
External Engine (Episode)      → usa nuestro DSL → mana-hub SOR
External Engine (Notification) → usa nuestro DSL → mana-hub SOR
External Engine (Evidence)     → usa nuestro DSL → mana-hub SOR
```

### Flujo de Datos

```
CÁMARA → ObservationEngine → mana-hub (persiste) → Consultores
                                    ↑
                      EpisodeEngine → mana-hub (episodios)
                                    ↑
                   NotificationService → mana-hub (notificaciones)
                                    ↑
                  EvidenceCollector → mana-hub (evidencia)
```
