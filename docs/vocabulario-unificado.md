# mana-hub: Vocabulario Unificado (Organización + Motor de Percepción)

> Documento complementario al Business Glossary.
> Resuelve los conflictos de nombres entre el lenguaje del **Motor de Percepción**
> (computadora de abordo) y el lenguaje de la **organización** (director médico,
> enfermeros, administración).

---

## 1. EL CONFLICTO

### "Observación" significa DOS cosas distintas

```
MUNDO MOTOR                              MUNDO CLÍNICO
──────────────                           ──────────────
"Observación" =                          "Observación" =
Lo que el SENSOR reporta                 Lo que un EXPERTO concluye

  "El sensor dice que                    "Observando 2 semanas de datos,
   hay una persona                        concluimos que María tiene
   acostada en la cama"                   un patrón de insomnio"

  (crudo, automático,                    (interpretado, humano o IA,
   por máquina)                           con juicio clínico)
```

**Si ambos se llaman "observación", vamos a tener bugs semánticos para siempre.**

---

## 2. VOCABULARIO UNIFICADO PROPUESTO

### La Cadena Canónica de Eventos

```
┌─────────────┐    ┌───────────────┐    ┌──────────────┐    ┌───────────┐
│ PERCEPCIÓN  │───▶│ CAMBIO DE     │───▶│  EPISODIO    │───▶│ HISTORIA  │
│ (crudo)     │    │ ESCENA        │    │  (si hay     │    │ CLÍNICA   │
│             │    │ (confirmado)  │    │   regla)     │    │           │
└─────────────┘    └───────────────┘    └──────────────┘    └───────────┘
     │                   │                    │                  ▲
     │                   │                    │                  │
     │                   ├──▶ NOTIFICACIÓN    │    ┌───────────┐ │
     │                   │    (solo visual,   │    │ HALLAZGO  │─┘
     │                   │    sin episodio)   │    │ (insight  │
     │                   │                    │    │  clínico) │
     ▼                   ▼                    ▼    └───────────┘
  Todo queda          Motor de            Centinela +
  registrado          Escena              Harbor deciden
```

### Tabla Canónica

| # | Término Canónico | Definición | Origen | Naturaleza |
|---|------------------|------------|--------|------------|
| 1 | **PERCEPCIÓN** | Lo que el sensor reporta en un instante: postura, ubicación, presencia de staff/accesorios. Dato crudo, sin validación. | Motor de Percepción (cámara) | Cruda, automática, volátil |
| 2 | **CAMBIO DE ESCENA** | Transición de estado CONFIRMADA tras aplicar hysteresis, sensibilidad y umbral de permanencia. También llamado *fact*. | Motor de Escena | Validada, confiable |
| 3 | **NOTIFICACIÓN** | Cambio de escena que se muestra visualmente (tarjeta, icono) pero NO abre episodio. Informativa. | Harbor (según política) | Informativa |
| 4 | **EPISODIO** | Suceso relevante que requiere atención, abierto según reglas del perfil de monitoreo. Agrupa todos los cambios de escena relacionados hasta su cierre. | Centinela + Harbor | Accionable, narrativo |
| 5 | **HALLAZGO** | Conclusión aprendida sobre un residente: patrón detectado, recomendación, insight clínico. Generado por experto humano, equipo clínico o IA. | Experto / Clínico / ML | Interpretada, acumulable |

---

## 3. DETALLE POR TÉRMINO

### 3.1 PERCEPCIÓN (antes "observación del motor")

> **Analogía:** como el sensor de temperatura del auto que le dice a la computadora
> de abordo "el motor está a 95°". Es una lectura, no una decisión.

**Qué incluye:**
- Postura del residente (acostado, sentado, de pie)
- Ubicación en la habitación (cama, pasillo, baño)
- Presencia de personal (staff presente: sí/no)
- Presencia de accesorios (silla de ruedas, andador, barandas levantadas)

**Limitación conocida del sensor:** con 2+ personas en la habitación deja de analizar
postura/ubicación y solo reporta "staff presente" hasta volver a quedar una persona.

