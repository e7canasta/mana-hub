# mana-hub: User Stories del Director Médico

## Visión General

El director médico es el **usuario principal** del sistema. Sus necesidades definen el valor del producto.

---

## Épica 1: Gestión de Residentes

### US-001: Admitir un nuevo residente
**Como** director médico  
**Quiero** registrar un nuevo residente con sus datos demográficos y diagnóstico inicial  
**Para** poder comenzar a monitorearlo  

**Criterios de aceptación:**
- [ ] Nombre completo, fecha de nacimiento, fecha de admisión
- [ ] Diagnóstico inicial (texto libre)
- [ ] Estado: ACTIVE
- [ ] Asignación a cama (opcional en el momento de admisión)
- [ ] Id generado automáticamente

---

### US-002: Asignar residente a cama
**Como** director médico  
**Quiero** asignar un residente a una cama específica  
**Para** que el sistema sepa dónde está y pueda monitorearlo  

**Criterios de aceptación:**
- [ ] Un residente solo puede estar en una cama a la vez
- [ ] La cama debe estar AVAILABLE
- [ ] Se registra la fecha de asignación
- [ ] Se puede cambiar de cama (con historial)

---

### US-003: Ver historial completo de un residente
**Como** director médico  
**Quiero** ver TODA la información de un residente en una sola vista  
**Para** tomar decisiones informadas sobre su cuidado  

**Criterios de aceptación:**
- [ ] Datos demográficos actuales
- [ ] Cama actual
- [ ] Perfil de monitoreo actual
- [ ] Últimas 10 alertas
- [ ] Últimas 5 rondas médicas
- [ ] Resúmenes de los últimos 7 días
- [ ] Evidencia asociada

---

## Épica 2: Configuración de Monitoreo

### US-004: Configurar perfil de monitoreo de un residente
**Como** director médico  
**Quiero** definir cómo se monitorea a cada residente  
**Para** que las alertas se ajusten a sus necesidades específicas  

**Criterios de aceptación:**
- [ ] Seleccionar preset (fall_risk, wanderer, night_watch, default)
- [ ] Asignar nivel de riesgo (LOW, MEDIUM, HIGH)
- [ ] Indicar ayuda de movilidad (walker, wheelchair, none)
- [ ] Modo autopilot (on/off)
- [ ] Overrides personalizados (JSON)
- [ ] Validez temporal (validFrom, validTo)
- [ ] Razón del cambio

---

### US-005: Ver catálogo de presets disponibles
**Como** director médico  
**Quiero** ver qué presets de monitoreo existen y qué umbrales tienen  
**Para** elegir el más adecuado para cada residente  

**Criterios de aceptación:**
- [ ] Lista de presets con nombre y descripción
- [ ] Umbrales de cada preset (bedExitAlertMinutes, etc.)
- [ ] Indicación de si es genérico o personalizado

---

### US-006: Cambiar perfil de monitoreo
**Como** director médico  
**Quiero** cambiar el perfil de monitoreo de un residente cuando su condición cambia  
**Para** adaptar el monitoreo a su estado actual  

**Criterios de aceptación:**
- [ ] Se registra el cambio con timestamp
- [ ] Se guarda el perfil anterior (historial)
- [ ] Se indica la razón del cambio
- [ ] El nuevo perfil se activa inmediatamente

---

## Épica 3: Gestión de Episodios

### US-007: Ver episodios pendientes
**Como** director médico  
**Quiero** ver todos los episodios que están pendientes de atención  
**Para** priorizar qué atender primero  

**Criterios de aceptación:**
- [ ] Lista ordenada por severidad (CRITICAL > WARNING > INFO)
- [ ] Incluye: residente, cama, título, timestamp
- [ ] Filtrar por severidad
- [ ] Filtrar por residente

---

### US-008: Revisar y resolver un episodio
**Como** director médico  
**Quiero** ver el detalle de un episodio, su evidencia, y marcarlo como resuelto  
**Para** documentar qué pasó y qué se hizo  

**Criterios de aceptación:**
- [ ] Ver detalle completo del episodio
- [ ] Ver evidencia asociada (video, fotos)
- [ ] Marcar como reconocido (acknowledge)
- [ ] Agregar nota de resolución
- [ ] Cambiar estado a resolved

---

### US-009: Ver episodios de un residente específico
**Como** director médico  
**Quiero** ver el historial de episodios de un residente  
**Para** identificar patrones recurrentes  

**Criterios de aceptación:**
- [ ] Lista de todos los episodios del residente
- [ ] Ordenadas por fecha (más reciente primero)
- [ ] Incluye estado de cada uno
- [ ] Filtro por rango de fechas

---

## Épica 4: Historial Clínico

### US-010: Ver timeline completo de un residente
**Como** director médico  
**Quiero** ver TODOS los eventos de la vida de un residente en una línea de tiempo  
**Para** entender su evolución completa  

**Criterios de aceptación:**
- [ ] Línea de tiempo cronológica
- [ ] Incluye: admisiones, cambios de perfil, alertas, rondas, resúmenes
- [ ] Cada evento tiene timestamp, tipo, y detalle
- [ ] Se puede expandir cada evento para ver más info
- [ ] Filtrar por tipo de evento

---

### US-011: Ver resúmenes clínicos diarios
**Como** director médico  
**Quiero** ver los resúmenes de sueño, movilidad y baño de cada día  
**Para** detectar tendencias y anomalías  

**Criterios de aceptación:**
- [ ] Resumen de sueño: horas, despertares, tiempo fuera de cama
- [ ] Resumen de movilidad: distancia, transferencias
- [ ] Resumen de baño: visitas, nocturnas
- [ ] Comparar con días anteriores
- [ ] Gráficos de tendencia (opcional)

---

## Épica 5: Rondas Médicas

### US-012: Programar ronda médica
**Como** director médico  
**Quiero** crear una ronda médica para uno o más residentes  
**Para** asegurar que se realicen las revisiones necesarias  

**Criterios de aceptación:**
- [ ] Seleccionar residentes
- [ ] Definir tareas (presión arterial, peso, etc.)
- [ ] Asignar a enfermero
- [ ] Fecha/hora programada
- [ ] Estado: PENDING

---

### US-013: Completar ronda médica
**Como** enfermero (asignado)  
**Quiero** marcar las tareas como completadas y agregar notas  
**Para** documentar la atención realizada  

**Criterios de aceptación:**
- [ ] Marcar cada tarea como completada
- [ ] Agregar nota clínica por tarea
- [ ] Cambiar estado de ronda a COMPLETED
- [ ] Timestamp de completado

---

## Épica 6: Evidencia

### US-014: Ver evidencia de un evento
**Como** director médico  
**Quiero** ver la evidencia (video, fotos) asociada a un evento  
**Para** verificar qué pasó realmente  

**Criterios de aceptación:**
- [ ] Clip de video del evento
- [ ] Timeline de movimiento
- [ ] Timestamps precisos
- [ ] Links a evidencia almacenada

---

## Priorización (MoSCoW)

| Prioridad | User Stories |
|-----------|--------------|
| **Must Have** | US-001, US-002, US-004, US-007, US-008, US-010 |
| **Should Have** | US-003, US-005, US-006, US-009, US-011 |
| **Could Have** | US-012, US-013, US-014 |
| **Won't Have (now)** | Reportes avanzados, analytics predictivo |
