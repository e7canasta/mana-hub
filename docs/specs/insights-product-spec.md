# Insights — Especificación de Producto

> **Para:** Director Médico, Jefa de Enfermería, Equipo Clínico
> **Versión:** POC — Primeros 14 días
> **Módulo:** mana-hub / insights

---

## Qué es esto

Insights es un módulo que **aprende** el patrón de cada residente y **avisa** cuando algo cambia. No reemplaza al personal: le da la información correcta, en el momento correcto, para que decida.

No es un tablero de números. Es un sistema que dice: *"María duerme peor que la semana pasada"* — no *"María tuvo 3.2 horas de sueño calmado"*.

**Lo que lo hace distinto:** Cada regla se puede **prender o apagar** individualmente por residente. Si la cámara no cubre la habitación de Juan, se apaga `CARE_THIN`. si María tiene nocturia conocida, se apaga `BATHROOM_NIGHT_UP`. El director define qué quiere monitorear para cada persona.

---

## Cómo funciona (la idea en una frase)

```
Cámara → datos crudos → resumen diario → línea base → hallazgos → recomendaciones
```

1. **Cada noche**, el sistema consolida lo que la cámara observó: cuánto durmió, cuántas veces se levantó, si fue al baño.
2. **Después de 7 días**, tiene una línea base: cómo duerme *esta persona* normalmente.
3. **A partir de ahí**, compara cada noche contra esa línea base. Si algo cambia, genera un hallazgo.

---

## Las Policies: qué prende y apaga el director

Cada residente tiene una **FindingPolicy** — la configuración de qué reglas están activas y con qué sensibilidad.

### Cómo funciona la cascada

```
¿Este residente tiene política propia?
  → SÍ: se usa esa
  → NO: se usa la default (la que cubre a todos)
    → Si no hay default, se crea una con todo prendido
```

### Qué se puede configurar por residente

| Categoría | Regla | Se puede apagar | Se puede ajustar umbral |
|-----------|-------|-----------------|------------------------|
| **Sueño** | Sueño inquieto alto | Sí | `restlessHighThreshold` (default: 25%) |
| **Sueño** | Sueño fragmentado | Sí | `restlessFragmentedThreshold` (default: 35%) |
| **Sueño** | Sueño en rango | Sí | `sleepInRangeThreshold` (default: 20%) |
| **Sueño** | Salidas de cama en aumento | Sí | `exitsRisingFactor` (default: 15%) |
| **Sueño** | Salidas de cama en alba | Sí | `dawnMinCount` (default: 3), `dawnRatio` (default: 66%) |
| **Sueño** | Baja de sueño semana a semana | Sí | `dropWoWMinutes` (default: 45 min) |
| **Cuidado** | Poco cuidado medido | Sí | `careThinMinutes` (default: 20 min/día) |
| **Baño** | Visitas nocturnas en aumento | Sí | `nightMinAvg` (default: 1), `nightRiseFactor` (default: 50%) |

### Ejemplo: el director configura a María

María tiene nocturia conocida y la cámara no cubre bien su habitación. El director apaga esas reglas:

```json
PUT /api/v1/insights/policies/maria
{
  "sleep": {
    "restlessHighEnabled": true,
    "dawnClusterEnabled": true
  },
  "care": {
    "careThinEnabled": false
  },
  "bathroom": {
    "bathroomNightEnabled": false
  }
}
```

**Resultado:** El sistema ya no le avisa a María por cuidado delgado ni por visitas nocturnas. Pero sigue monitoreando sueño inquieto y cluster de alba.

---

## El primer día al décimo cuarto: qué ve el equipo

### Días 1–6: "Estamos aprendiendo"

El sistema está en modo de aprendizaje. No tiene suficientes días para saber qué es normal para *este* residente.

**Lo que ve el equipo:**

| Tarjeta | Qué muestra |
|---------|-------------|
| **Línea base en formación** | "Alta hace 3 días. Hacen falta 7 días para hablar de tendencias. No evaluar umbrales todavía." |

**Qué NO hace:**
- No genera alertas
- No compara contra nada
- No dice "está bien" ni "está mal"

**Por qué importa:** En la POC, estos primeros días son donde el director médico y la jefa de enfermería ven que el sistema *existe* y *funciona*. No promete nada que no puede cumplir. Es honesto.

> *Steve Jobs diría: "Mostrar lo que sabemos hacer, no lo que no sabemos hacer."*

---

### Día 7: "Ya tenemos línea base"

El sistema ahora sabe cómo duerme *esta persona*. A partir de aquí, cada noche se compara contra su propio promedio.

**Lo que aparece:**

