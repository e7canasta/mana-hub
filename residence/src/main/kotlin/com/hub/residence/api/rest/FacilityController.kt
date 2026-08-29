package com.hub.residence.api.rest

import com.hub.residence.application.dto.*
import com.hub.residence.application.service.FacilityApplicationService
import com.hub.residence.domain.model.*
import com.hub.shared.domain.BedId
import com.hub.shared.domain.FacilityId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class FacilityController(
    private val facilityApplicationService: FacilityApplicationService
) {

    @PostMapping("/facilities")
    fun createFacility(@Valid @RequestBody request: CreateFacilityRequest): ResponseEntity<FacilityResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(facilityApplicationService.createFacility(request))
    }

    @GetMapping("/facilities")
    fun listFacilities(): ResponseEntity<List<FacilityResponse>> {
        return ResponseEntity.ok(facilityApplicationService.listFacilities())
    }

    @GetMapping("/facilities/{facilityId}")
    fun getFacility(@PathVariable facilityId: String): ResponseEntity<FacilityResponse> {
        return ResponseEntity.ok(facilityApplicationService.getFacility(FacilityId(facilityId)))
    }

    @PatchMapping("/facilities/{facilityId}")
    fun updateFacility(
        @PathVariable facilityId: String,
        @Valid @RequestBody request: UpdateFacilityRequest
    ): ResponseEntity<FacilityResponse> {
        return ResponseEntity.ok(facilityApplicationService.updateFacility(FacilityId(facilityId), request))
    }

    @GetMapping("/facilities/{facilityId}/tree")
    fun getFacilityTree(@PathVariable facilityId: String): ResponseEntity<FacilityTreeResponse> {
        return ResponseEntity.ok(facilityApplicationService.getFacilityTree(FacilityId(facilityId)))
    }

    @PostMapping("/facilities/{facilityId}/wings")
    fun createWing(
        @PathVariable facilityId: String,
        @Valid @RequestBody request: CreateWingRequest
    ): ResponseEntity<WingResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            facilityApplicationService.createWing(FacilityId(facilityId), request)
        )
    }

    @PostMapping("/wings/{wingId}/rooms")
    fun createRoom(
        @PathVariable wingId: String,
        @Valid @RequestBody request: CreateRoomRequest
    ): ResponseEntity<RoomResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            facilityApplicationService.createRoom(WingId(wingId), request)
        )
    }

    @PatchMapping("/wings/{wingId}")
    fun updateWing(
        @PathVariable wingId: String,
        @Valid @RequestBody request: UpdateWingRequest
    ): ResponseEntity<WingResponse> {
        return ResponseEntity.ok(facilityApplicationService.updateWing(WingId(wingId), request))
    }

    @GetMapping("/wings/{wingId}/rooms")
    fun listRoomsByWing(@PathVariable wingId: String): ResponseEntity<List<RoomResponse>> {
        return ResponseEntity.ok(facilityApplicationService.listRoomsByWing(WingId(wingId)))
    }

    @GetMapping("/wings/{wingId}/planogram")
    fun getWingPlanogram(@PathVariable wingId: String): ResponseEntity<WingPlanogramResponse> {
        return ResponseEntity.ok(facilityApplicationService.getWingPlanogram(WingId(wingId)))
    }

    @PutMapping("/wings/{wingId}/planogram")
    fun replaceWingPlanogram(
        @PathVariable wingId: String,
        @Valid @RequestBody request: UpsertWingPlanogramRequest
    ): ResponseEntity<WingPlanogramResponse> {
        return ResponseEntity.ok(facilityApplicationService.replaceWingPlanogram(WingId(wingId), request))
    }

    @PostMapping("/rooms/{roomId}/beds")
    fun createBed(
        @PathVariable roomId: String,
        @Valid @RequestBody request: CreateBedRequest
    ): ResponseEntity<BedResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            facilityApplicationService.createBed(RoomId(roomId), request)
        )
    }

    @PatchMapping("/rooms/{roomId}")
    fun updateRoom(
        @PathVariable roomId: String,
        @Valid @RequestBody request: UpdateRoomRequest
    ): ResponseEntity<RoomResponse> {
        return ResponseEntity.ok(facilityApplicationService.updateRoom(RoomId(roomId), request))
    }

    @GetMapping("/rooms/{roomId}/beds")
    fun listBedsByRoom(@PathVariable roomId: String): ResponseEntity<List<BedResponse>> {
        return ResponseEntity.ok(facilityApplicationService.listBedsByRoom(RoomId(roomId)))
    }

    @GetMapping("/rooms/{roomId}/privacy-regions")
    fun getRoomPrivacyRegions(@PathVariable roomId: String): ResponseEntity<List<RoomPrivacyRegionResponse>> {
        return ResponseEntity.ok(facilityApplicationService.getRoomPrivacyRegions(RoomId(roomId)))
    }

    @PutMapping("/rooms/{roomId}/privacy-regions")
    fun replaceRoomPrivacyRegions(
        @PathVariable roomId: String,
        @Valid @RequestBody requests: List<RoomPrivacyRegionRequest>
    ): ResponseEntity<List<RoomPrivacyRegionResponse>> {
        return ResponseEntity.ok(facilityApplicationService.replaceRoomPrivacyRegions(RoomId(roomId), requests))
    }

    @PatchMapping("/beds/{bedId}")
    fun updateBed(
        @PathVariable bedId: String,
        @Valid @RequestBody request: UpdateBedRequest
    ): ResponseEntity<BedResponse> {
        return ResponseEntity.ok(facilityApplicationService.updateBed(BedId(bedId), request))
    }

    @GetMapping("/beds")
    fun listBeds(): ResponseEntity<List<BedResponse>> {
        return ResponseEntity.ok(facilityApplicationService.listBeds())
    }
}
