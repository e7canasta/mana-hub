# Insights — Modelo de Datos para el Equipo Panel

> **Para:** Equipo de desarrollo del panel UX
> **Versión:** POC — Primeros 14 días
> **Módulo:** mana-hub / insights
> **Complemento de:** [insights-product-spec.md](./insights-product-spec.md) · [finding-policies-integration-guide.md](./finding-policies-integration-guide.md) · [finding-policies-openapi.yaml](./finding-policies-openapi.yaml)

---

## Qué es este documento

Este documento traduce las **specs BDD** del dominio insights en un lenguaje que el equipo de panel puede usar para diseñar las pantallas. No es una referencia de API (eso está en la guía de integración), sino una explicación de **qué datos existen, cómo se relacionan, y qué significa cada cosa**.

Las specs BDD son los tests que validan el comportamiento del sistema. Cada escenario es una historia clínica real que el panel debe poder mostrar.

---

## El pipeline de datos

```
Cámaras / sensores
       ↓
  Resúmenes diarios (sleep_summaries, bathroom_summaries, care_summaries)
       ↓
  Línea base (7 días mínimos)
       ↓
  FindingCatalog → Hallazgos (código + tipo + polaridad + severidad)
       ↓
  WellbeingRecommendations → Recomendaciones (texto + severidad)
       ↓
  SleepBriefing → Tarjetas KPI + Narrativa
       ↓
  Panel del director médico
```

---

## Entidades principales

### 1. Baseline — "¿Ya tenemos suficientes datos?"

> Spec: `ClinicalDecisionFlowSpec` — sección "Baseline — forming vs ready"

La línea base es el **punto de referencia** para cada residente. El sistema compara contra *cómo duerme esta persona*, no contra un promedio poblacional.

| Campo | Tipo | Qué significa |
|-------|------|---------------|
| `admissionDate` | Date | Fecha de alta del residente |
| `observedFrom` | Date | Desde cuándo se observa |
| `observedDays` | Int | Cuántos días tiene de observación |
| `ready` | Boolean | `true` si tiene ≥7 días (configurable) |

**Regla clave:**
- `ready = false` → Solo se muestra `BASELINE_FORMING`. No hay tendencias ni alertas.
- `ready = true` → Se activan todas las reglas de análisis.

**Lo que el panel debe mostrar:**

| Estado | Tarjeta | Ejemplo |
|--------|---------|---------|
| Forming | "Línea base en formación" | "Alta hace 3 días. Hacen falta 7 días para hablar de tendencias." |
| Ready | Todas las tarjetas KPI | Narrativa + hallazgos + recomendaciones |

---

### 2. FindingPolicy — "Qué reglas están activas para este residente"

> Spec: `FindingPolicySpec` — secciones "DEFAULTS", "APAGAR REGLAS", "CAMBIAR UMBRALES", "RESIDENTE vs DEFAULT"

Cada residente tiene una FindingPolicy. Es la configuración que el director médico usa para personalizar qué se monitorea.

**Estructura:**

```
FindingPolicy
├── id: String
├── residentId: String?     ← null = es la default
├── default: Boolean        ← true = cubre a todos
├── version: Long           ← optimistic locking
├── sleep: SleepPolicy      ← 6 reglas, 16 campos
├── care: CarePolicy        ← 1 regla, 2 campos
└── bathroom: BathroomPolicy ← 1 regla, 3 campos
```

**Cascada:**
```
¿El residente tiene política propia?
  → SÍ: se usa esa
  → NO: se usa la default
```

**Lo que el panel debe mostrar:**

Para cada regla, un toggle on/off + el umbral ajustable:

| Categoría | Regla | Toggle | Umbral | Default |
|-----------|-------|--------|--------|---------|
| Sueño | Sueño inquieto alto | `restlessHighEnabled` | `restlessHighThreshold` | 0.25 (25%) |
| Sueño | Sueño fragmentado | `restlessFragmentedEnabled` | `restlessFragmentedThreshold` | 0.35 (35%) |
| Sueño | Sueño en rango | `sleepInRangeEnabled` | `sleepInRangeThreshold` | 0.20 (20%) |
| Sueño | Salidas en aumento | `exitsRisingEnabled` | `exitsRisingFactor` | 1.15 (15%) |
| Sueño | Cluster de alba | `dawnClusterEnabled` | `dawnMinCount` + `dawnRatio` | 3 + 0.66 |
| Sueño | Baja semana a semana | `dropWoWEnabled` | `dropWoWMinutes` | 45 min |
| Cuidado | Poco cuidado medido | `careThinEnabled` | `careThinMinutes` | 20 min/día |
| Baño | Visitas nocturnas | `bathroomNightEnabled` | `nightMinAvg` + `nightRiseFactor` | 1.0 + 1.5 |