| Tarjeta | Qué muestra |
|---------|-------------|
| **Tiempo en sueño inquieto** | Promedio de los últimos 7 días, con % del total dormido |
| **Salidas de cama por noche** | Promedio + máximo en la ventana |
| **Tiempo total en cama** | Incluye despertares |
| **Eficiencia de sueño** | Dormido / tiempo en cama |
| **Narrativa** | "Durmió 5h 30min por noche en promedio, dentro de su rango habitual, con 2.1 salidas de cama por noche." |

---

### Días 8–14: "Detectamos cambios"

El sistema ahora puede decir qué cambió. Cada hallazgo tiene un **código**, un **tipo** y un **tono**.

---

## Los hallazgos: qué detecta el sistema

### Tipos de hallazgo

| Tipo | Qué significa | Cuándo aparece |
|------|---------------|----------------|
| **WATCH** | Algo que vale la pena mirar, sin urgencia | Cambios leves, datos nuevos |
| **TREND** | Una tendencia que se confirmó con datos | Comparación semana a semana |
| **CLUSTER** | Un patrón concentrado en tiempo | Agrupaciones de eventos |
| **POLICY** | Requiere una decisión del equipo | Cuando la configuración actual no cubre lo que se ve |
| **BRIEFING** | Resumen narrativo del período | Siempre que hay datos suficientes |

### Tono (polaridad)

| Tono | Significado |
|------|-------------|
| **CONCERN** | Algo requiere atención |
| **POSITIVE** | Todo está dentro del rango esperado |
| **NEUTRAL** | Informativo, sin juicio |

---

## Reglas de negocio: los hallazgos detallados

### 1. BASELINE_FORMING
**Código:** `BASELINE_FORMING`
**Tipo:** WATCH | **Tono:** NEUTRAL | **Se puede apagar:** No (siempre activo)
**Cuándo:** Los primeros 7 días desde el alta.
**Qué dice:** "Alta hace X días. Hacen falta 7 días para hablar de tendencias."
**Acción:** Ninguna. El sistema está aprendiendo.

---

### 2. BED_EXIT_DAWN_CLUSTER
**Código:** `BED_EXIT_DAWN_CLUSTER`
**Tipo:** CLUSTER | **Tono:** CONCERN | **Se puede apagar:** Sí (`dawnClusterEnabled`)
**Cuándo:** 3 o más salidas de cama en 7 días, y al menos 66% son entre las 5:00 y las 6:05.
**Qué dice:** "En los últimos siete días María salió de la cama 4 veces, siempre entre las 5:10 y las 5:55."
**Por qué importa:** Las salidas de madrugada son el momento de mayor riesgo de caída.

---

### 3. POLICY_BED_EDGE_DAWN
**Código:** `POLICY_BED_EDGE_DAWN`
**Tipo:** POLICY | **Tono:** CONCERN | **Requiere decisión:** Sí | **Se puede apagar:** No (viene con el cluster)
**Cuándo:** Hay un cluster de alba Y la alarma de borde de cama tiene un margen de espera.
**Qué dice:** "El aviso en el borde de la cama espera 3 minutos; en esa franja el retraso importa."
**Qué propone:** "Avisar apenas se detecte el borde de la cama, en lugar de esperar 3 minutos."

---

### 4. BED_EXITS_RISING
**Código:** `BED_EXITS_RISING`
**Tipo:** TREND | **Tono:** CONCERN | **Se puede apagar:** Sí (`exitsRisingEnabled`)
**Cuándo:** El promedio de salidas de cama de la última semana es ≥15% mayor que la semana anterior, con una diferencia de al menos 0.3 salidas.
**Qué dice:** "En la última semana hubo 3.2 salidas por noche, frente a 2.1 la semana anterior."
**Por qué importa:** Un aumento sostenido puede indicar dolor, incomodidad o cambio en el estado neurológico.

---

### 5. SLEEP_RESTLESS_HIGH
**Código:** `SLEEP_RESTLESS_HIGH`
**Tipo:** TREND | **Tono:** CONCERN | **Se puede apagar:** Sí (`restlessHighEnabled`)
**Umbral:** `restlessHighThreshold` (default: 25%)
**Qué dice:** "Tiempo en sueño inquieto 1h 45min, 32% del total dormido."
**Por qué importa:** Sueño inquieto alto puede indicar dolor, incomodidad postural o efecto medicación.

---

### 6. BATHROOM_NIGHT_UP
**Código:** `BATHROOM_NIGHT_UP`
**Tipo:** TREND | **Tono:** CONCERN | **Se puede apagar:** Sí (`bathroomNightEnabled`)
**Umbral:** `nightMinAvg` (default: 1) y `nightRiseFactor` (default: 50%)
**Qué dice:** "En la última semana hubo 2.3 visitas nocturnas por día, frente a 1.4 la semana anterior."
**Por qué importa:** Aumento de visitas nocturnas = más riesgo de caída + más interrupciones de sueño.

---

