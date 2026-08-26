package com.hub.clients.core

import com.hub.clients.audit.AuditScope
import com.hub.clients.care.CareScope
import com.hub.clients.evidence.EvidenceScope
import com.hub.clients.history.HistoryScope
import com.hub.clients.identity.IdentityScope
import com.hub.clients.identity.UserResponse
import com.hub.clients.observation.ObservationScope
import com.hub.clients.policy.PolicyScope
import com.hub.clients.population.PopulationScope
import com.hub.clients.population.ResidentResponse
import com.hub.clients.residence.BedResponse
import com.hub.clients.residence.FacilityResponse
import com.hub.clients.residence.FacilityTreeResponse
import com.hub.clients.residence.ResidenceScope
import com.hub.clients.streams.StreamResponse
import com.hub.clients.streams.StreamsScope
import com.hub.clients.surveillance.SurveillanceScope

@ManaHubDsl
class ManaHubScope internal constructor(url: String) {

    private val http = HttpApi(url)

    val identity: IdentityScope = IdentityScope(http)
    val residence: ResidenceScope = ResidenceScope(http)
    val population: PopulationScope = PopulationScope(http)
    val streams: StreamsScope = StreamsScope(http)
    val surveillance: SurveillanceScope = SurveillanceScope(http)
    val policy: PolicyScope = PolicyScope(http)
    val audit: AuditScope = AuditScope(http)
    val care: CareScope = CareScope(http)
    val history: HistoryScope = HistoryScope(http)
    val evidence: EvidenceScope = EvidenceScope(http)
    val observation: ObservationScope = ObservationScope(http)

    fun queryUsers(): List<UserResponse> =
        http.get("/api/v1/users", Array<UserResponse>::class.java).toList()

    fun queryUser(id: String): UserResponse =
        http.get("/api/v1/users/$id", UserResponse::class.java)

    fun queryFacilities(): List<FacilityResponse> =
        http.get("/api/v1/facilities", Array<FacilityResponse>::class.java).toList()

    fun queryFacility(id: String): FacilityResponse =
        http.get("/api/v1/facilities/$id", FacilityResponse::class.java)

    fun queryFacilityTree(id: String): FacilityTreeResponse =
        http.get("/api/v1/facilities/$id/tree", FacilityTreeResponse::class.java)

    fun queryBeds(): List<BedResponse> =
        http.get("/api/v1/beds", Array<BedResponse>::class.java).toList()

    fun queryResidents(): List<ResidentResponse> =
        http.get("/api/v1/residents", Array<ResidentResponse>::class.java).toList()

    fun queryResident(id: String): ResidentResponse =
        http.get("/api/v1/residents/$id", ResidentResponse::class.java)

    fun queryStreams(roomId: String): List<StreamResponse> =
        http.get("/api/v1/rooms/$roomId/streams", Array<StreamResponse>::class.java).toList()
}

fun manahub(url: String = "http://localhost:8080", block: ManaHubScope.() -> Unit): ManaHubScope =
    ManaHubScope(url).apply(block)
