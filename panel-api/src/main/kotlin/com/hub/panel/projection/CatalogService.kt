package com.hub.panel.projection

import com.hub.panel.dto.*
import com.manahive.contracts.policy.MobilityAid
import com.manahive.contracts.policy.PolicyMode
import com.manahive.contracts.policy.RiskLevel
import org.springframework.stereotype.Service

/**
 * Catálogo fijo de preferencias de monitoreo.
 * El vocabulario de dominio es estático: si cambia, cambia código.
 * Los icon son códigos que el panel mapea a assets.
 */
@Service
class CatalogService {

    fun alarmCatalog(): AlarmCatalogDto = AlarmCatalogDto(
        levels = listOf(RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH),
        mobilityAids = listOf(MobilityAid.NONE, MobilityAid.WALKER, MobilityAid.WHEELCHAIR),
        transitions = TRANSITIONS,
        presets = PRESETS,
        riskFactors = RISK_FACTORS,
    )

    companion object {
        private val TRANSITIONS = listOf(
            TransitionCatalogDto(
                id = TransitionId.FALL, group = TransitionGroup.FALL_PREVENTION,
                label = "Caída detectada", shortLabel = "Caída",
                detail = "Siempre activa. No se puede desactivar.",
                icon = "fall", locked = true, requiresAid = null, params = emptyList(),
            ),
            TransitionCatalogDto(
                id = TransitionId.BED_EXIT, group = TransitionGroup.FALL_PREVENTION,
                label = "Se levanta de la cama", shortLabel = "Sale de cama",
                detail = "Precede a la mayoría de las caídas nocturnas.",
                icon = "bed_exit", locked = false, requiresAid = null, params = listOf(
                    ParamCatalogDto("warningMinutes", "duration", "Aviso a los", "min", 1, 30, 1),
                    ParamCatalogDto("alertMinutes", "duration", "Alerta a los", "min", 1, 60, 1),
                ),
            ),
            TransitionCatalogDto(
                id = TransitionId.WHEELCHAIR_EXIT, group = TransitionGroup.FALL_PREVENTION,
                label = "Se levanta de la silla de ruedas", shortLabel = "Sale de la silla",
                detail = "Sólo si usa silla de ruedas.",
                icon = "wheelchair_exit", locked = false, requiresAid = MobilityAid.WHEELCHAIR, params = emptyList(),
            ),
            TransitionCatalogDto(
                id = TransitionId.BATHROOM_DWELL, group = TransitionGroup.LOCATION,
                label = "Mucho tiempo en el baño", shortLabel = "Baño",
                detail = "Permanencia prolongada fuera de vista.",
                icon = "bathroom_dwell", locked = false, requiresAid = null, params = listOf(
                    ParamCatalogDto("warningMinutes", "duration", "Aviso a los", "min", 1, 30, 1),
                    ParamCatalogDto("alertMinutes", "duration", "Alerta a los", "min", 1, 60, 1),
                ),
            ),
            TransitionCatalogDto(
                id = TransitionId.ROOM_EXIT, group = TransitionGroup.LOCATION,
                label = "Sale de la habitación", shortLabel = "Sale de la hab.",
                detail = "Relevante en residentes con deambulación.",
                icon = "room_exit", locked = false, requiresAid = null, params = emptyList(),
            ),
            TransitionCatalogDto(
                id = TransitionId.SLEEP_DWELL, group = TransitionGroup.SLEEP,
                label = "Mucho tiempo dormido", shortLabel = "Dormido",
                detail = "Permanencia por encima de lo esperado.",
                icon = "sleep_dwell", locked = false, requiresAid = null, params = emptyList(),
            ),
            TransitionCatalogDto(
                id = TransitionId.BED_RAIL, group = TransitionGroup.ENVIRONMENT,
                label = "Baranda de la cama", shortLabel = "Baranda",
                detail = "Avisa si la baranda no está levantada.",
                icon = "bed_rail", locked = false, requiresAid = null, params = emptyList(),
            ),
            TransitionCatalogDto(
                id = TransitionId.WALKER_AID, group = TransitionGroup.ENVIRONMENT,
                label = "Andador del residente", shortLabel = "Andador",
                detail = "Sólo si usa andador.",
                icon = "walker_aid", locked = false, requiresAid = MobilityAid.WALKER, params = emptyList(),
            ),
        )

        private val PRESETS = mapOf(
            RiskLevel.LOW to mapOf(
                TransitionId.FALL to ShiftPresetDto(AlarmAction.ALARM, AlarmAction.ALARM),
                TransitionId.BED_EXIT to ShiftPresetDto(AlarmAction.OFF, AlarmAction.NOTIFY),
                TransitionId.WHEELCHAIR_EXIT to ShiftPresetDto(AlarmAction.NOTIFY, AlarmAction.NOTIFY),
                TransitionId.BED_RAIL to ShiftPresetDto(AlarmAction.OFF, AlarmAction.OFF),
                TransitionId.WALKER_AID to ShiftPresetDto(AlarmAction.OFF, AlarmAction.NOTIFY),
                TransitionId.BATHROOM_DWELL to ShiftPresetDto(AlarmAction.OFF, AlarmAction.NOTIFY),
                TransitionId.ROOM_EXIT to ShiftPresetDto(AlarmAction.OFF, AlarmAction.OFF),
                TransitionId.SLEEP_DWELL to ShiftPresetDto(AlarmAction.OFF, AlarmAction.OFF),
            ),
            RiskLevel.MEDIUM to mapOf(
                TransitionId.FALL to ShiftPresetDto(AlarmAction.ALARM, AlarmAction.ALARM),
                TransitionId.BED_EXIT to ShiftPresetDto(AlarmAction.NOTIFY, AlarmAction.ALARM),
                TransitionId.WHEELCHAIR_EXIT to ShiftPresetDto(AlarmAction.ALARM, AlarmAction.ALARM),
                TransitionId.BED_RAIL to ShiftPresetDto(AlarmAction.OFF, AlarmAction.NOTIFY),
                TransitionId.WALKER_AID to ShiftPresetDto(AlarmAction.NOTIFY, AlarmAction.ALARM),
                TransitionId.BATHROOM_DWELL to ShiftPresetDto(AlarmAction.NOTIFY, AlarmAction.NOTIFY),
                TransitionId.ROOM_EXIT to ShiftPresetDto(AlarmAction.OFF, AlarmAction.NOTIFY),
                TransitionId.SLEEP_DWELL to ShiftPresetDto(AlarmAction.OFF, AlarmAction.OFF),
            ),
            RiskLevel.HIGH to mapOf(
                TransitionId.FALL to ShiftPresetDto(AlarmAction.ALARM, AlarmAction.ALARM),
                TransitionId.BED_EXIT to ShiftPresetDto(AlarmAction.ALARM, AlarmAction.ALARM),
                TransitionId.WHEELCHAIR_EXIT to ShiftPresetDto(AlarmAction.ALARM, AlarmAction.ALARM),
                TransitionId.BED_RAIL to ShiftPresetDto(AlarmAction.NOTIFY, AlarmAction.ALARM),
                TransitionId.WALKER_AID to ShiftPresetDto(AlarmAction.ALARM, AlarmAction.ALARM),
                TransitionId.BATHROOM_DWELL to ShiftPresetDto(AlarmAction.ALARM, AlarmAction.ALARM),
                TransitionId.ROOM_EXIT to ShiftPresetDto(AlarmAction.NOTIFY, AlarmAction.ALARM),
                TransitionId.SLEEP_DWELL to ShiftPresetDto(AlarmAction.NOTIFY, AlarmAction.OFF),
            ),
        )

        private val RISK_FACTORS = listOf(
            RiskFactorCatalogDto("fall_history", "Caídas registradas", "fall"),
            RiskFactorCatalogDto("bed_exits", "Salidas de cama nocturnas", "bed_exit"),
            RiskFactorCatalogDto("wakeups", "Despertares nocturnos", "wakeups"),
            RiskFactorCatalogDto("bathroom", "Baños frecuentes", "bathroom"),
            RiskFactorCatalogDto("walker", "Usa andador", "walker"),
            RiskFactorCatalogDto("wheelchair", "Usa silla de ruedas", "wheelchair"),
            RiskFactorCatalogDto("deambulacion", "Deambulación", "deambulacion"),
        )
    }
}
