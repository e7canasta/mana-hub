# Políticas de Monitoreo — Especificación de Producto

> **Para:** Director Médico, Jefa de Enfermería, Equipo Clínico
> **Versión:** POC — Primeros 14 días
> **Módulo:** mana-hub / policy + mana-hive / engines

---

## Qué es esto

Las Políticas de Monitoreo son la **configuración de vigilancia** de cada residente. Definen **qué se vigila, con qué intensidad, y qué pasa cuando algo detecta**.

No es un tablero de alarma. Es el sistema que dice: *"A Juan lo vigilo con nivel alto porque tuvo dos caídas este mes; a María, con nivel medio porque se levanta de noche pero vuelve sola"*.

**Lo que lo hace distinto:** Cada regla se puede **prender o apagar** individualmente por residente. Si la cámara no cubre bien el baño de Juan, se apaga la regla de baño. Si María tiene una silla de ruedas, las reglas de pie se adaptan. El director define qué quiere vigilar para cada persona, y el sistema respeta esa decisión.

---

## Cómo funciona (la idea en una frase)

```
Director configura → reglas se compilan → motores vigilan → episodios se abren → personal actúa
```

1. **El director** elige un nivel de vigilancia y ajusta las reglas que quiera.
2. **El sistema compila** esas reglas en instrucciones que los motores entienden.
3. **Los motores** miran lo que la cámara ve y comparan contra las reglas.
4. **Si algo cruza una línea**, se abre un episodio: aviso al personal.
5. **El personal resuelve** y cierra el episodio.

---

## Los niveles de vigilancia: las cuatro plantillas

Cada residente empieza con uno de cuatro **niveles**. El nivel es la "configuración base" — un paquete de reglas que ya viene armado.

### Los cuatro niveles

| Nivel | Para quién | Qué hace |
|-------|-----------|----------|
| **Monitoreo General** | Residente estable, sin factores de riesgo | Solo observa. No avisa por nada. Registra lo que pasa pero no genera alertas. |
| **Vigilia Nocturna** | Tiende a levantarse de noche | Avisa si se queda mucho tiempo sentado, al borde de la cama, parado o en el baño de noche. |
| **Riesgo de Caída** | Historial de caídas, usa andador | Avisa más rápido. El borde de la cama tiene 2 minutos, no 5. Las salidas se registran con video. |
| **Crítico** | Riesgo muy alto, vigilancia intensiva | Todo es urgente. Cada movimiento avisa. Solo se cierra si alguien fue Y el residente está seguro. |

### Qué ve el director al elegir

| Nivel | Qué significa en palabras del día a día |
|-------|----------------------------------------|
| **Bajo** | "Solo lo imprescindible. La caída siempre avisa." |
| **Medio** | "Suma las transiciones que anteceden a la mayoría de las caídas." |
| **Alto** | "Avisa antes de cada transición, de día y de noche." |

> **Nota:** El nivel "Crítico" no aparece en el selector de riesgo del director. Se accede cuando se necesita vigilancia extrema, generalmente por recomendación del sistema.

---

## El mapa de vigilancia: qué estados conoce el sistema

El sistema ve al residente como una persona que pasa por **estados** durante la noche:

```
Acostado → Incorporado → Borde de cama → De pie → [Baño | Habitación | Ausente]
                                                    ↓
                                                  En el piso
```

Cada estado tiene **reglas** que definen cuánto tiempo puede estar ahí antes de que algo pase.

### Los estados

| Estado | Qué significa | Cuándo importa |
|--------|---------------|----------------|
| **Acostado** | En la cama, posición normal | Es el estado "seguro". Las reglas de retorno lo usan como referencia. |
| **Incorporado** | Sentado en la cama | Puede ser normal (se despertó) o preocupante (no vuelve a acostarse). |
| **Borde de cama** | Intentando levantarse | Momento de mayor riesgo de caída antes de estar de pie. |
| **De pie** | Fuera de la cama, de pie | Puede deambular, ir al baño, o estar desorientado. |
| **En el baño** | Dentro del baño | Más riesgo de caída por superficies mojadas. |
| **Ausente** | No está en la habitación | Puede estar en el pasillo, en otro lado. Riesgo de perderse. |
| **En el piso** | Cayó | Emergencia. Siempre avisa. |

