package com.hub.insights.inbound

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.LocalDate

@Component
class HubClient(
    private val hubRestClient: RestClient,
) {
    fun listResidents(): List<HubResident> =
        hubRestClient.get()
            .uri("/api/v1/residents")
            .retrieve()
            .body(object : ParameterizedTypeReference<List<HubResident>>() {})
            ?: emptyList()

    fun getChart(residentId: String): HubChart? =
        try {
            hubRestClient.get()
                .uri("/api/v1/views/resident-chart/{id}", residentId)
                .retrieve()
                .body(HubChart::class.java)
        } catch (ex: RestClientResponseException) {
            if (ex.statusCode.value() == 404) null else throw ex
        }

    fun getSceneEvents(residentId: String): List<HubSceneEvent> =
        hubRestClient.get()
            .uri("/api/v1/residents/{id}/scene-events", residentId)
            .retrieve()
            .body(object : ParameterizedTypeReference<List<HubSceneEvent>>() {})
            ?: emptyList()

    fun getSleep(residentId: String, from: LocalDate, to: LocalDate): HubSleepTab =
        hubRestClient.get()
            .uri("/api/v1/views/resident-chart/{id}/sleep?from={from}&to={to}", residentId, from, to)
            .retrieve()
            .body(HubSleepTab::class.java)
            ?: HubSleepTab(residentId)

    fun getMobility(residentId: String, from: LocalDate, to: LocalDate): HubMobilityTab =
        hubRestClient.get()
            .uri("/api/v1/views/resident-chart/{id}/mobility?from={from}&to={to}", residentId, from, to)
            .retrieve()
            .body(HubMobilityTab::class.java)
            ?: HubMobilityTab(residentId)

    fun getBathroom(residentId: String, from: LocalDate, to: LocalDate): HubBathroomTab =
        hubRestClient.get()
            .uri("/api/v1/views/resident-chart/{id}/bathroom?from={from}&to={to}", residentId, from, to)
            .retrieve()
            .body(HubBathroomTab::class.java)
            ?: HubBathroomTab(residentId)

    fun getCare(residentId: String, from: LocalDate, to: LocalDate): HubCareTab =
        hubRestClient.get()
            .uri("/api/v1/views/resident-chart/{id}/care?from={from}&to={to}", residentId, from, to)
            .retrieve()
            .body(HubCareTab::class.java)
            ?: HubCareTab(residentId)

    fun ingestSleep(residentId: String, observedOn: LocalDate, data: Map<String, Any?>): PublishResult =
        postSummary("/internal/v1/clinical/sleep-summaries", envelope(residentId, observedOn, "sleep", data))

    fun ingestMobility(residentId: String, observedOn: LocalDate, data: Map<String, Any?>): PublishResult =
        postSummary("/internal/v1/clinical/mobility-summaries", envelope(residentId, observedOn, "mobility", data))

    fun ingestBathroom(residentId: String, observedOn: LocalDate, data: Map<String, Any?>): PublishResult =
        postSummary("/internal/v1/clinical/bathroom-summaries", envelope(residentId, observedOn, "bathroom", data))

    fun ingestCare(
        residentId: String,
        observedOn: LocalDate,
        totalMinutes: Int,
        proactiveMinutes: Int,
        roundsCount: Int,
        notesCount: Int,
    ): PublishResult {
        val body = mapOf(
            "sourceRecordId" to sourceId(residentId, observedOn, "care"),
            "residentId" to residentId,
            "observedOn" to observedOn.toString(),
            "totalMinutes" to totalMinutes,
            "proactiveMinutes" to proactiveMinutes,
            "roundsCount" to roundsCount,
            "notesCount" to notesCount,
            "source" to "insights",
            "modelVersion" to "insights-0.1",
        )
        return postSummary("/internal/v1/care-summaries", body)
    }

    private fun envelope(
        residentId: String,
        observedOn: LocalDate,
        kind: String,
        data: Map<String, Any?>,
    ) = IngestEnvelope(
        sourceRecordId = sourceId(residentId, observedOn, kind),
        residentId = residentId,
        observedOn = observedOn,
        data = data,
    )

    private fun sourceId(residentId: String, observedOn: LocalDate, kind: String) =
        "insights-$residentId-$observedOn-$kind"

    private fun postSummary(path: String, body: Any): PublishResult {
        return try {
            hubRestClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity()
            PublishResult.Created
        } catch (ex: RestClientResponseException) {
            when (ex.statusCode.value()) {
                409, 500 -> PublishResult.AlreadyExists
                else -> throw ex
            }
        }
    }
}

enum class PublishResult { Created, AlreadyExists, Skipped }
