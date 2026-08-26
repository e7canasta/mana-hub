package com.hub.clients.residence

import com.hub.clients.core.HttpApi
import com.hub.clients.core.ResidenceDsl

@ResidenceDsl
class ResidenceScope internal constructor(private val http: HttpApi) {

    fun setupFacility(name: String, block: FacilityBuilder.() -> Unit = {}): Facility {
        val builder = FacilityBuilder(name).apply(block)
        val resp = http.post("/api/v1/facilities", builder.toFacilityRequest(), FacilityResponse::class.java)
        val facility = Facility(http, resp)

        builder.wings.forEach { wingDef ->
            val wingResp = http.post(
                "/api/v1/facilities/${facility.id}/wings",
                wingDef.toWingRequest(),
                WingResponse::class.java
            )
            val wing = Wing(http, wingResp)

            wingDef.rooms.forEach { roomDef ->
                val roomResp = http.post(
                    "/api/v1/wings/${wing.id}/rooms",
                    roomDef.toRoomRequest(),
                    RoomResponse::class.java
                )
                val room = Room(http, roomResp)

                roomDef.bedLabels.forEach { label ->
                    http.post(
                        "/api/v1/rooms/${room.id}/beds",
                        CreateBedRequest(label),
                        BedResponse::class.java
                    )
                }
            }
        }

        return facility
    }
}

@ResidenceDsl
class FacilityBuilder(private val name: String) {
    var timezone: String = "UTC"
    internal val wings = mutableListOf<WingBuilder>()

    fun wing(name: String, block: WingBuilder.() -> Unit = {}) {
        wings.add(WingBuilder(name).apply(block))
    }

    internal fun toFacilityRequest() = CreateFacilityRequest(name, timezone)
}

@ResidenceDsl
class WingBuilder internal constructor(val name: String) {
    var floor: String? = null
    var sortOrder: Int = 0
    internal val rooms = mutableListOf<RoomBuilder>()

    fun room(number: String, block: RoomBuilder.() -> Unit = {}) {
        rooms.add(RoomBuilder(number).apply(block))
    }

    internal fun toWingRequest() = CreateWingRequest(name, floor, sortOrder)
}

@ResidenceDsl
class RoomBuilder internal constructor(val number: String) {
    var roomType: String? = null
    var streamKey: String? = null
    internal val bedLabels = mutableListOf<String>()

    fun bed(label: String) {
        bedLabels.add(label)
    }

    internal fun toRoomRequest() = CreateRoomRequest(number, roomType, streamKey)
}

class Facility internal constructor(
    private val http: HttpApi,
    val raw: FacilityResponse
) {
    val id: String get() = raw.id
    val name: String get() = raw.name
    val timezone: String get() = raw.timezone

    fun tree(): FacilityTreeResponse =
        http.get("/api/v1/facilities/$id/tree", FacilityTreeResponse::class.java)

    fun firstBed(): BedResponse? {
        val t = tree()
        return t.wings.firstOrNull()?.rooms?.firstOrNull()?.beds?.firstOrNull()
    }

    override fun toString(): String = "Facility($name)"
}

class Wing internal constructor(
    private val http: HttpApi,
    val raw: WingResponse
) {
    val id: String get() = raw.id
    val name: String get() = raw.name

    override fun toString(): String = "Wing($name)"
}

class Room internal constructor(
    private val http: HttpApi,
    val raw: RoomResponse
) {
    val id: String get() = raw.id
    val number: String get() = raw.number

    override fun toString(): String = "Room($number)"
}
