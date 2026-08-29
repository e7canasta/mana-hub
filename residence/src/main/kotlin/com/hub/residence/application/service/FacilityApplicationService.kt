package com.hub.residence.application.service

import com.hub.residence.application.dto.*
import com.hub.residence.domain.model.*
import com.hub.shared.domain.BedId
import com.hub.shared.domain.FacilityId
import com.hub.residence.domain.repository.*
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private const val MONITOR_ALREADY_LINKED = "El monitor ya esta vinculado a otra cama"

@Service
class FacilityApplicationService(
    private val facilityRepository: FacilityRepository,
    private val wingRepository: WingRepository,
    private val roomRepository: RoomRepository,
    private val bedRepository: BedRepository,
    private val planogramRepository: PlanogramRepository,
    private val roomPrivacyRegionRepository: RoomPrivacyRegionRepository
) {

    @Transactional
    fun createFacility(request: CreateFacilityRequest): FacilityResponse {
        val facility = Facility.create(name = request.name, timezone = request.timezone)
        return facilityRepository.save(facility).toResponse()
    }

    @Transactional(readOnly = true)
    fun getFacility(id: FacilityId): FacilityResponse {
        return facilityRepository.findById(id)?.toResponse()
            ?: throw IllegalArgumentException("Facility not found: $id")
    }

    @Transactional(readOnly = true)
    fun listFacilities(): List<FacilityResponse> {
        return facilityRepository.findAll().map { it.toResponse() }
    }

    @Transactional
    fun updateFacility(id: FacilityId, request: UpdateFacilityRequest): FacilityResponse {
        val facility = facilityRepository.findById(id)
            ?: throw IllegalArgumentException("Facility not found: $id")
        return facilityRepository.save(facility.updateProfile(request.name, request.timezone)).toResponse()
    }

    @Transactional(readOnly = true)
    fun getFacilityTree(id: FacilityId): FacilityTreeResponse {
        val facility = facilityRepository.findById(id)
            ?: throw IllegalArgumentException("Facility not found: $id")

        val wings = wingRepository.findByFacilityId(id).map { wing ->
            val rooms = roomRepository.findByWingId(wing.id).map { room ->
                val beds = bedRepository.findByRoomId(room.id).map { it.toResponse() }
                RoomTreeResponse(room = room.toResponse(), beds = beds)
            }
            WingTreeResponse(wing = wing.toResponse(), rooms = rooms)
        }

        return FacilityTreeResponse(facility = facility.toResponse(), wings = wings)
    }

    @Transactional
    fun createWing(facilityId: FacilityId, request: CreateWingRequest): WingResponse {
        require(facilityRepository.findById(facilityId) != null) { "Facility not found: $facilityId" }
        val wing = Wing.create(facilityId = facilityId, name = request.name, floor = request.floor, sortOrder = request.sortOrder)
        return wingRepository.save(wing).toResponse()
    }

    @Transactional
    fun updateWing(id: WingId, request: UpdateWingRequest): WingResponse {
        val wing = wingRepository.findById(id)
            ?: throw IllegalArgumentException("Wing not found: $id")
        return wingRepository.save(wing.updateProfile(request.name, request.floor, request.sortOrder)).toResponse()
    }

    @Transactional
    fun createRoom(wingId: WingId, request: CreateRoomRequest): RoomResponse {
        require(wingRepository.findById(wingId) != null) { "Wing not found: $wingId" }
        val room = Room.create(wingId = wingId, number = request.number, roomType = request.roomType, streamKey = request.streamKey)
        return roomRepository.save(room).toResponse()
    }

    @Transactional
    fun updateRoom(id: RoomId, request: UpdateRoomRequest): RoomResponse {
        val room = roomRepository.findById(id)
            ?: throw IllegalArgumentException("Room not found: $id")
        return roomRepository.save(room.updateProfile(request.number, request.roomType, request.streamKey)).toResponse()
    }

    @Transactional
    fun createBed(roomId: RoomId, request: CreateBedRequest): BedResponse {
        require(roomRepository.findById(roomId) != null) { "Room not found: $roomId" }
        val bed = Bed.create(roomId = roomId, label = request.label, monitorKey = request.monitorKey)
        return saveBed(bed).toResponse()
    }

    @Transactional
    fun updateBed(id: BedId, request: UpdateBedRequest): BedResponse {
        val bed = bedRepository.findById(id)
            ?: throw IllegalArgumentException("Bed not found: $id")
        return saveBed(bed.updateProfile(request.label, request.monitorKey)).toResponse()
    }

    private fun saveBed(bed: Bed): Bed {
        return try {
            bedRepository.saveAndFlush(bed)
        } catch (e: DataIntegrityViolationException) {
            throw IllegalStateException(MONITOR_ALREADY_LINKED, e)
        }
    }

    @Transactional(readOnly = true)
    fun listBeds(): List<BedResponse> {
        return bedRepository.findAll().map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun listRoomsByWing(wingId: WingId): List<RoomResponse> {
        require(wingRepository.findById(wingId) != null) { "Wing not found: $wingId" }
        return roomRepository.findByWingId(wingId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun listBedsByRoom(roomId: RoomId): List<BedResponse> {
        require(roomRepository.findById(roomId) != null) { "Room not found: $roomId" }
        return bedRepository.findByRoomId(roomId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getWingPlanogram(wingId: WingId): WingPlanogramResponse {
        require(wingRepository.findById(wingId) != null) { "Wing not found: $wingId" }
        return wingId.toPlanogramResponse(planogramRepository.findActiveByWingId(wingId))
    }

    @Transactional
    fun replaceWingPlanogram(wingId: WingId, request: UpsertWingPlanogramRequest): WingPlanogramResponse {
        require(wingRepository.findById(wingId) != null) { "Wing not found: $wingId" }
        planogramRepository.deleteByWingId(wingId)
        val placements = request.placements.map {
            PlanogramPlacement(
                id = UUID.randomUUID().toString(), wingId = wingId, roomId = RoomId(it.roomId),
                x = it.x, y = it.y, sortOrder = it.sortOrder, active = true
            )
        }
        planogramRepository.saveAll(placements)
        return wingId.toPlanogramResponse(placements.sortedBy { it.sortOrder })
    }

    @Transactional(readOnly = true)
    fun getRoomPrivacyRegions(roomId: RoomId): List<RoomPrivacyRegionResponse> {
        require(roomRepository.findById(roomId) != null) { "Room not found: $roomId" }
        return roomPrivacyRegionRepository.findActiveByRoomId(roomId).map { it.toResponse() }
    }

    @Transactional
    fun replaceRoomPrivacyRegions(roomId: RoomId, requests: List<RoomPrivacyRegionRequest>): List<RoomPrivacyRegionResponse> {
        require(roomRepository.findById(roomId) != null) { "Room not found: $roomId" }
        roomPrivacyRegionRepository.deleteByRoomId(roomId)
        val regions = requests.map {
            RoomPrivacyRegion(
                id = UUID.randomUUID().toString(), roomId = roomId,
                x = it.x, y = it.y, w = it.w, h = it.h, active = true
            )
        }
        return roomPrivacyRegionRepository.saveAll(regions).map { it.toResponse() }
    }
    private fun Facility.toResponse() = FacilityResponse(id = id.value, name = name, timezone = timezone, isRetired = isRetired)
    private fun Wing.toResponse() = WingResponse(id = id.value, facilityId = facilityId.value, name = name, floor = floor, sortOrder = sortOrder, isRetired = isRetired)
    private fun Room.toResponse() = RoomResponse(id = id.value, wingId = wingId.value, number = number, roomType = roomType, streamKey = streamKey, isRetired = isRetired)
    private fun Bed.toResponse() = BedResponse(id = id.value, roomId = roomId.value, label = label, monitorKey = monitorKey, isRetired = isRetired)

    private fun RoomPrivacyRegion.toResponse() = RoomPrivacyRegionResponse(id = id, x = x, y = y, w = w, h = h)

    private fun WingId.toPlanogramResponse(placements: List<PlanogramPlacement>) = WingPlanogramResponse(
        wingId = value,
        placements = placements.map { PlanogramPlacementResponse(roomId = it.roomId.value, x = it.x, y = it.y, sortOrder = it.sortOrder) }
    )
}
