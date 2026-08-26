package com.hub.clients.residence

import com.fasterxml.jackson.annotation.JsonProperty

data class CreateFacilityRequest(
    val name: String,
    val timezone: String = "UTC"
)

data class FacilityResponse(
    val id: String,
    val name: String,
    val timezone: String,
    @JsonProperty("retired") val isRetired: Boolean = false
)

data class CreateWingRequest(
    val name: String,
    val floor: String? = null,
    val sortOrder: Int = 0
)

data class WingResponse(
    val id: String,
    @JsonProperty("facilityId") val facilityId: String,
    val name: String,
    val floor: String? = null,
    val sortOrder: Int = 0,
    @JsonProperty("retired") val isRetired: Boolean = false
)

data class CreateRoomRequest(
    val number: String,
    val roomType: String? = null,
    val streamKey: String? = null
)

data class RoomResponse(
    val id: String,
    @JsonProperty("wingId") val wingId: String,
    val number: String,
    val roomType: String? = null,
    val streamKey: String? = null,
    @JsonProperty("retired") val isRetired: Boolean = false
)

data class CreateBedRequest(
    val label: String,
    val monitorKey: String? = null
)

data class BedResponse(
    val id: String,
    @JsonProperty("roomId") val roomId: String,
    val label: String,
    val monitorKey: String? = null,
    @JsonProperty("retired") val isRetired: Boolean = false
)

data class FacilityTreeResponse(
    val facility: FacilityResponse,
    val wings: List<WingTreeResponse>
)

data class WingTreeResponse(
    val wing: WingResponse,
    val rooms: List<RoomTreeResponse>
)

data class RoomTreeResponse(
    val room: RoomResponse,
    val beds: List<BedResponse>
)
