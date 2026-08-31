package com.hub.insights.rollup

import com.manahive.contracts.scene.PersonState
import com.manahive.contracts.scene.StateKind
import com.manahive.contracts.scene.kind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PersonStateCodecTest {

    @Test
    fun `wire hive usa simpleName de PersonState`() {
        assertThat(PersonStateCodec.parse("Lying")).isEqualTo(StateKind.LYING)
        assertThat(PersonStateCodec.parse("SittingInBed")).isEqualTo(StateKind.SITTING_IN_BED)
        assertThat(PersonStateCodec.parse("AttemptingExit")).isEqualTo(StateKind.ATTEMPTING_EXIT)
        assertThat(PersonStateCodec.parse("BedEdge")).isEqualTo(StateKind.BED_EDGE)
        assertThat(PersonStateCodec.parse("Standing")).isEqualTo(StateKind.STANDING)
        assertThat(PersonStateCodec.parse("InBathroom")).isEqualTo(StateKind.IN_BATHROOM)
        assertThat(PersonStateCodec.parse("InRoom")).isEqualTo(StateKind.IN_ROOM)
        assertThat(PersonStateCodec.parse("InHallway")).isEqualTo(StateKind.IN_HALLWAY)
        assertThat(PersonStateCodec.parse("Outdoor")).isEqualTo(StateKind.OUTDOOR)
        assertThat(PersonStateCodec.parse("Absent")).isEqualTo(StateKind.ABSENT)
        assertThat(PersonStateCodec.parse("OnFloor")).isEqualTo(StateKind.ON_FLOOR)
        assertThat(PersonStateCodec.parse("InChair")).isEqualTo(StateKind.IN_CHAIR)
        assertThat(PersonStateCodec.parse("InWheelchair")).isEqualTo(StateKind.IN_WHEELCHAIR)
        assertThat(PersonStateCodec.parse("Unknown")).isEqualTo(StateKind.UNKNOWN)
    }

    @Test
    fun `StateKind y ObservationKind IN_BED tambien parsean`() {
        assertThat(PersonStateCodec.parse("LYING")).isEqualTo(StateKind.LYING)
        assertThat(PersonStateCodec.parse("IN_BED")).isEqualTo(StateKind.LYING)
        assertThat(PersonStateCodec.parse("SITTING_IN_BED")).isEqualTo(StateKind.SITTING_IN_BED)
    }

    @Test
    fun `roundtrip PersonState simpleName == codec`() {
        for (kind in StateKind.entries) {
            val name = PersonStateCodec.person(kind)::class.simpleName!!
            assertThat(PersonStateCodec.parse(name)).isEqualTo(kind)
        }
        assertThat(PersonState.Lying.kind).isEqualTo(StateKind.LYING)
        assertThat(PersonState.SittingInBed.kind).isEqualTo(StateKind.SITTING_IN_BED)
    }

    @Test
    fun `SceneEventTypes son los simpleName del sealed hive`() {
        assertThat(SceneEventTypes.TRANSITION).isEqualTo("TransitionDetected")
        assertThat(SceneEventTypes.NIGHT_OPENED).isEqualTo("NightOpened")
        assertThat(SceneEventTypes.STAFF_PRESENCE).isEqualTo("StaffPresenceDetected")
        assertThat(SceneEventTypes.STAFF_LEFT).isEqualTo("StaffLeftDetected")
        assertThat(SceneEventTypes.COME_BACK_EXCEEDED).isEqualTo("ComeBackExceeded")
        assertThat(SceneEventTypes.changesPersonState("SceneStateChanged")).isFalse()
        assertThat(SceneEventTypes.changesPersonState("NightOpened")).isTrue()
    }

    @Test
    fun `inBed sigue el catalogo hive no walking inventado`() {
        assertThat(StateKind.LYING.inBed).isTrue()
        assertThat(StateKind.BED_EDGE.inBed).isTrue()
        assertThat(StateKind.STANDING.inBed).isFalse()
        assertThat(StateKind.IN_BATHROOM.inBed).isFalse()
        assertThat(StateKind.STANDING.outOfRoom).isFalse()
        assertThat(StateKind.IN_HALLWAY.outOfRoom).isTrue()
        assertThat(StateKind.ABSENT.outOfRoom).isTrue()
        assertThat(StateKind.SITTING_IN_BED.awakeInBed).isTrue()
        assertThat(StateKind.LYING.awakeInBed).isFalse()
    }
}
