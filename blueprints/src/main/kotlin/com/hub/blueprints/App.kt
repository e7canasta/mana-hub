package com.hub.blueprints

import com.hub.blueprints.scenarios.*

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  MANA-HUB BLUEPRINTS
 * ══════════════════════════════════════════════════════════════════════════════
 *
 *  Real-world scenarios that exercise the mana-hub SDK.
 *  Each scenario validates a complete domain flow.
 *
 *  Canonical chain:
 *    PERCEPTION → SCENE CHANGE → EPISODE → CLINICAL HISTORY
 *
 *  Run all:
 *    ./gradlew :blueprints:run
 *
 *  Run one:
 *    ./gradlew :blueprints:run --main-class com.hub.blueprints.scenarios.ResidentOnboarding
 */
fun main() {
    println("╔══════════════════════════════════════════════════════════════╗")
    println("║  MANA-HUB BLUEPRINTS                                       ║")
    println("║  SDK Scenarios — Domain Specific Language                  ║")
    println("╚══════════════════════════════════════════════════════════════╝")
    println()

    runScenario("Resident Onboarding") { ResidentOnboarding.main() }
    runScenario("Perception Ingestion") { PercepcionIngestion.main() }
    runScenario("Scene Change Flow") { CambioDeEscenaFlow.main() }
    runScenario("Episode Lifecycle") { EpisodioLifecycle.main() }
    runScenario("Care Round") { RondaDeCuidados.main() }
    runScenario("Night Shift") { NocturnoTurno.main() }
    runScenario("Finding Registration") { FindingRegistration.main() }

    println("╔══════════════════════════════════════════════════════════════╗")
    println("║  BLUEPRINTS COMPLETADOS                                     ║")
    println("╚══════════════════════════════════════════════════════════════╝")
}

private fun runScenario(name: String, block: () -> Unit) {
    println("═══════════════════════════════════════════════════════════")
    println("  SCENARIO: $name")
    println("═══════════════════════════════════════════════════════════")
    try {
        block()
    } catch (e: Exception) {
        println("  ✗ Error: ${e.message}")
        e.printStackTrace()
    }
    println()
}