### Las reglas por estado

Cada estado tiene hasta **tres tiempos**:

| Tiempo | Qué significa | Qué pasa cuando se cumple |
|--------|---------------|---------------------------|
| **Preaviso** | "Lleva X minutos ahí" | El sistema lo nota, pero no avisa. Es una señal silenciosa. |
| **Alerta a los** | "Lleva X minutos, hay que mirar" | Se abre un episodio. El personal se entera. |
| **Escalada** | "Siguió ahí, es más urgente" | La alerta sube de nivel. Si estaba en aviso, pasa a emergencia. |

### Ejemplo: qué pasa cuando Juan se sienta al borde de la cama

Con nivel **Vigilia Nocturna**:
- **Minuto 3:** Preaviso silencioso (el sistema lo registra).
- **Minuto 5:** Se abre episodio. "Juan lleva 5 minutos al borde de la cama."
- **Minuto 10:** Escalada si no se resolvió.

Con nivel **Riesgo de Caída**:
- **Minuto 1:** Preaviso silencioso.
- **Minuto 2:** Se abre episodio. "Juan lleva 2 minutos al borde de la cama."
- **Minuto 3:** Escalada.

---

## Las reglas de retorno: "si sale, que vuelva"

Además de cuánto puede estar en cada estado, el sistemavigila si el residente **vuelve a la cama** después de levantarse.

| Regla | Qué significa | Ejemplo |
|-------|---------------|---------|
| **Vuelve a la cama** | Si el residente se levantó y no volvió en el plazo, avisa | "María se levantó hace 8 minutos y no volvió a acostarse. Avisa a los 10." |

Esto es especialmente importante de noche: el residente se levanta, camina, y no vuelve. Puede estar desorientado, en el baño, o en el piso.

---

## Los tiempos de confirmación: "no alertar por falsas"

El sistema no reacciona ante cada frame de video. Cuando detecta un cambio de estado (por ejemplo, de "acostado" a "de pie"), **espera un tiempo** para confirmar que el cambio es real.

| Tiempo de confirmación | Qué significa |
|------------------------|---------------|
| **1–3 segundos** | Confirmación rápida. El cambio es casi seguro. |
| **Más largo** | Confirmación lenta. El sistema es más cauteloso. |

¿Por qué importa? Porque a veces el residente se mueve en la cama y la cámara interpreta que se sentó. La confirmación evita falsas alarmas.

---

## Lo que el director puede ajustar por residente

### Nivel de vigilancia

Elige uno de los cuatro niveles. Esto cambia todas las reglas de una vez.

### Ayuda para movilidad

| Opción | Qué significa | Qué cambia |
|--------|---------------|------------|
| **Sin apoyo** | Camina solo | Las reglas estándar aplican |
| **Andador** | Usa andador | Algunas reglas se ajustan (el andador cambia la dinámica) |
| **Silla de ruedas** | Usa silla de ruedas | Las reglas de pie/borde se adaptan al uso de silla |

### Autopilot

| Estado | Qué significa |
|--------|---------------|
| **Apagado** (default) | El sistema propone cambios, el director decide |
| **Prendido** | El sistema aplica las recomendaciones sin esperar confirmación |

> **Por qué existe el autopilot:** Para residentes donde el patrón es claro y el director confía en que el sistema ajuste solo. Siempre queda registro de qué cambió y por qué.

### Ajustes individuales por regla

El director puede modificar **cada regla por separado**. Por ejemplo:

- "La regla de baño no aplica para María porque la cámara no cubre bien ese ángulo."
- "Para Juan, el borde de la cama necesita más tiempo porque es lento para sentarse."

---

## Las reglas: qué se puede configurar individualmente

### Reglas de permanencia (cuánto puede estar en un estado)

| Regla | Se puede apagar | Se puede ajustar tiempo | Se puede cambiar gravedad | Se puede cambiar cómo cierra |
|-------|-----------------|------------------------|--------------------------|------------------------------|
| **Acostado** | Sí (preaviso y alerta) | Sí | Sí | Sí |
| **Incorporado** | Sí | Sí (preaviso: 1–180 min, alerta: 1–240 min) | Sí | Sí |
| **Borde de cama** | Sí | Sí | Sí | Sí |
| **De pie** | Sí | Sí | Sí | Sí |
| **En el baño** | Sí | Sí | Sí | Sí |
| **Ausente** | Sí | Sí | Sí | Sí |
| **En el piso** | No (siempre activo) | No | No (siempre CRITICAL) | No |