**Regla de oro:** la percepción NUNCA dispara acciones directamente.
Siempre pasa por el Motor de Escena primero.

| Antes se llamaba | Ahora se llama |
|------------------|----------------|
| Observación (del motor) | **Percepción** |
| Evento crudo | **Percepción** |
| Sensor event | **Percepción** |

---

### 3.2 CAMBIO DE ESCENA (el "fact" del motor)

> Es la percepción que sobrevivió al filtro: hysteresis + confianza + sensibilidad.

**Qué agrega sobre la percepción:**
- Confirma estabilidad temporal (no fue ruido)
- Aplica umbrales de permanencia configurados en el perfil de monitoreo
- Representa transición (`from_state → to_state`) o permanencia prolongada

**Importante:** los eventos de permanencia SOLO existen si la política los define.
Sin umbral configurado, no hay evento de permanencia.

| Antes se llamaba | Ahora se llama |
|------------------|----------------|
| Scene event / fact | **Cambio de Escena** |
| Fact de escena | **Cambio de Escena** |
| Evento de estado | **Cambio de Escena** |

---

### 3.3 NOTIFICACIÓN (informativa, sin episodio)

> "Entró al baño" cambia el icono de la tarjeta. Nadie tiene que ir a ningún lado.
> Pero queda registrado que ocurrió y que se mostró.

**Características:**
- Visual, no bloqueante
- No abre episodio
- Puede requerir confirmación virtual ligera (opcional, según política)
- Siempre queda registrada

| Antes se llamaba | Ahora se llama |
|------------------|----------------|
| Alerta info | **Notificación** |
| Evento notificable | **Notificación** |

---

### 3.4 EPISODIO (requiere atención)

> Tiene narrativa: apertura → acontecimientos → resolución.

**Características:**
- Lo abre el Centinela/Harbor según las reglas del perfil de monitoreo
- **Agrupa** múltiples cambios de escena relacionados (no cierra y abre uno nuevo
  por cada cambio; asigna la severidad más alta alcanzada)
- Requiere confirmación según severidad (ver matriz abajo)
- Puede cerrarse automáticamente si el residente vuelve a ESTADO SEGURO

**Matriz Severidad × Confirmación × Grabación**

| Severidad | Confirmación | Grabación | Nota |
|-----------|--------------|-----------|------|
| INFO* | Virtual (panel) | No | Opcional según política, para no generar ruido |
| WARNING | Virtual (panel) + ver video en vivo | Opcional (configurable) | Algunos warnings piden sitio, no obligatorio |
| CRITICAL | **En sitio** (ir a la habitación) | Sí, siempre | Obligatorio dejar en estado seguro |
| EMERGENCY | **En sitio** (ir a la habitación, urgente) | Sí, siempre | Obligatorio |

\* INFO como episodio es opcional por política; normalmente INFO es solo notificación.

**Los dos significados de "resolver" (según rol):**

| Rol | "Resolver" significa | Qué registra el sistema |
|-----|---------------------|------------------------|
| **Enfermero** | Ir a la habitación y devolver al residente a ESTADO SEGURO | Confirmación en sitio + nota clínica + cierre |
| **Director médico** | Revisar, monitorear, documentar, pedir aclaraciones | Nota clínica + cierre con observaciones |
| **Sistema (automático)** | El residente volvió solo a ESTADO SEGURO | Cierre automático con marca "auto-resuelto" |

**ESTADO SEGURO:** estado del residente/habitación donde no hay riesgo inminente
(ej.: acostado en cama). Si el residente vuelve a un estado seguro aunque nadie
intervino, el episodio se cierra automáticamente.

| Antes se llamaba | Ahora se llama |
|------------------|----------------|
| Alerta warning/critical | **Episodio** |
| Alert | **Episodio** |

---

### 3.5 HALLAZGO (insight, conclusión)

> "Observamos que cada noche entre 2 y 4 AM María intenta levantarse."
> Eso NO es una percepción ni un episodio. Es conocimiento ganado.

