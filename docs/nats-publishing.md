# NATS Publishing

## Sprint 2026: outbound events

Mana-hub es el System of Record y publica hechos confirmados después de que un
caso de uso o un proceso interno persiste el cambio. En esta primera versión,
Hub no consume comandos NATS.

```text
Murmur / CRM UI
  -> nurse.episode.resolve_requested.v1 (acción)
  -> event-bridge
  -> mana-hub persiste RESOLVED
  -> DomainEvent: EpisodeResolved
  -> hub.episode.v1.{bedId} (hecho confirmado)
  -> Murmur / Cox / CRM UI
```

## Primera versión

El bean `NatsDomainEventPublisher` escucha eventos Spring seleccionados y
publica `EpisodeResolved` dentro del envelope compartido de `com.manahive:contracts`.
El publisher está activo solo cuando `nats.enabled=true`.

Subject actual:

```text
hub.episode.v1.{bedId}
```

El payload conserva `episodeId`, `residentId`, `bedId` y `actorId`. El
`eventId` del dominio es la clave de correlación y deduplicación.

## Responsabilidades

- Murmur y CRM UI expresan acciones del operador.
- event-bridge convierte y enruta acciones hacia Hub.
- mana-hub valida, persiste y publica el hecho confirmado.
- Cox puede grabar el hecho mientras la WorkbenchSession permanece abierta.

## Evolución prevista

La publicación directa es el primer incremento. Para producción distribuida se
debe reemplazar por un outbox transaccional: el cambio y el evento se guardan en
la misma transacción, y un dispatcher reintenta la entrega a NATS.

La dirección `NATS -> mana-hub` queda fuera de este sprint y se diseñará como
comandos explícitos, separados de los hechos `hub.*`.