### Regla de retorno (vuelve a la cama)

| Regla | Se puede apagar | Se puede ajustar tiempo | Se puede cambiar gravedad |
|-------|-----------------|------------------------|--------------------------|
| **Vuelve a la cama** | Sí | Sí (preaviso: 1–180 min, alerta: 1–240 min) | Sí |

### Reglas de confirmación de transición (cuánto esperar para confirmar un cambio)

| Transición | Se puede apagar | Se puede ajustar confirmación |
|------------|-----------------|------------------------------|
| **Acostado → Borde** | Sí | Sí (en milisegundos) |
| **Acostado → De pie** | Sí | Sí |
| **Borde → De pie** | Sí | Sí |
| **De pie → Baño** | Sí | Sí |
| **→ En el piso** | No (siempre activo) | No |

---

## La gravedad: qué tan serio es

Cada regla tiene un nivel de **gravedad** que define qué tan urgente es la alerta:

| Gravedad | Qué significa | Quién se entera | Qué espera el sistema |
|----------|---------------|-----------------|----------------------|
| **Solo registrar** | Queda en el registro, no avisa a nadie | Nadie (queda en historial) | Que quede constancia |
| **Avisar al turno** | Se entera el personal de turno | Enfermero de turno | Que lo sepa, no hace falta ir ahora |
| **Avisar y que vayan** | El turno se entera y alguien tiene que ir | Enfermero + alguien en camino | Que alguien vaya, sin urgencia extrema |
| **Emergencia** | Se enteran todos, ya | Todos, ahora | Que alguien vaya inmediatamente |

---

## Cómo se cierra un episodio

Cuando se abre un episodio (porque una regla se activó), el sistema define **cuándo se puede cerrar**:

| Condición de cierre | Qué significa |
|---------------------|---------------|
| **Cuando vuelve a estar seguro** | El residente volvió a la cama (acostado). No necesita que alguien vaya. |
| **Cuando alguien fue o está seguro** | Cualquiera de las dos: que el personal fue, O que el residente volvió solo. |
| **Cuando alguien fue y está seguro** | Las dos cosas: alguien fue Y el residente está seguro. Más estricto. |

> **Ejemplo:** Con nivel Crítico, la condición es "alguien fue y está seguro". No basta con que el residente vuelva solo: el personal tiene que haber ido.

---

## Las recomendaciones: qué sugiere el sistema

El sistema puede **proponer cambios** en la configuración. No los apaga directamente (a menos que el autopilot esté prendido).

### Qué tipo de cambios propone

| Tipo de cambio | Qué significa | Ejemplo |
|----------------|---------------|---------|
| **Cambio de nivel** | Subir o bajar el nivel de vigilancia | "María tuvo 3 episodios esta semana. Subir de Vigilia Nocturna a Riesgo de Caída." |
| **Ajuste de regla** | Modificar el tiempo de una regla específica | "Juan tarda más en sentarse. Subir el preaviso de borde de 1 a 3 minutos." |

### Qué ve el director cuando hay una recomendación

El sistema muestra una tarjeta con:

1. **El hallazgo:** "Patrón de salidas nocturnas aumentado"
2. **La narrativa:** Escrita para el director, no para un técnico
3. **La propuesta:** "Subir de Vigilia Nocturna a Riesgo de Caída"
4. **La decisión:** "Aplicar el cambio" o "No hacerlo"

> "Al aplicarlo queda registrado este hallazgo como el motivo del cambio, con los episodios que lo originaron."

---

## El historial: qué cambió, quién, y por qué

Cada cambio queda registrado con:

| Campo | Qué guarda |
|-------|-----------|
| **Motivo** | Por qué se hizo el cambio (obligatorio) |
| **Quién** | Quién lo hizo |
| **Cuándo** | Fecha y hora |
| **Huella** | Dos residentes con la misma huella se vigilan exactamente igual |

El historial se ve en la pantalla del residente, debajo del editor.

---

## El piso: cómo se ve todo junto

### Por residente (la ficha)

La pantalla del residente tiene 7 pestañas. Las relevantes son:

