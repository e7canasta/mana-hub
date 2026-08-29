package com.hub.residence.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.hub.residence.domain.model.*

data class CreateFacilityRequest(
    val name: String,
    val timezone: String = "UTC"
)

data class UpdateFacilityRequest(
    val name: String? = null,
    val timezone: String? = null
)

data class FacilityResponse(
    val id: String,
    val name: String,
    val timezone: String,
    @JsonProperty("isRetired") val isRetired: Boolean
)

data class CreateWingRequest(
    val name: String,
    val floor: String? = null,
    val sortOrder: Int = 0
)

data class UpdateWingRequest(
    val name: String? = null,
    val floor: String? = null,
    val sortOrder: Int? = null
)

data class WingResponse(
    val id: String,
    val facilityId: String,
    val name: String,
    val floor: String?,
    val sortOrder: Int,
    @JsonProperty("isRetired") val isRetired: Boolean
)

data class CreateRoomRequest(
    val number: String,
    val roomType: String? = null,
    val streamKey: String? = null
)

data class UpdateRoomRequest(
    val number: String? = null,
    val roomType: String? = null,
    val streamKey: String? = null
)

data class RoomResponse(
    val id: String,
    val wingId: String,
    val number: String,
    val roomType: String?,
    val streamKey: String?,
    @JsonProperty("isRetired") val isRetired: Boolean
)

data class CreateBedRequest(
    val label: String,
    val monitorKey: String? = null
)

data class UpdateBedRequest(
    val label: String? = null,
    val monitorKey: String? = null
)

data class BedResponse(
    val id: String,
    val roomId: String,
    val label: String,
    val monitorKey: String?,
    @JsonProperty("isRetired") val isRetired: Boolean
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

data class PlanogramPlacementRequest(
    val roomId: String,
    val x: Double,
    val y: Double,
    val sortOrder: Int = 0
)

data class UpsertWingPlanogramRequest(
    val wingId: String? = null,
    val placements: List<PlanogramPlacementRequest> = emptyList()
)

data class PlanogramPlacementResponse(
    val roomId: String,
    val x: Double,
    val y: Double,
    val sortOrder: Int
)

data class WingPlanogramResponse(
    val wingId: String,
    val placements: List<PlanogramPlacementResponse>
)

data class RoomPrivacyRegionRequest(
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double
)

data class RoomPrivacyRegionResponse(
    val id: String,
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double
)
