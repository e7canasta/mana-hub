# Finding Policies — Guía de Integración (Panel UX)

> **Base URL:** `http://localhost:8080/api/v1/insights/policies`
> **Content-Type:** `application/json`
> **OpenAPI parcial:** [finding-policies-openapi.yaml](./finding-policies-openapi.yaml)
> **Swagger UI live:** `http://localhost:8080/swagger-ui.html` (filtrar por tag "Finding Policies")
> **Contexto de negocio:** Ver [insights-product-spec.md](./insights-product-spec.md) — reglas de hallazgos, cascada, y qué significa cada campo clínicamente.
> **Modelo de datos:** Ver [insights-data-model-guide.md](./insights-data-model-guide.md) — entidades, relaciones, y specs BDD que validan el comportamiento.

## Modelo de datos

Cada residente puede tener su propia **política de hallazgos**. Si no tiene, se usa la **default** (cascada).

```
FindingPolicy
├── id: String
├── residentId: String?        ← null en la default
├── default: Boolean           ← true = política global
├── version: Long              ← optimistic locking
├── sleep: SleepPolicy         ← 16 campos
├── care: CarePolicy           ← 2 campos
└── bathroom: BathroomPolicy   ← 3 campos
```

## Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/default` | Leer política default |
| `PUT` | `/default` | Actualizar política default |
| `GET` | `/{residentId}` | Leer política de un residente (cascada a default si no tiene) |
| `PUT` | `/{residentId}` | Crear o actualizar política de un residente |
| `PUT` | `/{residentId}/reset` | Borrar política propia → vuelve a cascada |

## Ejemplos curl

### 1. Leer la default

```bash
curl -s http://localhost:8080/api/v1/insights/policies/default | jq
```

Respuesta:

```json
{
  "id": "policy-default",
  "residentId": null,
  "default": true,
  "sleep": {
    "restlessHighEnabled": true,
    "restlessHighThreshold": 0.25,
    "restlessFragmentedEnabled": true,
    "restlessFragmentedThreshold": 0.35,
    "exitsRisingEnabled": true,
    "exitsRisingFactor": 1.15,
    "exitsRisingMinDelta": 0.3,
    "sleepInRangeEnabled": true,
    "sleepInRangeThreshold": 0.2,
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
    "careThinMinutes": 20.0
  },
  "bathroom": {
    "bathroomNightEnabled": true,
    "nightMinAvg": 1.0,
    "nightRiseFactor": 1.5
  },
  "version": 1
}
```

### 2. Actualizar solo un campo de la default

```bash
curl -s -X PUT http://localhost:8080/api/v1/insights/policies/default \
  -H 'Content-Type: application/json' \
  -d '{
    "sleep": {
      "dawnMinCount": 5
    }
  }' | jq
```

> **Importante:** Si mandás un JSON parcial, solo se actualizan los campos enviados. Los demás conservan su valor.

### 3. Crear política propia para un residente

```bash
curl -s -X PUT http://localhost:8080/api/v1/insights/policies/jose \
  -H 'Content-Type: application/json' \
  -d '{
    "sleep": {
      "restlessHighThreshold": 0.40,
      "dawnClusterEnabled": false
    },
    "bathroom": {
      "nightMinAvg": 2.0
    }
  }' | jq
```

### 4. Resetear a cascada

```bash
curl -s -X PUT http://localhost:8080/api/v1/insights/policies/jose/reset | jq
```

### 5. Verificar cascada

```bash
curl -s http://localhost:8080/api/v1/insights/policies/jose | jq
```

Si el residente no tiene política propia, retorna la default.

## Campos de SleepPolicy

| Campo | Tipo | Default | Descripción |
|-------|------|---------|-------------|
| `restlessHighEnabled` | Boolean | `true` | Regla de sueño inquieto alto |
| `restlessHighThreshold` | Double | `0.25` | Umbral (0–1). Por encima = hallazgo |
| `restlessFragmentedEnabled` | Boolean | `true` | Regla de sueño fragmentado |
| `restlessFragmentedThreshold` | Double | `0.35` | Umbral (0–1). Por encima = patológico |
| `exitsRisingEnabled` | Boolean | `true` | Regla de salidas de cama en aumento |
| `exitsRisingFactor` | Double | `1.15` | Factor de aumento entre semanas |
| `exitsRisingMinDelta` | Double | `0.3` | Diferencia mínima absoluta de salidas |
| `sleepInRangeEnabled` | Boolean | `true` | Regla de sueño en rango habitual |
| `sleepInRangeThreshold` | Double | `0.20` | Umbral de variación |
| `dropWoWEnabled` | Boolean | `true` | Regla de baja semana a semana |
| `dropWoWMinutes` | Int | `45` | Baja mínima en minutos |
| `dawnClusterEnabled` | Boolean | `true` | Regla de cluster de alba |
| `dawnFrom` | String | `"05:00"` | Inicio de ventana (HH:mm) |
| `dawnTo` | String | `"06:05"` | Fin de ventana (HH:mm) |
| `dawnMinCount` | Int | `3` | Mínimo de salidas en ventana |
| `dawnRatio` | Double | `0.66` | Proporción mínima (0–1) |

## Campos de CarePolicy

| Campo | Tipo | Default | Descripción |
|-------|------|---------|-------------|
| `careThinEnabled` | Boolean | `true` | Regla de poco cuidado. Apagar si cámara no cubre |
| `careThinMinutes` | Double | `20.0` | Minutos mínimos diarios. Por debajo = hallazgo |

## Campos de BathroomPolicy

| Campo | Tipo | Default | Descripción |
|-------|------|---------|-------------|
| `bathroomNightEnabled` | Boolean | `true` | Regla de visitas nocturnas. Apagar si nocturia conocida |
| `nightMinAvg` | Double | `1.0` | Promedio mínimo de visitas/noche |
| `nightRiseFactor` | Double | `1.5` | Factor de aumento entre semanas (1.5 = 50%) |

## Notas para el frontend

- **PUT parcial:** Mandá solo los campos que querés cambiar. No es necesario enviar todo el objeto.
- **Cascada:** `GET /{residentId}` siempre retorna una política completa (la propia o la default). No necesitás hacer fallback manual.
- **Reset:** `PUT /{residentId}/reset` borra la política propia. El residente vuelve a usar la default.
- **`default`:** El campo se llama `default` en JSON (no `isDefault`). Es un boolean.
- **`version`:** Se usa para optimistic locking. Si modificás la política y alguien más la cambió al mismo tiempo, vas a gettear un error. En ese caso, leé de nuevo y re-intentá.
- **Swagger:** La documentación completa está en `http://localhost:8080/swagger-ui/index.html` (sección "Finding Policies").
