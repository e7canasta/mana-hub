package com.hub.shared.domain

/**
 * Resolves bed → room → wing hierarchy using repository interfaces.
 *
 * Both bootstrap/ProjectionService and panel-api/PanelProjectionService
 * independently resolve this hierarchy. This shared resolver eliminates
 * the duplication.
 *
 * The resolver caches results per call to avoid N+1 queries when resolving
 * multiple beds in the same room/wing.
 */
interface LocationResolver {
    fun resolve(bedId: BedId): BedLocation?
    fun resolveAll(bedIds: Set<BedId>): Map<BedId, BedLocation>
    fun zone(bedId: BedId): java.time.ZoneId
}