| Pestaña | Qué muestra |
|---------|-------------|
| **Resumen** | Estado actual, nivel de alarma, factores de riesgo, autopilot |
| **Presets** | El editor completo de reglas + explicación de por qué está así + historial |
| **Perfil** | Datos del residente (nombre, fecha de nacimiento) + nivel de alarma (solo lectura) |

### Por instalación (la vista del piso)

La pantalla de alarmas muestra una tabla con todos los residentes:

| Columna | Qué muestra |
|---------|-------------|
| **Residente** | Nombre y cama |
| **Nivel** | Bajo / Medio / Alto |
| **Día/Noche** | Cuántas reglas activas por turno |
| **Estado** | "Autopilot" / "Recomendación sin decidir" / "Editado por [nombre]" |

Tres pestañas filtran:
- **Requieren acción:** Residentes con recomendación pendiente
- **Todos:** Todos los residentes
- **Autopilot:** Residentes con autopilot prendido

---

## Las pantallas de configuración

### El tablero de reglas

Las reglas se organizan en **5 grupos** con pictogramas:

| Grupo | Qué cubre | Ejemplo de regla |
|-------|-----------|------------------|
| **En la cama** | Antes de que se levante | Incorporado, Borde de cama |
| **Fuera de la cama** | Después de levantarse | De pie |
| **Donde quedó** | Ubicación | Baño, Habitación, Ausente |
| **En el piso** | Emergencia | Caída (siempre activa) |
| **Silla y accesorios** | Equipo | Silla de ruedas, andador |

Cada regla es un **mosaico** de 88px con:
- Un pictograma que la identifica
- El tiempo configurado (ej: "5→10m")
- Un indicador de si fue ajustada manualmente
- La gravedad actual

### El editor de regla

Cuando el director toca un mosaico, se abre el detalle:

**Para reglas de permanencia y retorno:**
- Toggle: "Esta regla avisa" / "Esta regla no avisa"
- Preaviso: stepper de 1–180 minutos
- Alerta: stepper de 1–240 minutos
- Gravedad: segmentado (Solo registrar / Avisar al turno / Avisar y que vayan / Emergencia)
- Cierra: segmentado (Cuando vuelve seguro / Cuando fue o seguro / Cuando fue y seguro)

**Para reglas de transición:**
- Toggle: mismo que arriba
- Confirmación: stepper en milisegundos
- Gravedad y Cierra: mismas opciones

**Validación:** El preaviso siempre tiene que ser menor que la alerta. Si no, el sistema muestra un error: "El preaviso tiene que ser menor que el plazo de alerta. El motor rechaza el perfil entero si no lo es."

---

## Lo que el sistema NO hace (y por qué)

| No hace | Por qué |
|---------|---------|
| No vigila solo | Necesita una cámara y un perfil configurado. Sin configuración, no monitorea. |
| No decide el nivel | El director elige. El sistema puede sugerir, pero no cambia solo (excepto con autopilot). |
| No reemplaza rondas | Las complementa. Las rondas son presencia humana; esto es vigilancia continua. |
| No manda alertas a las 3am | Las alertas van al panel. El equipo las revisa cuando puede. Las emergencias son las únicas que pueden interrumpir. |
| No cierra solo (siempre) | Si la condición es "alguien fue y está seguro", necesita que alguien fue. No asume. |
| No confirma transiciones instantáneamente | Siempre espera un tiempo para evitar falsas alarmas. |

---

## Resumen: qué controla el director

| Decisión | Dónde la toma | Qué impacta |
|----------|---------------|-------------|
| Nivel de vigilancia | Pestaña Presets, selector de nivel | Todas las reglas de una vez |
| Ayuda para movilidad | Pestaña Presets, selector de apoyo | Filtra reglas según el dispositivo |
| Autopilot | Pestaña Presets, toggle | Si el sistema aplica sugerencias solo |
| Regla individual | Tablero de reglas → tocar mosaico | Solo esa regla (tiempo, gravedad, cierre) |
| Apagar una regla | Editor de regla → toggle off | Esa regla deja de vigilar |
| Motivo del cambio | Campo "Motivo del cambio" (obligatorio) | Registro de auditoría |

---

## Glosario para el equipo