**Spec que valida esto:** `FindingPolicySpec` — cuando `restlessHighEnabled=false`, el sistema NO genera `SLEEP_RESTLESS_HIGH` aunque el umbral se supera.

---

### 3. Finding — "Qué detectó el sistema"

> Spec: `FindingCatalogExtendedSpec` — todas las secciones

Un finding es un **evento de análisis** que el sistema genera cuando detecta algo. No es una alerta — es información para que el director decida.

| Campo | Tipo | Qué significa |
|-------|------|---------------|
| `code` | String | Identificador único (ej: `SLEEP_RESTLESS_HIGH`) |
| `kind` | FindingKind | `WATCH`, `TREND`, `CLUSTER`, `POLICY`, `BRIEFING` |
| `polarity` | Polarity | `CONCERN`, `POSITIVE`, `NEUTRAL` |
| `severity` | String | `info`, `warning` |
| `body` | String | Texto en lenguaje natural |
| `headline` | String? | Título corto |
| `evidence` | Map | Datos que sustentan el hallazgo |
| `awaitingDecision` | Boolean | Requiere decisión del equipo |
| `proposal` | Proposal? | Acción sugerida |

**Los 9 tipos de hallazgo:**

| # | Código | Kind | Polaridad | Se puede apagar | Spec que lo valida |
|---|--------|------|-----------|-----------------|-------------------|
| 1 | `BASELINE_FORMING` | WATCH | NEUTRAL | No | `FindingCatalogExtendedSpec` — "residente recien llegado solo muestra BASELINE_FORMING" |
| 2 | `BED_EXIT_DAWN_CLUSTER` | CLUSTER | CONCERN | Sí (`dawnClusterEnabled`) | `FindingCatalogExtendedSpec` — "cluster con todas las salidas en alba" |
| 3 | `POLICY_BED_EDGE_DAWN` | POLICY | CONCERN | No | `FindingCatalogExtendedSpec` — "cluster con warning de borde produce POLICY_BED_EDGE_DAWN" |
| 4 | `BED_EXITS_RISING` | TREND | CONCERN | Sí (`exitsRisingEnabled`) | `FindingCatalogExtendedSpec` — "salidas suben respecto de la semana anterior" |
| 5 | `SLEEP_RESTLESS_HIGH` | TREND | CONCERN | Sí (`restlessHighEnabled`) | `FindingCatalogExtendedSpec` — "restless share mayor a 25% genera SLEEP_RESTLESS_HIGH" |
| 6 | `BATHROOM_NIGHT_UP` | TREND | CONCERN | Sí (`bathroomNightEnabled`) | `FindingCatalogExtendedSpec` — "banos nocturnos suben mas de 1.5x" |
| 7 | `CARE_THIN` | WATCH | CONCERN | Sí (`careThinEnabled`) | `FindingCatalogExtendedSpec` — "promedio menor a 20 minutos genera CARE_THIN" |
| 8 | `SLEEP_IN_RANGE` | WATCH | POSITIVE | Sí (`sleepInRangeEnabled`) | `FindingCatalogExtendedSpec` — "restless share menor o igual a 20% genera SLEEP_IN_RANGE" |
| 9 | `SLEEP_14D_BRIEFING` | BRIEFING | NEUTRAL | No | `FindingCatalogExtendedSpec` — "residente con sueño estable genera SLEEP_14D_BRIEFING" |

**Lo que el panel debe mostrar:**

- **Briefing:** Solo `BRIEFING` + `WATCH` + `POSITIVE`
- **Alertas:** `TREND` + `CLUSTER` con `CONCERN`
- **Decisiones pendientes:** `awaitingDecision = true` con `proposal`

---

### 4. Recomendación — "Qué sugiere el sistema"

> Spec: `WellbeingRecommendationsExtendedSpec` — todas las secciones

Las recomendaciones son **opiniones del sistema** basadas en la línea base. No son órdenes.

| Campo | Tipo | Qué significa |
|-------|------|---------------|
| `code` | String | Identificador único |
| `severity` | String | `info`, `warning` |
| `text` | String | Texto en lenguaje natural |

**Recomendaciones de sueño:**

| Código | Severidad | Cuándo se genera | Spec |
|--------|-----------|------------------|------|
| `BASELINE_FORMING` | info | Primeros 7 días | "linea base en formacion solo retorna BASELINE_FORMING" |
| `SLEEP_IN_RANGE` | info | restlessShare ≤ 20% | "restless share dentro del rango genera SLEEP_IN_RANGE" |
| `SLEEP_RESTLESS` | warning | restlessShare 20–35% | "restless share entre 20 y 35 genera SLEEP_RESTLESS" |
| `SLEEP_FRAGMENTED` | warning | restlessShare > 35% | "restless share mayor a 35 genera SLEEP_FRAGMENTED" |
| `SLEEP_DROP_WOW` | warning | Calm bajó ≥45 min | "delta negativo mayor a 45 minutos agrega SLEEP_DROP_WOW" |

