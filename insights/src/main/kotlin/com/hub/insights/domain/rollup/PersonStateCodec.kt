package com.hub.insights.domain.rollup

import com.manahive.contracts.perception.ObservationKind
import com.manahive.contracts.scene.PersonState
import com.manahive.contracts.scene.SceneEvent
import com.manahive.contracts.scene.StateKind
import com.manahive.contracts.scene.kind
import com.manahive.contracts.scene.personStateFromKind
import com.manahive.contracts.scene.toPersonState

/**
 * Wire format de hive ([SceneEventSerializer]): `from`/`to` son el simpleName
 * de [PersonState] (`Lying`, `SittingInBed`, `InBathroom`…).
 *
 * Hub aplana eso en `fromState`/`toState`. Insights parsea al [StateKind] canónico.
 */
object PersonStateCodec {

    private val bySimpleName: Map<String, StateKind> =
        StateKind.entries.associate { personStateFromKind(it)::class.simpleName!! to it }

    fun parse(raw: String?): StateKind? {
        if (raw.isNullOrBlank() || raw.equals("null", ignoreCase = true)) return null
        val trimmed = raw.trim()
        bySimpleName[trimmed]?.let { return it }
        val enumName = trimmed.uppercase().replace('-', '_').replace(' ', '_')
        runCatching { StateKind.valueOf(enumName) }.getOrNull()?.let { return it }
        runCatching { ObservationKind.valueOf(enumName).toPersonState().kind }.getOrNull()?.let { return it }
        return null
    }

    fun person(kind: StateKind): PersonState = personStateFromKind(kind)
}

val StateKind.inBed: Boolean
    get() = when (this) {
        StateKind.LYING, StateKind.SITTING_IN_BED, StateKind.ATTEMPTING_EXIT, StateKind.BED_EDGE -> true
        StateKind.STANDING, StateKind.ON_FLOOR, StateKind.IN_BATHROOM, StateKind.IN_ROOM,
        StateKind.IN_HALLWAY, StateKind.OUTDOOR, StateKind.ABSENT,
        StateKind.IN_CHAIR, StateKind.IN_WHEELCHAIR, StateKind.UNKNOWN -> false
    }

/** AttemptingExit = gusanito en cama, no sueño. */
val StateKind.restlessInBed: Boolean
    get() = when (this) {
        StateKind.ATTEMPTING_EXIT -> true
        StateKind.LYING, StateKind.SITTING_IN_BED, StateKind.BED_EDGE,
        StateKind.STANDING, StateKind.ON_FLOOR, StateKind.IN_BATHROOM, StateKind.IN_ROOM,
        StateKind.IN_HALLWAY, StateKind.OUTDOOR, StateKind.ABSENT,
        StateKind.IN_CHAIR, StateKind.IN_WHEELCHAIR, StateKind.UNKNOWN -> false
    }

/** SittingInBed / BedEdge: en cama despierto. */
val StateKind.awakeInBed: Boolean
    get() = when (this) {
        StateKind.SITTING_IN_BED, StateKind.BED_EDGE -> true
        StateKind.LYING, StateKind.ATTEMPTING_EXIT,
        StateKind.STANDING, StateKind.ON_FLOOR, StateKind.IN_BATHROOM, StateKind.IN_ROOM,
        StateKind.IN_HALLWAY, StateKind.OUTDOOR, StateKind.ABSENT,
        StateKind.IN_CHAIR, StateKind.IN_WHEELCHAIR, StateKind.UNKNOWN -> false
    }

/** Fuera de habitación → andar. Standing / InRoom siguen adentro. */
val StateKind.outOfRoom: Boolean
    get() = when (this) {
        StateKind.IN_HALLWAY, StateKind.OUTDOOR, StateKind.ABSENT -> true
        StateKind.LYING, StateKind.SITTING_IN_BED, StateKind.ATTEMPTING_EXIT, StateKind.BED_EDGE,
        StateKind.STANDING, StateKind.ON_FLOOR, StateKind.IN_BATHROOM, StateKind.IN_ROOM,
        StateKind.IN_CHAIR, StateKind.IN_WHEELCHAIR, StateKind.UNKNOWN -> false
    }

val StateKind.outOfSight: Boolean
    get() = when (this) {
        StateKind.ABSENT, StateKind.UNKNOWN -> true
        StateKind.LYING, StateKind.SITTING_IN_BED, StateKind.ATTEMPTING_EXIT, StateKind.BED_EDGE,
        StateKind.STANDING, StateKind.ON_FLOOR, StateKind.IN_BATHROOM, StateKind.IN_ROOM,
        StateKind.IN_HALLWAY, StateKind.OUTDOOR,
        StateKind.IN_CHAIR, StateKind.IN_WHEELCHAIR -> false
    }

/**
 * Nombres de [SceneEvent] = simpleName del sealed hive.
 * Solo TransitionDetected y NightOpened cambian el FSM de persona.
 */
object SceneEventTypes {
    val TRANSITION = SceneEvent.TransitionDetected::class.simpleName!!
    val NIGHT_OPENED = SceneEvent.NightOpened::class.simpleName!!
    val NIGHT_CLOSED = SceneEvent.NightClosed::class.simpleName!!
    val DWELL_WARNING = SceneEvent.DwellWarning::class.simpleName!!
    val DWELL_EXCEEDED = SceneEvent.DwellExceeded::class.simpleName!!
    val SCENE_STATE_CHANGED = SceneEvent.SceneStateChanged::class.simpleName!!
    val SCENE_DWELL_WARNING = SceneEvent.SceneDwellWarning::class.simpleName!!
    val SCENE_DWELL_EXCEEDED = SceneEvent.SceneDwellExceeded::class.simpleName!!
    val STAFF_PRESENCE = SceneEvent.StaffPresenceDetected::class.simpleName!!
    val STAFF_LEFT = SceneEvent.StaffLeftDetected::class.simpleName!!
    val SIGNAL_LOST = SceneEvent.SignalLost::class.simpleName!!
    val SIGNAL_RECOVERED = SceneEvent.SignalRecovered::class.simpleName!!
    val COME_BACK_WARNING = SceneEvent.ComeBackWarning::class.simpleName!!
    val COME_BACK_EXCEEDED = SceneEvent.ComeBackExceeded::class.simpleName!!

    private val personFsm = setOf(TRANSITION, NIGHT_OPENED)

    fun changesPersonState(type: String?): Boolean = type in personFsm
}
