package com.hub.clients.population

import com.hub.clients.core.HttpApi
import com.hub.clients.core.PopulationDsl
import java.time.LocalDate

@PopulationDsl
class PopulationScope internal constructor(private val http: HttpApi) {

    fun admitResident(block: ResidentBuilder.() -> Unit): Resident {
        val builder = ResidentBuilder().apply(block)
        val resp = http.post("/api/v1/residents", builder.toAdmissionRequest(), ResidentResponse::class.java)
        return Resident(http, resp)
    }

    fun admitResident(
        fullName: String,
        birthDate: LocalDate? = null,
        admissionDate: LocalDate = LocalDate.now()
    ): Resident = admitResident {
        this.fullName = fullName
        this.birthDate = birthDate
        this.admissionDate = admissionDate
    }
}

@PopulationDsl
class ResidentBuilder {
    var fullName: String = ""
    var birthDate: LocalDate? = null
    var admissionDate: LocalDate = LocalDate.now()
    var externalId: String? = null

    internal fun toAdmissionRequest() = CreateResidentRequest(fullName, birthDate, admissionDate, externalId)
}

class Resident internal constructor(
    private val http: HttpApi,
    var raw: ResidentResponse
) {
    val id: String get() = raw.id
    val fullName: String get() = raw.fullName
    val birthDate: LocalDate? get() = raw.birthDate
    val admissionDate: LocalDate get() = raw.admissionDate
    val status: ResidentStatus get() = raw.status
    val isDischarged: Boolean get() = raw.isDischarged

    fun assignTo(bedId: String): AssignmentResponse =
        http.post("/api/v1/residents/$id/assignments", CreateAssignmentRequest(bedId), AssignmentResponse::class.java)

    fun assignments(): List<AssignmentResponse> =
        http.get("/api/v1/residents/$id/assignments", Array<AssignmentResponse>::class.java).toList()

    fun discharge(actorId: String? = null): ResidentResponse {
        val resp = http.post("/api/v1/residents/$id/discharge", DischargeRequest(actorId), ResidentResponse::class.java)
        raw = resp
        return resp
    }

    override fun toString(): String = "Resident($fullName, $status)"
}