**Recomendaciones de cuidado:**

| Código | Severidad | Cuándo se genera |
|--------|-----------|------------------|
| `CARE_BASELINE_FORMING` | info | Primeros 7 días |
| `CARE_NOT_MEASURED` | info | Sin rollup de cuidado |
| `CARE_NONE` | info | 0 minutos medidos |

**Recomendaciones de episodios:**

| Código | Severidad | Cuándo se genera |
|--------|-----------|------------------|
| `EPISODE_SELF_RECOVERY` | info | Autorecuperación |
| `EPISODE_STAFF_CLOSED` | info | Personal resolvió |

---

### 5. SleepBriefing — "Las tarjetas KPI y la narrativa"

> Spec: `SleepBriefingSpec` — todas las secciones

El briefing es lo que el director ve primero. Son **tarjetas resumen** + una **narrativa en lenguaje natural**.

**Las 4 tarjetas KPI:**

| Código | Qué muestra | Ejemplo |
|--------|-------------|---------|
| `RESTLESS` | Sueño inquieto: minutos + % | "1h 37 — 28% del total dormido" |
| `BED_EXITS` | Salidas de cama: promedio + máximo | "3.0 — máximo: 5" |
| `TIME_IN_BED` | Tiempo total en cama | "6h 30 — incluye despertares" |
| `EFFICIENCY` | Eficiencia de sueño | "89% — dormido sobre tiempo en cama" |

**La narrativa:**

El sistema genera una oración en lenguaje natural:
> "Durmió 5h 30min por noche en promedio, dentro de su rango habitual, con 2.1 salidas de cama por noche. Las salidas vienen aumentando respecto de la semana anterior."

**Spec que valida:** `SleepBriefingSpec` — "narrativa describe horas dormidas y rango habitual"

---

### 6. PolicyCopy — "Las alarmas en lenguaje hablado"

> Spec: `PolicyCopySpec` — todas las secciones

PolicyCopy traduce la configuración de alarmas del residente a **líneas que el director puede leer**.

**Los 4 estados que se muestran:**

| Estado | Label | Qué significa |
|--------|-------|---------------|
| `SITTING_IN_BED` | Sentado en cama | El residente está sentado pero en la cama |
| `BED_EDGE` | Al borde de la cama | Está al borde, a punto de bajarse |
| `STANDING` | De pie | Está de pie, fuera de la cama |
| `ABSENT` | Sin observación | No se ve en cámara |

**Ejemplo de línea hablada:**
> "Al borde de la cama: avisa a los 1 min, escala a los 2"

**Override del director:**
Cuando el director ajusta un tiempo manualmente, la línea dice "Ajuste manual".

---

### 7. BedExits — "Cómo se detectan las salidas de cama"

> Spec: `BedExitsSpec` — todas las secciones

Una **salida de cama** es cuando el residente pasa de un estado `inBed` a uno que no lo es.

| Transición | ¿Es salida? |
|------------|-------------|
| `LYING → STANDING` | Sí |
| `SITTING_IN_BED → STANDING` | Sí |
| `BED_EDGE → STANDING` | Sí |
| `STANDING → LYING` | No (volver a la cama) |
| `LYING → SITTING_IN_BED` | No (sigue en cama) |

**Ventana de alba (05:00–06:05):**
Las salidas en esta franja se agrupan en un "cluster de alba" que dispara alertas.

**Staff después de una salida:**
Si una enfermera llega dentro de los 20 minutos después de una salida, se cuenta como "necesitó atención".

---

## Cómo se relaciona todo

```
┌─────────────────────────────────────────────────────────┐
│                    FindingPolicy                         │
│  (qué reglas están activas para este residente)         │
└──────────────────────┬──────────────────────────────────┘
                       │ configura
                       ▼
┌─────────────────────────────────────────────────────────┐
│                    FindingCatalog                         │
│  (evalúa las reglas contra los datos)                    │
│                                                          │
│  Inputs:                                                 │
│  ├── SleepDerived (resumen de sueño 7d)                 │
│  ├── BathroomSummary (resumen de baño)                  │
│  ├── CareSummary (resumen de cuidado)                   │
│  ├── BedExits (salidas de cama)                         │
│  └── Baseline (línea base)                              │
│                                                          │
│  Output: List<Finding>                                   │
└──────────────────────┬──────────────────────────────────┘
                       │ genera
                       ▼
┌─────────────────────────────────────────────────────────┐
│              WellbeingRecommendations                     │
│  (opiniones del sistema basadas en hallazgos)            │
└──────────────────────┬──────────────────────────────────┘
                       │ alimenta
                       ▼
┌─────────────────────────────────────────────────────────┐
│                   SleepBriefing                           │
│  (tarjetas KPI + narrativa en lenguaje natural)          │
└──────────────────────┬──────────────────────────────────┘
                       │ muestra
                       ▼
┌─────────────────────────────────────────────────────────┐
│                  Panel del Director                       │
│  Briefing → Hallazgos → Recomendaciones → Configuración │
└─────────────────────────────────────────────────────────┘
```