| Término | Qué significa en lenguaje del día a día |
|---------|----------------------------------------|
| **Nivel de vigilancia** | Qué tan intenso se vigila a este residente |
| **Regla** | Una condición que el sistema vigila (ej: "si está sentado más de 10 minutos") |
| **Preaviso** | "Lleva X minutos ahí" — el sistema lo nota pero no avisa |
| **Alerta** | "Lleva X minutos, hay que mirar" — se abre un episodio |
| **Escalada** | "Siguió ahí, es más urgente" — la alerta sube de nivel |
| **Episodio** | Algo que requirió atención (caída, deambulación, etc.) |
| **Confirmación** | Cuánto espera el sistema para creer que el cambio es real |
| **Retorno** | Si el residente se levantó, si vuelve a la cama en el plazo |
| **Gravedad** | Qué tan serio es: desde "solo registrar" hasta "emergencia" |
| **Cierre** | Qué tiene que pasar para que el episodio se cierre |
| **Autopilot** | Si el sistema aplica cambios sin esperar que el director confirme |
| **Huella** | Identificador único de una configuración: misma huella = misma vigilancia |
| **Motivo** | Por qué se hizo un cambio (obligatorio en cada guardado) |
| **DAG** | El mapa de estados por los que pasa el residente (acostado → sentado → de pie → etc.) |

---

# Anexo: El Gemelo Digital y los Scene Events

> **Este anexo explica qué ve el sistema 24/7, cómo lo process, y qué queda registrado.**

---

## Qué es el gemelo digital

El **gemelo digital** es una réplica en tiempo real de lo que pasa en la habitación de cada residente. No es una grabación: es un modelo que el sistema actualiza constantemente con lo que la cámara observa.

Cada cama tiene su propio gemelo. Mientras la cámara funciona, el gemelo sabe:

- **Quién está en la cama** (el residente asignado)
- **En qué posición está** (acostado, sentado, de pie, en el piso)
- **Dónde está** (en la cama, en el baño, en el pasillo, ausente)
- **Si hay personal en la habitación** (y si está al alcance)
- **Dónde está el equipamiento** (andador, silla de ruedas, barandales)
- **Si la cámara está funcionando** (señal activa o perdida)

Todo esto se actualiza **cada vez que la cámara envía una observación** y **cada 30 segundos** aunque no llegue nada nuevo.

---

## Qué ve la cámara: las 13 posiciones del cuerpo

El sistema reconoce **13 estados** en los que el residente puede estar. Son mutuamente excluyentes: en cada momento, el residente está en exactamente uno.

### En la cama (4 estados)

| Estado | Qué ve la cámara | Cuándo importa |
|--------|------------------|----------------|
| **Acostado** | Posición horizontal, reclinado | Es el estado "seguro". De aquí salen las alertas de retorno. |
| **Incorporado** | Sentado en la cama, espalda erguida | Puede ser normal (se despertó) o preocupante (no vuelve). |
| **Intentando salir** | Brazos/rostro al borde, movimiento de "gusanito" | Preaviso antes de llegar al borde. |
| **Borde de cama** | Sentado en el borde, pies colgando | Momento de mayor riesgo antes de estar de pie. |

### Fuera de la cama (6 estados)

| Estado | Qué ve la cámara | Cuándo importa |
|--------|------------------|----------------|
| **De pie** | Erguido, posición no especificada | Puede deambular, ir al baño, o estar desorientado. |
| **En el baño** | Dentro del área del baño | Riesgo por superficies mojadas. |
| **En la habitación** | En la habitación pero no en cama/baño | Puede estar caminando o sentado en otro lado. |
| **En el pasillo** | En el corredor | Fuera de su habitación. |
| **Afuera** | Fuera del edificio | Situación de riesgo. |
| **Ausente** | No está en la habitación, ubicación indeterminada | Puede estar en cualquier lado. |

### Mobiliario (3 estados)

| Estado | Qué ve la cámara | Cuándo importa |
|--------|------------------|----------------|
| **En silla** | Sentado en una silla común | Posición alternativa a la cama. |
| **En silla de ruedas** | Sentado en silla de ruedas | El andador o la silla cambian la dinámica de movimiento. |
| **En el piso** | En el suelo | **Emergencia.** Siempre avisa. Es el resultado que el sistema existe para detectar. |

### Desconocido (1 estado con causa)

