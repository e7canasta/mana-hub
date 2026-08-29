package com.hub.policy.domain.model

import java.time.Duration

/**
 * The four catalog levels, inspired by mana-hive's LevelCatalogs.
 * Each level defines resident state rules, come-back rules, and transitions.
 */
object DagCatalogs {

    /**
     * NIVEL 0: STANDARD — baseline, solo observación, sin alertas.
     * "No hagas nada, solo registra qué pasó"
     */
    val STANDARD = DagCatalog(
        version = "2.1.0",
        residentStates = mapOf(
            StateKind.LYING to ResidentStateRule(state = StateKind.LYING),
            StateKind.SITTING_IN_BED to ResidentStateRule(state = StateKind.SITTING_IN_BED),
            StateKind.BED_EDGE to ResidentStateRule(state = StateKind.BED_EDGE),
            StateKind.STANDING to ResidentStateRule(state = StateKind.STANDING),
            StateKind.IN_BATHROOM to ResidentStateRule(state = StateKind.IN_BATHROOM),
            StateKind.ABSENT to ResidentStateRule(state = StateKind.ABSENT),
        ),
        comeBackRules = emptyMap(),
        transitions = listOf(
            DagTransitionRule(StateKind.LYING, StateKind.SITTING_IN_BED, Duration.ofMillis(1500)),
            DagTransitionRule(StateKind.LYING, StateKind.BED_EDGE, Duration.ofMillis(1500)),
            DagTransitionRule(StateKind.LYING, StateKind.STANDING, Duration.ofMillis(2000)),
            DagTransitionRule(StateKind.SITTING_IN_BED, StateKind.LYING, Duration.ofMillis(1000)),
            DagTransitionRule(StateKind.SITTING_IN_BED, StateKind.STANDING, Duration.ofMillis(1500)),
            DagTransitionRule(StateKind.BED_EDGE, StateKind.STANDING, Duration.ofMillis(1500)),
            DagTransitionRule(StateKind.BED_EDGE, StateKind.LYING, Duration.ofMillis(1000)),
            DagTransitionRule(StateKind.STANDING, StateKind.IN_BATHROOM, Duration.ofMillis(2000)),
            DagTransitionRule(StateKind.STANDING, StateKind.IN_ROOM, Duration.ofMillis(2000)),
            DagTransitionRule(StateKind.STANDING, StateKind.ABSENT, Duration.ofMillis(3000)),
            DagTransitionRule(StateKind.IN_BATHROOM, StateKind.STANDING, Duration.ofMillis(2000)),
            DagTransitionRule(StateKind.IN_BATHROOM, StateKind.IN_ROOM, Duration.ofMillis(2000)),
            DagTransitionRule(StateKind.IN_ROOM, StateKind.LYING, Duration.ofMillis(3000)),
            DagTransitionRule(StateKind.IN_ROOM, StateKind.STANDING, Duration.ofMillis(2000)),
            DagTransitionRule(StateKind.IN_ROOM, StateKind.IN_BATHROOM, Duration.ofMillis(2000)),
        ),
    )