---

## Datos de entrada (lo que el panel recibe)

### Resúmenes diarios

El panel no necesita crear estos datos — vienen de las cámaras. Pero necesita saber qué significan.

**sleep_summaries:**

| Campo | Tipo | Qué significa |
|-------|------|---------------|
| `calmMinutes` | Int | Minutos dormido tranquilo |
| `restlessMinutes` | Int | Minutos en cama inquieto |
| `awakeMinutes` | Int | Minutos despierto en cama |
| `outOfBedMinutes` | Int | Minutos fuera de la cama |
| `bedExitCount` | Int | Cantidad de salidas de cama |
| `wakeCount` | Int | Cantidad de despertares |

**bathroom_summaries:**

| Campo | Tipo | Qué significa |
|-------|------|---------------|
| `visitCount` | Int | Visitas totales al baño |
| `nightVisitCount` | Int | Visitas nocturnas |
| `assistedCount` | Int | Visitas con asistencia |
| `totalMinutes` | Int | Tiempo total en baño |

**care_summaries:**

| Campo | Tipo | Qué significa |
|-------|------|---------------|
| `totalMinutes` | Int | Minutos totales de cuidado medido |
| `proactiveMinutes` | Int | Minutos de cuidado proactivo |

---

## Datos calculados (lo que el panel muestra)

### SleepDerived — "los números del sueño"

| Campo | Tipo | Qué significa |
|-------|------|---------------|
| `avgCalmMinutes7d` | Int? | Promedio 7d de sueño calm |
| `avgRestlessMinutes7d` | Int? | Promedio 7d de sueño inquieto |
| `avgAsleepMinutes7d` | Int? | Promedio 7d total dormido |
| `restlessShare` | Double? | % de sueño inquieto sobre total |
| `avgBedExits` | Double? | Promedio de salidas por noche |
| `maxBedExits` | Int? | Máximo de salidas en un día |
| `sleepEfficiency` | Double? | Dormido / tiempo en cama |
| `deltaCalmMinutesWoW` | Int? | Diferencia de sueño calm semana a semana |

---

## Escenarios del panel

### Escenario 1: Director abre el briefing de un residente

1. Llama `GET /api/v1/insights/residents/{id}/briefing?days=14`
2. Recibe:
   - `baselineReady`: si es `false`, mostrar "Línea base en formación"
   - `sleepCards`: las 4 tarjetas KPI
   - `narrative`: la oración en lenguaje natural
   - `findings`: la lista de hallazgos
   - `recommendations`: las recomendaciones
   - `policyToday`: las reglas de alarma habladas

### Escenario 2: Director configura reglas de un residente

1. Ve `GET /api/v1/insights/policies/{residentId}`
2. Modifica toggles y umbrales
3. Envía `PUT /api/v1/insights/policies/{residentId}`
4. El sistema aplica los cambios a partir de la próxima evaluación

### Escenario 3: Director resetea a default

1. Envía `PUT /api/v1/insights/policies/{residentId}/reset`
2. El residente vuelve a usar la política default
3. La respuesta muestra la default aplicada

---

## Glosario para el panel

| Término | Significado técnico | Significado clínico |
|---------|--------------------|--------------------|
| **Baseline** | `Baseline.ready` | "¿Ya sabemos cómo duerme esta persona?" |
| **Finding** | `Finding.code` + `kind` | "Algo que el sistema notó" |
| **Hallazgo** | Finding con `polarity = CONCERN` | "Algo que vale la pena mirar" |
| **Recomendación** | `Recommendation.code` | "Lo que el sistema sugiere" |
| **Briefing** | `SleepBriefing` | "Resumen rápido de cómo está todo" |
| **Policy** | `FindingPolicy` | "Qué reglas están activas" |
| **Cascada** | `residentId = null` | "Si no tiene política propia, se usa la default" |
| **Cluster de alba** | `BedExits.isDawn()` | "Varias salidas entre 5:00 y 6:05" |
| **Restless share** | `restlessMinutes / asleepMinutes` | "% de tiempo inquieto sobre total dormido" |
| **Delta WoW** | `deltaCalmMinutesWoW` | "Cómo cambió el sueño calm de una semana a la otra" |