| Estado | Qué significa | Qué pasa |
|--------|---------------|----------|
| **Desconocido (señal perdida)** | La cámara dejó de enviar datos | El sistema no sabe qué pasa. Se registra como "desconocido". |
| **Desconocido (escena confusa)** | La cámara no puede determinar la posición | Similar: el sistema prefiere no saber a asumir mal. |

---

## Qué mide alrededor del residente: el estado de la escena

Además de la posición del cuerpo, el sistema vigila **5 cosas independientes** que están alrededor del residente:

| Campo | Qué mide | Estados posibles |
|-------|----------|------------------|
| **Personal presente** | Si hay enfermero/cuidador en la habitación | Desconocido / No hay / Sí hay / Al alcance |
| **Silla de ruedas** | Dónde está la silla de ruedas | Desconocido / No está / Está / Al alcance |
| **Andador** | Dónde está el andador | Desconocido / No está / Está / Al alcance |
| **Barandal izquierdo** | Posición del barandal izquierdo | Desconocido / Bajado / Subido / Con cubierta |
| **Barandal derecho** | Posición del barandal derecho | Desconocido / Bajado / Subido / Con cubierta |

> **Por qué son independientes:** Porque el barandal puede estar bajado mientras la silla de ruedas está en su lugar. Cada campo tiene su propio reloj: si el barandal baja a las 3:00 y la silla se mueve a las 3:10, el reloj del barandal NO se resetea.

> **Todos empiezan en "Desconocido":** Sin datos de sensores, el sistema no asume que los barandales están bajados o que no hay personal. Si asumiera, generaría falsas alarmas al iniciar.

---

## Las transiciones: cómo cambia el estado

El sistema no solo mira dónde está el residente: mira **cómo pasó de un estado a otro**. Cada cambio tiene un **tiempo de confirmación** (hysteresis) para evitar falsas alarmas.

### Las transiciones más importantes

| De | A | Confirmación | Qué significa |
|----|---|-------------|---------------|
| Acostado → Incorporado | 1.0–1.5 seg | Se incorporó en la cama |
| Acostado → Borde | 1.5 seg | Pasó de acostado al borde directo |
| Acostado → De pie | 1.5 seg | Se levantó de la cama |
| Borde → De pie | 1.2 seg | Se paró desde el borde |
| De pie → Baño | 2.0 seg | Fue al baño |
| De pie → Ausente | 2.0 seg | Salió de la habitación |
| Cualquier estado → En el piso | **0.8 seg** | **Cayó.** Confirmación más rápida: 800 milisegundos. |

### Por qué la caída tiene la confirmación más corta

Porque una caída es una emergencia. El sistema no puede esperar 3 segundos para confirmar que alguien cayó. 800 milisegundos es suficiente para distinguir una caída de un movimiento normal.

### Transiciones ilegales

Algunos cambios **no pueden pasar directamente**. Por ejemplo, de "acostado" a "en el baño" no es legal: tiene que pasar por "de pie" primero. Si la cámara reporta ese salto, el sistema lo descarta como error de detección.

---

## Los 14 eventos de escena: todo lo que el sistema reporta

Cada vez que algo cambia, el sistema genera un **evento de escena** (Scene Event). Son 14 tipos organizados en 5 categorías:

### Hechos de la persona (4 tipos)

| Evento | Qué reporta | Ejemplo |
|--------|-------------|---------|
| **Noche abierta** | Inicio del turno nocturno, quién está en la cama | "Noche iniciada. Residente: María. Estado inicial: acostada." |
| **Transición detectada** | Cambio de posición del cuerpo | "María pasó de acostada a incorporada a las 23:15." |
| **Preaviso de permanencia** | Lleva tiempo en un estado, se aproxima al límite | "María lleva 8 minutos incorporada. Preaviso: el límite es 10." |
| **Permanencia excedida** | Se pasó del tiempo máximo en un estado | "María lleva 12 minutos incorporada. Límite: 10 minutos." |

### Hechos del entorno (3 tipos)

| Evento | Qué reporta | Ejemplo |
|--------|-------------|---------|
| **Cambio en la escena** | Algo alrededor del residente cambió | "El barandal izquierdo se bajó a las 02:30." |
| **Preaviso de permanencia del entorno** | Un elemento del entorno lleva tiempo en un estado | "El personal lleva 10 minutos en la habitación." |
| **Permanencia del entorno excedida** | El elemento del entorno se pasó del límite | "El personal lleva 30 minutos. Límite configurado." |

