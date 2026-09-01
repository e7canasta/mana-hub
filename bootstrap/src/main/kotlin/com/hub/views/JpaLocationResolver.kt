package com.hub.views

import com.hub.residence.domain.repository.BedRepository
import com.hub.residence.domain.repository.FacilityRepository
import com.hub.residence.domain.repository.RoomRepository
import com.hub.residence.domain.repository.WingRepository
import com.hub.shared.domain.BedId
import com.hub.shared.domain.BedLocation
import com.hub.shared.domain.LocationResolver
import org.springframework.stereotype.Component
import java.time.ZoneId

private val DEFAULT_ZONE = ZoneId.of("America/Argentina/Buenos_Aires")

@Component
class JpaLocationResolver(
    private val bedRepository: BedRepository,
    private val roomRepository: RoomRepository,
    private val wingRepository: WingRepository,
    private val facilityRepository: FacilityRepository,
) : LocationResolver {

    private val rooms = mutableMapOf<String, com.hub.residence.domain.model.Room?>()
    private val wings = mutableMapOf<String, com.hub.residence.domain.model.Wing?>()
    private val facilities = mutableMapOf<String, com.hub.residence.domain.model.Facility?>()

    override fun resolve(bedId: BedId): BedLocation? {
        val bed = bedRepository.findById(bedId) ?: return null
        val room = rooms.getOrPut(bed.roomId.value) { roomRepository.findById(bed.roomId) }
        val wing = room?.let { r -> wings.getOrPut(r.wingId.value) { wingRepository.findById(r.wingId) } }
        return BedLocation.of(wing?.name, room?.number, bed.label)
    }

    override fun resolveAll(bedIds: Set<BedId>): Map<BedId, BedLocation> {
        val result = mutableMapOf<BedId, BedLocation>()
        for (bedId in bedIds) {
            val resolved = resolve(bedId)
            if (resolved != null) {
                result[bedId] = resolved
            }
        }
        return result
    }

    override fun zone(bedId: BedId): ZoneId {
        val bed = bedRepository.findById(bedId) ?: return DEFAULT_ZONE
        val room = rooms.getOrPut(bed.roomId.value) { roomRepository.findById(bed.roomId) }
        val wing = room?.let { r -> wings.getOrPut(r.wingId.value) { wingRepository.findById(r.wingId) } }
        val facility = wing?.let { w -> facilities.getOrPut(w.facilityId.value) { facilityRepository.findById(w.facilityId) } }
        return facility?.timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: DEFAULT_ZONE
    }
}