### 7. CARE_THIN
**Código:** `CARE_THIN`
**Tipo:** WATCH | **Tono:** CONCERN | **Se puede apagar:** Sí (`careThinEnabled`)
**Umbral:** `careThinMinutes` (default: 20 min/día)
**Qué dice:** "El tiempo de cuidado medido ronda 12.5 min por día."
**Por qué importa:** No significa que no haya cuidado: significa que la cámara no lo vio.

---

### 8. SLEEP_IN_RANGE
**Código:** `SLEEP_IN_RANGE`
**Tipo:** WATCH | **Tono:** POSITIVE | **Se puede apagar:** Sí (`sleepInRangeEnabled`)
**Umbral:** `sleepInRangeThreshold` (default: 20%)
**Qué dice:** "Comparado contra su propia línea base, no contra un estándar: en su rango."

---

### 9. SLEEP_14D_BRIEFING
**Código:** `SLEEP_14D_BRIEFING`
**Tipo:** BRIEFING | **Tono:** NEUTRAL | **Se puede apagar:** No (siempre activo)
**Qué dice:** Narrativa en lenguaje natural: "Durmió 6h 10min por noche en promedio, dentro de su rango habitual."

---

## Las recomendaciones: qué sugiere el sistema

Las recomendaciones **no son órdenes**. Son información para que el equipo decida.

### Recomendaciones de sueño

| Código | Severidad | Se puede apagar | Cuándo | Qué dice |
|--------|-----------|-----------------|--------|----------|
| `BASELINE_FORMING` | info | No | Días 1–6 | "Alta hace X días. No evaluar tendencias todavía." |
| `SLEEP_IN_RANGE` | info | Sí | restlessShare ≤ umbral | "En su rango habitual." |
| `SLEEP_RESTLESS` | warning | Sí | restlessShare 20–35% | "Sueño inquieto por encima de su rango habitual." |
| `SLEEP_FRAGMENTED` | warning | Sí | restlessShare > 35% | "Noche muy fragmentada. Revisar perfil ComeBack." |
| `SLEEP_DROP_WOW` | warning | Sí | Calm bajó ≥45 min | "Duerme bastante menos que la semana anterior." |

### Recomendaciones de cuidado

| Código | Severidad | Cuándo | Qué dice |
|--------|-----------|--------|----------|
| `CARE_BASELINE_FORMING` | info | Días 1–6 | "El gráfico en cero no es una caída de actividad." |
| `CARE_NOT_MEASURED` | info | Sin datos | "Todavía no hay un rollup de cuidado." |
| `CARE_NONE` | info | 0 minutos | "Sin visitas de cuidado registradas." |

### Recomendaciones de episodios

| Código | Severidad | Cuándo | Qué dice |
|--------|-----------|--------|----------|
| `EPISODE_SELF_RECOVERY` | info | Autorecuperación | "El episodio se cerró porque volvió solo al estado seguro." |
| `EPISODE_STAFF_CLOSED` | info | Personal resolvió | "Episodio resuelto con intervención." |

---

## Las alarmas: qué avisa y cuándo

Cada residente tiene un **perfil de riesgo** que determina cuánto espera antes de alertar.

### Niveles de riesgo

| Nivel | Label | Significado |
|-------|-------|-------------|
| **LOW** | Vigilancia baja | Residente estable, sin factores de riesgo de caída |
| **MEDIUM** | Deambulación nocturna | Tiende a levantarse de noche |
| **HIGH** | Riesgo de caída | Historial de caídas o factores de riesgo altos |
| **CRITICAL** | Crítico | Riesgo muy alto, requiere vigilancia intensiva |

### Tiempos de alarma por nivel y estado

| Estado | HIGH (min aviso → alerta) | MEDIUM | CRITICAL |
|--------|---------------------------|--------|----------|
| **Sentado en cama** | 15 → 20 | 20 → 30 | 10 → 15 |
| **Al borde de la cama** | 1 → 2 | 3 → 5 | 1 → 2 |
| **De pie** | 2 → 3 | 10 → 15 | 2 → 3 |
| **Sin observación** | 5 → 10 | 5 → 10 | 2 → 5 |

---

## KPIs que el equipo ve en la pantalla

### Tarjetas de sueño

| Tarjeta | Qué muestra | Ejemplo |
|---------|-------------|---------|
| **Tiempo en sueño inquieto** | Promedio 7d + % del total | "1h 45min — 32% del total dormido" |
| **Salidas de cama por noche** | Promedio 7d + máximo | "2.1 — máximo: 5" |
| **Tiempo total en cama** | Promedio 7d | "8h 20min — incluye despertares" |
| **Eficiencia de sueño** | Dormido / tiempo en cama | "72%" |

### Resumen narrativo