### Hechos del personal (2 tipos)

| Evento | Qué reporta | Ejemplo |
|--------|-------------|---------|
| **Personal detectado** | Entró un enfermero/cuidador | "Personal entró a la habitación a las 03:10." |
| **Personal se fue** | El enfermero/cuidador salió | "Personal salió de la habitación a las 03:25." |

### Hechos de señal (2 tipos)

| Evento | Qué reporta | Ejemplo |
|--------|-------------|---------|
| **Señal perdida** | La cámara dejó de funcionar | "Señal de la cámara perdida desde las 03:40." |
| **Señal recuperada** | La cámara volvió a funcionar | "Señal recuperada a las 03:45." |

### Hechos de retorno (2 tipos)

| Evento | Qué reporta | Ejemplo |
|--------|-------------|---------|
| **Preaviso de retorno** | El residente lleva tiempo fuera de la cama y se aproxima al límite | "María salió de la cama hace 7 minutos. Límite de retorno: 10." |
| **Retorno excedido** | El residente no volvió en el plazo | "María salió de la cama hace 12 minutos y no volvió." |

### Hecho de cierre (1 tipo)

| Evento | Qué reporta | Ejemplo |
|--------|-------------|---------|
| **Noche cerrada** | Resumen del turno nocturno | "Noche cerrada. 14 transiciones. 23 minutos en desconocido. 1 episodio." |

---

## La instantánea digital: el estado completo en cada momento

Cada vez que se genera un evento de escena, el sistema adjunta una **instantánea** (snapshot) con el estado completo del gemelo digital en ese momento:

| Campo | Qué guarda |
|-------|-----------|
| **Posición** | En qué estado estaba el residente |
| **Desde cuándo** | Cuánto tiempo lleva en ese estado |
| **Escena** | Estado de los 5 campos del entorno (personal, equipamiento, barandales) |
| **Desde cuándo la escena** | Cuándo cambió por última vez cualquier campo de la escena |
| **Señal** | Si la cámara está funcionando |
| **Último latido** | Cuándo fue el último heartbeat de la cámara |

> **Por qué se guarda:** Porque cuando el director pregunta "¿qué pasó a las 3:15?", el sistema puede mostrar exactamente qué veía la cámara en ese momento: posición, entorno, señal, todo.

---

## El ciclo continuo: qué pasa cada 30 segundos

El sistema no espera a que llegue una observación de la cámara para trabajar. Cada 30 segundos ejecuta un **ciclo de barrido** que revisa todo:

### Paso 1: Barrido de permanencia

Revisa a cada residente: ¿lleva demasiado tiempo en su estado actual?

| Si el residente está... | Y lleva... | Pasa... |
|-------------------------|-----------|---------|
| Incorporado 8 min (límite: 10) | 80% del tiempo | **Preaviso** silencioso |
| Incorporado 12 min (límite: 10) | 100%+ del tiempo | **Permanencia excedida** → se abre episodio |
| De pie 2 min (límite: 3) | 80% del tiempo | **Preaviso** silencioso |
| De pie 4 min (límite: 3) | 100%+ del tiempo | **Permanencia excedida** → se abre episodio |

### Paso 2: Barrido de retorno

Revisa: ¿el residente salió de la cama y no volvió?

| Si el residente... | Y lleva... | Pasa... |
|--------------------|-----------|---------|
| Salió de la cama hace 7 min (límite: 10) | 80% del tiempo | **Preaviso de retorno** |
| Salió de la cama hace 12 min (límite: 10) | 100%+ del tiempo | **Retorno excedido** → se abre episodio |

### Paso 3: Barrido de señal

Revisa: ¿la cámara está funcionando?

| Si la cámara... | Y lleva... | Pasa... |
|-----------------|-----------|---------|
| No da señal desde hace 90 seg | 90 segundos | **Señal perdida** |
| Acaba de recuperar señal | - | **Señal recuperada** |

### Paso 4: Barrido del entorno

Revisa cada uno de los 5 campos de la escena: ¿lleva mucho tiempo en el mismo estado?

