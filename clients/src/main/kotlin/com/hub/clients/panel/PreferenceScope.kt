package com.hub.clients.panel

import com.hub.clients.core.HttpApi
import com.hub.panel.dto.*
import com.manahive.contracts.policy.MobilityAid
import com.manahive.contracts.policy.PolicyMode
import com.manahive.contracts.policy.RiskLevel

class PreferenceScope internal constructor(private val http: HttpApi) {

    fun catalog(): AlarmCatalogDto =
        http.get("/api/v1/panel/preferences/catalog", AlarmCatalogDto::class.java)

    fun list(): List<PreferenceListItemDto> =
        http.get("/api/v1/panel/preferences", Array<PreferenceListItemDto>::class.java).toList()

    fun detail(residentId: String): PreferenceFullDto =
        http.get("/api/v1/panel/preferences/$residentId", PreferenceFullDto::class.java)

    fun save(residentId: String, block: SavePreferencesBuilder.() -> Unit): SavePreferencesResponse {
        val builder = SavePreferencesBuilder().apply(block)
        return http.post(
            "/api/v1/panel/preferences/$residentId/save",
            builder.toRequest(),
            SavePreferencesResponse::class.java,
        )
    }

    fun recommendations(residentId: String): RecommendationListResponse =
        http.get("/api/v1/panel/preferences/$residentId/recommendations", RecommendationListResponse::class.java)
}

class SavePreferencesBuilder {
    var riskLevel: RiskLevel? = null
    var mobilityAid: MobilityAid? = null
    var autopilot: Boolean? = null
    var mode: PolicyMode? = null
    var templateId: String? = null
    var overrides: Map<TransitionId, TransitionOverrideDto>? = null
    var reason: String? = null
    var updatedBy: String? = null

    internal fun toRequest() = SavePreferencesRequest(
        riskLevel = riskLevel,
        mobilityAid = mobilityAid,
        autopilot = autopilot,
        mode = mode,
        templateId = templateId,
        overrides = overrides,
        reason = reason,
        updatedBy = updatedBy,
    )
}