    /**
     * NIVEL 1: NIGHT_WANDERING — alertas básicas para nocturno.
     * "Si se despierta de noche, avísenme"
     */
    val NIGHT_WANDERING = DagCatalog(
        version = "2.1.0",
        residentStates = mapOf(
            StateKind.LYING to ResidentStateRule(state = StateKind.LYING),
            StateKind.SITTING_IN_BED to ResidentStateRule(
                state = StateKind.SITTING_IN_BED,
                warningAfter = Duration.ofMinutes(20),
                alertAfter = Duration.ofMinutes(30),
                severity = Severity.WARNING,
            ),
            StateKind.BED_EDGE to ResidentStateRule(
                state = StateKind.BED_EDGE,
                warningAfter = Duration.ofMinutes(3),
                alertAfter = Duration.ofMinutes(5),
                severity = Severity.WARNING,
            ),
            StateKind.STANDING to ResidentStateRule(
                state = StateKind.STANDING,
                warningAfter = Duration.ofMinutes(10),
                alertAfter = Duration.ofMinutes(15),
                severity = Severity.WARNING,
                closureCondition = ClosureCondition.SAFE_ONLY,
            ),
            StateKind.IN_BATHROOM to ResidentStateRule(
                state = StateKind.IN_BATHROOM,
                warningAfter = Duration.ofMinutes(15),
                alertAfter = Duration.ofMinutes(25),
                severity = Severity.WARNING,
                closureCondition = ClosureCondition.SAFE_ONLY,
            ),
            StateKind.ABSENT to ResidentStateRule(
                state = StateKind.ABSENT,
                warningAfter = Duration.ofMinutes(5),
                alertAfter = Duration.ofMinutes(10),
                severity = Severity.WARNING,
            ),
        ),
        comeBackRules = mapOf(
            StateKind.LYING to ComeBackRule(
                baseline = StateKind.LYING,
                warningAfter = Duration.ofMinutes(5),
                alertAfter = Duration.ofMinutes(10),
                severity = Severity.WARNING,
            ),
        ),
        transitions = listOf(
            DagTransitionRule(StateKind.LYING, StateKind.SITTING_IN_BED, Duration.ofMillis(1000)),
            DagTransitionRule(StateKind.LYING, StateKind.BED_EDGE, Duration.ofMillis(1000)),
            DagTransitionRule(StateKind.LYING, StateKind.STANDING, Duration.ofMillis(1000),
                recordBefore = Duration.ofMinutes(2), recordAfter = Duration.ofMinutes(5)),
            DagTransitionRule(StateKind.SITTING_IN_BED, StateKind.STANDING, Duration.ofMillis(1000)),
            DagTransitionRule(StateKind.STANDING, StateKind.IN_BATHROOM, Duration.ofMillis(1000)),
            DagTransitionRule(StateKind.STANDING, StateKind.ABSENT, Duration.ofMillis(2000)),
        ),
    )

    /**
     * NIVEL 2: FALL_RISK — alertas intensivas para riesgo de caída.
     * "Si se mueve, avísenme rápido"
     */
    val FALL_RISK = DagCatalog(
        version = "2.1.0",
        residentStates = mapOf(
            StateKind.LYING to ResidentStateRule(state = StateKind.LYING),
            StateKind.SITTING_IN_BED to ResidentStateRule(
                state = StateKind.SITTING_IN_BED,
                warningAfter = Duration.ofMinutes(15),
                alertAfter = Duration.ofMinutes(20),
                severity = Severity.WARNING,
            ),
            StateKind.BED_EDGE to ResidentStateRule(
                state = StateKind.BED_EDGE,
                warningAfter = Duration.ofMinutes(1),
                alertAfter = Duration.ofMinutes(2),
                severity = Severity.WARNING,
            ),
            StateKind.STANDING to ResidentStateRule(
                state = StateKind.STANDING,
                warningAfter = Duration.ofMinutes(2),
                alertAfter = Duration.ofMinutes(3),
                severity = Severity.WARNING,
                closureCondition = ClosureCondition.SAFE_ONLY,
            ),
            StateKind.IN_BATHROOM to ResidentStateRule(
                state = StateKind.IN_BATHROOM,
                warningAfter = Duration.ofMinutes(10),
                alertAfter = Duration.ofMinutes(15),
                severity = Severity.WARNING,
                closureCondition = ClosureCondition.SAFE_ONLY,
            ),
            StateKind.ABSENT to ResidentStateRule(
                state = StateKind.ABSENT,
                warningAfter = Duration.ofMinutes(5),
                alertAfter = Duration.ofMinutes(10),
                severity = Severity.WARNING,
            ),
        ),
        comeBackRules = mapOf(
            StateKind.LYING to ComeBackRule(
                baseline = StateKind.LYING,
                warningAfter = Duration.ofMinutes(3),
                alertAfter = Duration.ofMinutes(5),
                severity = Severity.WARNING,
            ),
        ),
        transitions = listOf(
            DagTransitionRule(StateKind.LYING, StateKind.SITTING_IN_BED, Duration.ofMillis(2000)),
            DagTransitionRule(StateKind.LYING, StateKind.STANDING, Duration.ofMillis(3000),
                recordBefore = Duration.ofMinutes(2), recordAfter = Duration.ofMinutes(5)),
            DagTransitionRule(StateKind.SITTING_IN_BED, StateKind.STANDING, Duration.ofMillis(2000)),
        ),
    )