El sistema genera una oración en lenguaje natural:
> "Durmió 5h 30min por noche en promedio, por debajo de su rango habitual, con 2.1 salidas de cama por noche. Las salidas vienen aumentando respecto de la semana anterior."

---

## Las pantallas

### Por residente
- **Briefing:** Tarjetas KPI + narrativa + hallazgos + recomendaciones + política de alarmas
- **Reporte:** Todo lo del briefing + historial de episodios
- **Políticas de monitoreo:** Qué reglas están activas, con qué umbrales

### Por instalación
- **Briefing de instalación:** Cuántos residentes, cuántos en baseline forming, qué necesita revisión, qué está positivo
- **Reporte de instalación:** Todo el briefing + reportes individuales

---

## Configuración de políticas por residente

### Endpoints

| Método | Ruta | Qué hace |
|--------|------|----------|
| `GET` | `/api/v1/insights/policies/default` | Ver la política default (cubre a todos) |
| `PUT` | `/api/v1/insights/policies/default` | Actualizar la default |
| `GET` | `/api/v1/insights/policies/{residentId}` | Ver la política de un residente (o la default si no tiene) |
| `PUT` | `/api/v1/insights/policies/{residentId}` | Crear o actualizar la política de un residente |
| `PUT` | `/api/v1/insights/policies/{residentId}/reset` | Volver a la default |

### Ejemplo de solicitud

```json
PUT /api/v1/insights/policies/jose
{
  "sleep": {
    "restlessHighEnabled": false,
    "restlessHighThreshold": 0.35,
    "dawnClusterEnabled": true,
    "exitsRisingEnabled": true,
    "exitsRisingFactor": 1.20
  },
  "care": {
    "careThinEnabled": true,
    "careThinMinutes": 15.0
  }
}
```

### Respuesta

```json
{
  "id": "abc-123",
  "residentId": "jose",
  "isDefault": false,
  "sleep": {
    "restlessHighEnabled": false,
    "restlessHighThreshold": 0.35,
    "restlessFragmentedEnabled": true,
    "restlessFragmentedThreshold": 0.35,
    "exitsRisingEnabled": true,
    "exitsRisingFactor": 1.20,
    "exitsRisingMinDelta": 0.3,
    "sleepInRangeEnabled": true,
    "sleepInRangeThreshold": 0.20,
    "dropWoWEnabled": true,
    "dropWoWMinutes": 45,
    "dawnClusterEnabled": true,
    "dawnFrom": "05:00",
    "dawnTo": "06:05",
    "dawnMinCount": 3,
    "dawnRatio": 0.66
  },
  "care": {
    "careThinEnabled": true,
    "careThinMinutes": 15.0
  },
  "bathroom": {
    "bathroomNightEnabled": true,
    "nightMinAvg": 1.0,
    "nightRiseFactor": 1.5
  }
}
```

---

## Lo que el sistema NO hace (y por qué)

| No hace | Por qué |
|---------|---------|
| No diagnostica | No es un médico. Detecta patrones, no enfermedades. |
| No decide por el equipo | Propone, el equipo decide. |
| No reemplaza rondas | Las complementa con datos objetivos. |
| No manda alertas a las 3am | Las alertas son para el panel. El equipo las revisa cuando puede. |
| No compara contra estándares nacionales | Compara contra *este* residente. Cada persona es distinta. |

---

## Métricas de éxito de la POC

| Métrica | Target día 7 | Target día 14 |
|---------|-------------|--------------|
| Residentes con línea base | Todos los que entraron antes del día 7 | Todos |
| Hallazgos generados | Al menos 1 por residente con datos | 2–3 por residente |
| Políticas configuradas | 1 default funcionando | 3–5 residentes con políticas propias |
| Tiempo para revisar briefing | < 2 min por residente | < 1 min |
| Satisfacción jefa de enfermería | "Entiendo qué me dice" | "Lo uso para la reunión de staff" |

---

## Glosario para el equipo

| Término | Qué significa en lenguaje del día a día |
|---------|----------------------------------------|
| **Hallazgo** | Algo que el sistema notó y vale la pena mirar |
| **Línea base** | Cómo duerme *esta* persona normalmente |
| **Sueño inquieto** | Tiempo en la cama sin estar dormido (moviéndose, incomodo) |
| **Salida de cama** | El residente se levantó de la cama |
| **Cluster de alba** | Varias salidas de madrugada (5:00–6:05) |
| **Episodio** | Algo que requirió atención (caída, escape, etc.) |
| **Auto-recuperación** | El residente volvió al estado seguro sin ayuda |
| **Policy de monitoreo** | La configuración de alarmas de este residente |
| **Policy de insights** | Qué reglas de análisis están activas para este residente |
| **Briefing** | Resumen rápido de cómo está todo |
| **Ventana** | El período de tiempo que se está mirando (7, 14, 30 días) |
