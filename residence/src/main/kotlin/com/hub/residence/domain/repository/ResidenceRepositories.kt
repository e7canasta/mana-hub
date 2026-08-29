package com.hub.residence.domain.repository

import com.hub.residence.domain.model.*
import com.hub.shared.domain.BedId
import com.hub.shared.domain.FacilityId

interface FacilityRepository {
    fun findById(id: FacilityId): Facility?
    fun findAll(): List<Facility>
    fun save(facility: Facility): Facility
}

interface WingRepository {
    fun findById(id: WingId): Wing?
    fun findByFacilityId(facilityId: FacilityId): List<Wing>
    fun save(wing: Wing): Wing
}

interface RoomRepository {
    fun findById(id: RoomId): Room?
    fun findByWingId(wingId: WingId): List<Room>
    fun save(room: Room): Room
}

interface BedRepository {
    fun findById(id: BedId): Bed?
    fun findAll(): List<Bed>
    fun findByRoomId(roomId: RoomId): List<Bed>
    fun save(bed: Bed): Bed
    fun saveAndFlush(bed: Bed): Bed
}

interface PlanogramRepository {
    fun findActiveByWingId(wingId: WingId): List<PlanogramPlacement>
    fun deleteByWingId(wingId: WingId)
    fun saveAll(placements: List<PlanogramPlacement>): List<PlanogramPlacement>
}

interface RoomPrivacyRegionRepository {
    fun findActiveByRoomId(roomId: RoomId): List<RoomPrivacyRegion>
    fun deleteByRoomId(roomId: RoomId)
    fun saveAll(regions: List<RoomPrivacyRegion>): List<RoomPrivacyRegion>
}
