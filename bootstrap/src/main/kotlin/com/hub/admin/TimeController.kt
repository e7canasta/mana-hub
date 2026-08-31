package com.hub.admin

import com.hub.shared.time.SwitchableHubClock
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.time.Instant

/**
 * Control del reloj del Hub, con el mismo vocabulario que el de mana-hive.
 *
 * El simulador de escenarios ya pone a hive en manual y lo adelanta por NATS
 * (`useManual`, `advance`, `useSystem`). El Hub no tenia con que responder a lo
 * mismo, asi que todo lo que escribia durante una corrida caia en la hora real
 * mientras los eventos caian en la simulada.
 *
 * Los nombres son los de hive **a proposito**: el que corre un escenario tiene
 * que poder pensar en un solo reloj, aunque por debajo sean dos procesos. Un
 * tercer vocabulario para la misma idea es una forma de garantizar que alguien
 * los desincronice.
 */
@RestController
@RequestMapping("/api/v1/admin/time")
class TimeController(
    private val clock: SwitchableHubClock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun show(): Map<String, Any> = mapOf(
        "now" to clock.now().toString(),
        "mode" to if (clock.isManual) "manual" else "system",
    )

    @PostMapping
    fun control(@RequestBody body: Map<String, Any>): ResponseEntity<Map<String, Any>> =
        when (val action = body["action"] as? String) {
            "useManual" -> {
                val at = (body["startAt"] as? String)?.let { runCatching { Instant.parse(it) }.getOrNull() }
                    ?: return ResponseEntity.badRequest()
                        .body(mapOf("error" to "useManual necesita startAt en ISO-8601"))
                clock.useManual(at)
                log.info("Reloj del Hub en manual desde {}", at)
                ResponseEntity.ok(show())
            }

            "advance" -> {
                val d = (body["duration"] as? String)?.let { runCatching { Duration.parse(it) }.getOrNull() }
                    ?: return ResponseEntity.badRequest()
                        .body(mapOf("error" to "advance necesita duration en ISO-8601, p.ej. PT17M"))
                runCatching { clock.advance(d) }
                    .fold(
                        onSuccess = {
                            log.info("Reloj del Hub adelantado {} → {}", d, it)
                            ResponseEntity.ok(show())
                        },
                        /* En modo sistema esto es un error y no un no-op: un
                         * escenario que cree que adelanto el tiempo y no lo hizo
                         * produce datos que parecen validos y no lo son. */
                        onFailure = { ResponseEntity.badRequest().body(mapOf("error" to (it.message ?: "no se pudo adelantar"))) },
                    )
            }

            "useSystem" -> {
                clock.useSystem()
                log.info("Reloj del Hub de vuelta en modo sistema")
                ResponseEntity.ok(show())
            }

            else -> ResponseEntity.badRequest()
                .body(mapOf("error" to "action desconocida: ${action ?: "(ausente)"} — useManual | advance | useSystem"))
        }
}