**Origen posible:**
- Experto humano revisando datos
- Equipo clínico en ronda
- Sistema de IA/ML analizando patrones

**Destino:** alimenta decisiones (ajustar perfil de monitoreo, plan de cuidado).

**Estado actual en mana-hub:** hoy no tenemos tabla para hallazgos.
Las notas clínicas (`care_notes` tipo OBSERVATION) son el precursor más cercano.
Ver §6 (deuda de modelo).

| Antes se llamaba | Ahora se llama |
|------------------|----------------|
| Observación clínica | **Hallazgo** |
| Insight | **Hallazgo** |
| Recomendación | **Hallazgo** (con subtype) |

---

## 4. MAPEO CANÓNICO ↔ MOTOR ↔ MANA-HUB

| Término Canónico | Motor de Percepción | Contexto mana-hub | Código (actual) | Tabla (actual) |
|------------------|--------------------|--------------------|------------------|----------------|
| Percepción | Observación (input) | Observation | `SensorEvent` | `sensor_events` |
| Cambio de Escena | Fact / Scene event | Observation | `SceneEvent`, `CurrentBedState` | `scene_events`, `current_bed_states` |
| Notificación | Harbor notify | Observation | `IngestNotificationRequest` ✅ | `notification_events` |
| Episodio | Episodio (Centinela/Harbor) | Surveillance | `Episode` ✅ | `episodes` ✅ |
| Evidencia | Recording/NVR clips | Evidence | `Evidence`, `Timeline`, `ClipWindow` | `evidence`, `timelines`, `clip_windows` |
| Hallazgo | *(no existe en motor)* | Care | `ResidentNote` ✅ | `resident_notes` ✅ |
| Resumen Clínico | *(no existe en motor)* | Observation | `SleepSummary`, `MobilitySummary`, `BathroomSummary` | `sleep_summaries`, etc. |

---

## 5. REGLAS DE USO DEL LENGUAJE

### ✅ DECIR

- "El sensor emitió una **percepción**"
- "Hubo un **cambio de escena** a las 03:12"
- "Eso generó solo una **notificación**, no llegó a episodio"
- "El **episodio** #123 se auto-resolvió cuando volvió a la cama"
- "La doctora registró un **hallazgo**: patrón de insomnio"

### ❌ NO DECIR

- ~~"La observación dice que..."~~ → ambiguo: ¿percepción o hallazgo?
- ~~"Alerta"~~ → genérico; usar notificación o episodio según corresponda
- ~~"Evento"~~ a secas → especificar: percepción, cambio de escena, episodio
- ~~"Insight"~~ → usar hallazgo (español clínico universal)

### Regla de desambiguación rápida

> ¿Lo dijo el SENSOR? → **Percepción**
> ¿Lo confirmó el MOTOR DE ESCENA? → **Cambio de Escena**
> ¿Solo informa? → **Notificación**
> ¿Requiere atención? → **Episodio**
> ¿Lo concluyó una PERSONA o una IA con análisis? → **Hallazgo**

---

## 6. DEUDA DE MODELO DETECTADA

| Deuda | Descripción | Prioridad | Estado |
|-------|-------------|-----------|--------|
| ~~Notificaciones no se persisten~~ | ~~Hoy las notificaciones (sin episodio) viven solo en el motor~~ | ~~Alta~~ | ✅ Resuelto |
| ~~Renombrar código~~ | ~~`Alert`→`Episode`~~, ~~`AlertLevel`→`Severity`~~ | ~~Baja~~ | ✅ Completado |
| Auto-resolución | Modelar cierre automático por ESTADO SEGURO con marca `resolvedBy=AUTO` | Alta | Pendiente |

---

## 7. RESUMEN EN UNA FRASE

> **Percepciones** crudas se confirman como **cambios de escena**;
> algunos solo **notifican**, otros abren **episodios**;
> todo converge en la **historia clínica**;
> y sobre ella, los expertos generan **hallazgos**.

---

*Documento creado: 2026-08-25*
*Versión: 1.0*
*Complementa: business-glossary-domain-model.md*
