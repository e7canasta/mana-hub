package com.hub.history.domain.model

enum class InterventionKind {
    FAMILY_NOTIFIED,
    STAFF_DISPATCHED,
    BANDAGE_APPLIED,
    MEDICATION_GIVEN,
    TRANSFERRED_TO_HOSPITAL,
    REPOSITIONED,
    CALLED_FOR_HELP,
    VITAL_SIGNS_CHECKED,
    OTHER;

    companion object {
        fun from(value: String): InterventionKind = when (value.uppercase()) {
            "FAMILY_NOTIFIED" -> FAMILY_NOTIFIED
            "STAFF_DISPATCHED" -> STAFF_DISPATCHED
            "BANDAGE_APPLIED" -> BANDAGE_APPLIED
            "MEDICATION_GIVEN" -> MEDICATION_GIVEN
            "TRANSFERRED_TO_HOSPITAL" -> TRANSFERRED_TO_HOSPITAL
            "REPOSITIONED" -> REPOSITIONED
            "CALLED_FOR_HELP" -> CALLED_FOR_HELP
            "VITAL_SIGNS_CHECKED" -> VITAL_SIGNS_CHECKED
            "OTHER" -> OTHER
            else -> OTHER
        }
    }
}
