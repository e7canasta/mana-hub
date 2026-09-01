package com.hub.residence.api.rest

import com.hub.residence.application.dto.*
import com.hub.residence.application.service.FacilityApplicationService
import com.hub.residence.domain.model.*
import com.hub.shared.domain.BedId
import com.hub.shared.domain.FacilityId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class FacilityController(
    private val facilityApplicationService: FacilityApplicationService
) {

    @PostMapping("/facilities")
    @ResponseStatus(HttpStatus.CREATED)
    fun createFacility(@Valid @RequestBody request: CreateFacilityRequest): FacilityResponse =
        facilityApplicationService.createFacility(request)

    @GetMapping("/facilities")
    fun listFacilities(): List<FacilityResponse> =
        facilityApplicationService.listFacilities()

    @GetMapping("/facilities/{facilityId}")
    fun getFacility(@PathVariable facilityId: String): FacilityResponse =
        facilityApplicationService.getFacility(FacilityId(facilityId))

    @PatchMapping("/facilities/{facilityId}")
    fun updateFacility(
        @PathVariable facilityId: String,
        @Valid @RequestBody request: UpdateFacilityRequest
    ): FacilityResponse =
        facilityApplicationService.updateFacility(FacilityId(facilityId), request)

    @GetMapping("/facilities/{facilityId}/tree")
    fun getFacilityTree(@PathVariable facilityId: String): FacilityTreeResponse =
        facilityApplicationService.getFacilityTree(FacilityId(facilityId))

    @PostMapping("/facilities/{facilityId}/wings")
    @ResponseStatus(HttpStatus.CREATED)
    fun createWing(
        @PathVariable facilityId: String,
        @Valid @RequestBody request: CreateWingRequest
    ): WingResponse =
        facilityApplicationService.createWing(FacilityId(facilityId), request)

    @PostMapping("/wings/{wingId}/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRoom(
        @PathVariable wingId: String,
        @Valid @RequestBody request: CreateRoomRequest
    ): RoomResponse =
        facilityApplicationService.createRoom(WingId(wingId), request)

    @PatchMapping("/wings/{wingId}")
    fun updateWing(
        @PathVariable wingId: String,
        @Valid @RequestBody request: UpdateWingRequest
    ): WingResponse =
        facilityApplicationService.updateWing(WingId(wingId), request)

    @GetMapping("/wings/{wingId}/rooms")
    fun listRoomsByWing(@PathVariable wingId: String): List<RoomResponse> =
        facilityApplicationService.listRoomsByWing(WingId(wingId))

    @GetMapping("/wings/{wingId}/planogram")
    fun getWingPlanogram(@PathVariable wingId: String): WingPlanogramResponse =
        facilityApplicationService.getWingPlanogram(WingId(wingId))

    @PutMapping("/wings/{wingId}/planogram")
    fun replaceWingPlanogram(
        @PathVariable wingId: String,
        @Valid @RequestBody request: UpsertWingPlanogramRequest
    ): WingPlanogramResponse =
        facilityApplicationService.replaceWingPlanogram(WingId(wingId), request)

    @PostMapping("/rooms/{roomId}/beds")
    @ResponseStatus(HttpStatus.CREATED)
    fun createBed(
        @PathVariable roomId: String,
        @Valid @RequestBody request: CreateBedRequest
    ): BedResponse =
        facilityApplicationService.createBed(RoomId(roomId), request)

    @PatchMapping("/rooms/{roomId}")
    fun updateRoom(
        @PathVariable roomId: String,
        @Valid @RequestBody request: UpdateRoomRequest
    ): RoomResponse =
        facilityApplicationService.updateRoom(RoomId(roomId), request)

    @GetMapping("/rooms/{roomId}/beds")
    fun listBedsByRoom(@PathVariable roomId: String): List<BedResponse> =
        facilityApplicationService.listBedsByRoom(RoomId(roomId))

    @GetMapping("/rooms/{roomId}/privacy-regions")
    fun getRoomPrivacyRegions(@PathVariable roomId: String): List<RoomPrivacyRegionResponse> =
        facilityApplicationService.getRoomPrivacyRegions(RoomId(roomId))

    @PutMapping("/rooms/{roomId}/privacy-regions")
    fun replaceRoomPrivacyRegions(
        @PathVariable roomId: String,
        @Valid @RequestBody requests: List<RoomPrivacyRegionRequest>
    ): List<RoomPrivacyRegionResponse> =
        facilityApplicationService.replaceRoomPrivacyRegions(RoomId(roomId), requests)

    @PatchMapping("/beds/{bedId}")
    fun updateBed(
        @PathVariable bedId: String,
        @Valid @RequestBody request: UpdateBedRequest
    ): BedResponse =
        facilityApplicationService.updateBed(BedId(bedId), request)

    @GetMapping("/beds")
    fun listBeds(): List<BedResponse> =
        facilityApplicationService.listBeds()
}