    /**
     * NIVEL 3: CRITICAL — alertas inmediatas para residentes críticos.
     * "Alerta inmediata en cualquier movimiento"
     */
    val CRITICAL = DagCatalog(
        version = "2.1.0",
        residentStates = mapOf(
            StateKind.LYING to ResidentStateRule(state = StateKind.LYING),
            StateKind.SITTING_IN_BED to ResidentStateRule(
                state = StateKind.SITTING_IN_BED,
                warningAfter = Duration.ofMinutes(10),
                alertAfter = Duration.ofMinutes(15),
                severity = Severity.CRITICAL,
                closureCondition = ClosureCondition.STAFF_AND_SAFE,
            ),
            StateKind.BED_EDGE to ResidentStateRule(
                state = StateKind.BED_EDGE,
                warningAfter = Duration.ofMinutes(1),
                alertAfter = Duration.ofMinutes(2),
                severity = Severity.CRITICAL,
                closureCondition = ClosureCondition.STAFF_AND_SAFE,
            ),
            StateKind.STANDING to ResidentStateRule(
                state = StateKind.STANDING,
                warningAfter = Duration.ofMinutes(2),
                alertAfter = Duration.ofMinutes(3),
                severity = Severity.CRITICAL,
                closureCondition = ClosureCondition.STAFF_AND_SAFE,
            ),
            StateKind.IN_BATHROOM to ResidentStateRule(
                state = StateKind.IN_BATHROOM,
                warningAfter = Duration.ofMinutes(5),
                alertAfter = Duration.ofMinutes(10),
                severity = Severity.CRITICAL,
                closureCondition = ClosureCondition.STAFF_AND_SAFE,
            ),
            StateKind.ABSENT to ResidentStateRule(
                state = StateKind.ABSENT,
                warningAfter = Duration.ofMinutes(2),
                alertAfter = Duration.ofMinutes(5),
                severity = Severity.CRITICAL,
                closureCondition = ClosureCondition.STAFF_AND_SAFE,
            ),
        ),
        comeBackRules = mapOf(
            StateKind.LYING to ComeBackRule(
                baseline = StateKind.LYING,
                warningAfter = Duration.ofMinutes(2),
                alertAfter = Duration.ofMinutes(3),
                severity = Severity.CRITICAL,
                closureCondition = ClosureCondition.STAFF_AND_SAFE,
            ),
        ),
        transitions = listOf(
            DagTransitionRule(StateKind.LYING, StateKind.SITTING_IN_BED, Duration.ofMillis(1000)),
            DagTransitionRule(StateKind.LYING, StateKind.STANDING, Duration.ofMillis(1000),
                recordBefore = Duration.ofMinutes(5), recordAfter = Duration.ofMinutes(10)),
            DagTransitionRule(StateKind.SITTING_IN_BED, StateKind.STANDING, Duration.ofMillis(1000)),
        ),
    )

    /** Index by watch level */
    val BY_LEVEL: Map<WatchLevel, DagCatalog> = mapOf(
        WatchLevel.STANDARD to STANDARD,
        WatchLevel.NIGHT_WANDERING to NIGHT_WANDERING,
        WatchLevel.FALL_RISK to FALL_RISK,
        WatchLevel.CRITICAL to CRITICAL,
    )

    /** Get catalog for a watch level */
    fun forLevel(level: WatchLevel): DagCatalog = BY_LEVEL.getValue(level)
}