| Si el barandal... | Y lleva... | Pasa... |
|-------------------|-----------|---------|
| Bajado desde hace 20 min (límite: 15) | 100%+ del tiempo | **Permanencia del entorno excedida** |

### Por qué el barrido es importante

Porque la cámara puede dejar de enviar datos. Si el residente se levantó a las 3:00 y la cámara perdió señal a las 3:05, sin el barrido el sistema no sabría que algo pasó. El barrido es el "patrullero del silencio": funciona aunque no lleguen observaciones.

---

## La evidencia: qué queda registrado

Cada vez que se abre un episodio o detecta algo importante, el sistema genera **evidencia**:

### Qué se graba

| Tipo de evidencia | Qué contiene | Cuándo se genera |
|-------------------|--------------|------------------|
| **Incidente** | Video alrededor del evento (antes y después) | Al abrir un episodio (por permanencia excedida, retorno, caída) |
| **Clipe** | Fragmento de video recortado del incidente | Al cerrar el episodio |
| **Línea de tiempo** | Secuencia de eventos antes y después del ancla | Alrededor de cada episodio importante |

### Ventanas de grabación

| Evento | Grabación antes | Grabación después | Calidad |
|--------|----------------|-------------------|---------|
| Transición a de pie | 2 minutos | 5 minutos | HD (1280x720) |
| Permanencia excedida | 0 | 5 minutos | HD |
| Caída | 5 minutos | 10 minutos | **FULL (1920x1080)** |
| Episodio abierto | 0 | Duración del episodio | Según gravedad |

> **La caída se graba en la máxima calidad:** Porque es la emergencia que el sistema existe para detectar. 1920x1080 a 30fps, 5 minutos antes y 10 después.

### Dónde queda la evidencia

La evidencia se persiste en la base de datos de mana-hub:

| Tabla | Qué guarda |
|-------|-----------|
| **evidence** | Cada registro de evidencia (tipo, categoría, momento, regla que lo generó) |
| **timelines** | Ventanas de eventos antes/después de un evento ancla |
| **clip_windows** | Ventanas de tiempo para extraer clips de video |
| **scene_events** | Todos los eventos de escena con su instantánea digital completa |

---

## Flujo completo: de la cámara al panel

```
CÁMARA detecta movimiento
  ↓
Observación (posición + confianza + timestamp)
  ↓
Motor de escena valida:
  ¿La confianza es suficiente?
  ¿La transición es legal?
  ¿Se sostuvo el tiempo suficiente?
  ↓
Gemelo digital se actualiza
  ↓
Evento de escena se genera (con instantánea adjunta)
  ↓
Motor sentinela evalúa contra las reglas:
  ¿Esta transición abre un episodio?
  ¿Esta permanencia excedió el límite?
  ↓
Si hay episodio → Motor de aviso notifica al personal
  ↓
Motor de grabación decide: ¿grabar video? ¿Qué calidad? ¿Cuánto antes/después?
  ↓
Evidencia se persiste en la base de datos
  ↓
El director ve en el panel:
  - La tarjeta del hallazgo
  - El video del incidente
  - La línea de tiempo completa
  - La instantánea de qué pasaba en ese momento
```

---

## Glosario del anexo

| Término | Qué significa |
|---------|---------------|
| **Gemelo digital** | Réplica en tiempo real de lo que pasa en la habitación de un residente |
| **Instantánea (snapshot)** | Estado completo del gemelo digital en un momento dado |
| **Evento de escena (Scene Event)** | Algo que el sistema detectó y reportó (transición, permanencia, señal, etc.) |
| **Observación** | Dato crudo que llega de la cámara (posición + confianza + timestamp) |
| **Barrido (sweep)** | Revisión periódica cada 30 segundos de todos los residentes activos |
| **Hysteresis** | Tiempo mínimo que un cambio tiene que sostenerse antes de creerlo |
| **Permanencia** | Cuánto tiempo lleva el residente en un estado |
| **Retorno** | Si el residente volvió a la cama después de levantarse |
| **Señal** | Estado de la cámara (funcionando / perdida) |
| **Incidente** | Registro de evidencia generado al abrir un episodio |
| **Clip** | Fragmento de video recortado de un incidente |
| **Línea de tiempo** | Secuencia cronológica de eventos alrededor de un momento clave |
