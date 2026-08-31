package com.hub.panel.controller

import com.hub.panel.command.PanelCommandService
import com.hub.panel.projection.CatalogService
import com.hub.panel.projection.PanelProjectionService
import com.hub.shared.panel.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController("panelPreferenceController")
@RequestMapping("/api/v1/panel/preferences")
class PanelPreferenceController(
    private val projection: PanelProjectionService,
    private val command: PanelCommandService,
    private val catalog: CatalogService,
) {

    @GetMapping("/catalog")
    fun catalog(): AlarmCatalogDto = catalog.alarmCatalog()

    @GetMapping
    fun list(): List<PreferenceListItemDto> = projection.preferenceList()

    @GetMapping("/{residentId}")
    fun detail(@PathVariable residentId: String): ResponseEntity<PreferenceFullDto> =
        projection.preferenceFull(residentId)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PostMapping("/{residentId}/save")
    fun save(
        @PathVariable residentId: String,
        @RequestBody body: SavePreferencesRequest,
    ): ResponseEntity<SavePreferencesResponse> {
        val result = command.savePreferences(
            residentId = residentId,
            riskLevel = body.riskLevel,
            mobilityAid = body.mobilityAid,
            autopilot = body.autopilot,
            mode = body.mode,
            templateId = body.templateId,
            overrides = body.overrides,
            reason = body.reason,
            updatedBy = body.updatedBy,
        )
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{residentId}/recommendations")
    fun recommendations(@PathVariable residentId: String): ResponseEntity<RecommendationListResponse> {
        return ResponseEntity.ok(RecommendationListResponse(recommendations = emptyList()))
    }
}
