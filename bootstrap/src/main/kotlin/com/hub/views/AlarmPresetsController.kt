package com.hub.views

import com.hub.policy.application.service.AlarmProfileApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/views/resident-chart/{residentId}")
class AlarmPresetsController(
    private val projectionService: ProjectionService,
    private val alarmProfileService: AlarmProfileApplicationService,
) {

    /** Presets de alarma — la configuración que le dice al sistema cómo monitorear. */
    @GetMapping("/alarm-presets")
    fun getAlarmPresets(@PathVariable residentId: String): ResponseEntity<AlarmPresetsProjection> {
        return ResponseEntity.ok(projectionService.getAlarmPresets(residentId))
    }

    /** Actualizar presets de alarma — la única escritura de la POC. */
    @PatchMapping("/alarm-presets")
    fun updateAlarmPresets(
        @PathVariable residentId: String,
        @RequestBody command: UpdateAlarmPresetsCommand,
    ): ResponseEntity<AlarmPresetsProjection> {
        alarmProfileService.updateResidentProfile(
            residentId,
            com.hub.policy.application.dto.UpdateAlarmProfileRequest(
                riskLevel = command.riskLevel,
                mobilityAid = command.mobilityAid,
                autopilot = command.autopilot,
                mode = command.mode,
                templateId = command.templateId,
                /* El comando trae `overrides` como objeto y el servicio los
                 * espera serializados. Antes este mapeo no existia: el campo se
                 * recibia, se ignoraba, y la respuesta 200 con `overrides: {}`
                 * hacia parecer que se habia guardado. */
                overridesJson = command.overrides?.let {
                    com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(it)
                },
                reason = command.reason,
                updatedBy = command.updatedBy,
            ),
        )
        return ResponseEntity.ok(projectionService.getAlarmPresets(residentId))
    }
}
