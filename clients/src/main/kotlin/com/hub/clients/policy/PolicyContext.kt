package com.hub.clients.policy

import com.hub.clients.core.HttpApi
import com.hub.clients.core.PolicyDsl

@PolicyDsl
class PolicyScope internal constructor(private val http: HttpApi) {

    fun catalog(): AlarmPresetCatalogResponse =
        http.get("/api/v1/alarm-presets/catalog", AlarmPresetCatalogResponse::class.java)

    fun presetById(presetId: String): AlarmPresetDefinition? =
        catalog().presets.find { it.id == presetId }

    fun configureAlarmProfile(residentId: String, block: AlarmProfileBuilder.() -> Unit): AlarmProfile {
        val builder = AlarmProfileBuilder().apply(block)
        val resp = http.patch(
            "/api/v1/alarm-presets/$residentId",
            builder.toRequest(),
            AlarmProfileResponse::class.java
        )
        return AlarmProfile(http, resp)
    }

    fun alarmProfile(residentId: String): AlarmProfileResponse? {
        return try {
            http.get("/api/v1/alarm-presets/$residentId", AlarmProfileResponse::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun alarmProfileHistory(residentId: String): List<AlarmProfileResponse> =
        http.get("/api/v1/alarm-presets/$residentId/history", Array<AlarmProfileResponse>::class.java).toList()
}

@PolicyDsl
class AlarmProfileBuilder {
    var mobilityAid: String? = null
    var autopilot: Boolean = false
    var mode: String? = null
    var templateId: String? = null
    var overridesJson: String = "{}"
    var riskLevel: RiskLevel = RiskLevel.MEDIUM
    var updatedBy: String? = null

    internal fun toRequest() = UpdateAlarmProfileRequest(
        mobilityAid = mobilityAid,
        autopilot = autopilot,
        mode = mode,
        templateId = templateId,
        overridesJson = overridesJson,
        riskLevel = riskLevel,
        updatedBy = updatedBy
    )
}

class AlarmProfile internal constructor(
    private val http: HttpApi,
    val raw: AlarmProfileResponse
) {
    val id: String get() = raw.id
    val residentId: String get() = raw.residentId
    val riskLevel: RiskLevel get() = raw.riskLevel
    val autopilot: Boolean get() = raw.autopilot
    val templateId: String? get() = raw.templateId
    val isCurrent: Boolean get() = raw.isCurrent

    fun update(block: AlarmProfileBuilder.() -> Unit): AlarmProfile {
        val builder = AlarmProfileBuilder().apply(block)
        val resp = http.patch(
            "/api/v1/alarm-presets/$residentId",
            builder.toRequest(),
            AlarmProfileResponse::class.java
        )
        return AlarmProfile(http, resp)
    }

    override fun toString(): String = "AlarmProfile($riskLevel, template=$templateId)"
}
