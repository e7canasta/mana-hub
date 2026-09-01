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
    private sealed interface Command {
        data object UseSystem : Command
        data class UseManual(val startAt: Instant) : Command
        data class Advance(val duration: Duration) : Command
        data object Unknown : Command
    }

    @GetMapping
    fun show(): Map<String, Any> = mapOf(
        "now" to clock.now().toString(),
        "mode" to if (clock.isManual) "manual" else "system",
    )

    @PostMapping
    fun control(@RequestBody body: Map<String, Any>): ResponseEntity<Map<String, Any>> =
        when (val cmd = parseCommand(body)) {
            is Command.UseManual -> {
                clock.useManual(cmd.startAt)
                log.info("Reloj del Hub en manual desde {}", cmd.startAt)
                ResponseEntity.ok(show())
            }

            is Command.Advance -> {
                runCatching { clock.advance(cmd.duration) }
                    .fold(
                        onSuccess = {
                            log.info("Reloj del Hub adelantado {} → {}", cmd.duration, it)
                            ResponseEntity.ok(show())
                        },
                        /* En modo sistema esto es un error y no un no-op: un
                         * escenario que cree que adelanto el tiempo y no lo hizo
                         * produce datos que parecen validos y no lo son. */
                        onFailure = { ResponseEntity.badRequest().body(mapOf("error" to (it.message ?: "no se pudo adelantar"))) },
                    )
            }

            is Command.UseSystem -> {
                clock.useSystem()
                log.info("Reloj del Hub de vuelta en modo sistema")
                ResponseEntity.ok(show())
            }

            is Command.Unknown -> ResponseEntity.badRequest()
                .body(mapOf("error" to "action desconocida: ${cmd.label ?: "(ausente)"} — useManual | advance | useSystem"))
        }

    private fun parseCommand(body: Map<String, Any>): Command {
        val action = body["action"] as? String
        return when (action) {
            "useManual" -> {
                val at = (body["startAt"] as? String)?.let { runCatching { Instant.parse(it) }.getOrNull() }
                    ?: return Command.Unknown
                Command.UseManual(at)
            }
            "advance" -> {
                val d = (body["duration"] as? String)?.let { runCatching { Duration.parse(it) }.getOrNull() }
                    ?: return Command.Unknown
                Command.Advance(d)
            }
            "useSystem" -> Command.UseSystem
            else -> Command.Unknown
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TimeController::class.java)
    }
}
