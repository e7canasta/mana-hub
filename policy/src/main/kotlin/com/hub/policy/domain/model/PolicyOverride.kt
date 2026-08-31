package com.hub.policy.domain.model

import com.hub.shared.domain.Identifier

/**
 * Lo que el director apartó del catálogo para un residente.
 *
 * Las tres variantes son **la misma clase de cosa** —una regla— y por eso todas
 * llevan los mismos tres campos comunes: si la regla habla, qué tan grave es y
 * cómo cierra el episodio. Lo único que las distingue es su dimensión de tiempo:
 * la permanencia se mide en minutos tolerados, el retorno en minutos de espera,
 * y la transición en cuánto hay que creerle al sensor.
 *
 * Antes sólo [ComeBackOverride] llevaba `severity` y `closureCondition`, y eso
 * era una asimetría sin razón: `ProfileStateRule` en los contratos de mana-hive
 * —lo que estos overrides sobreescriben— tiene las dos para todos los estados.
 * El resultado medido era que el panel mandaba la gravedad de una regla de
 * permanencia, el Hub respondía 200 y la tiraba.
 */
sealed interface PolicyOverride {
    val id: Identifier
    val ruleId: String

    /**
     * La regla se mira pero no habla.
     *
     * Es el `observeOnly` de `ProfileStateRule`, y es la forma que el perfil ya
     * tiene de apagar una regla sin borrarla: el estado se sigue observando y no
     * genera ningún aviso. Hacía falta un campo propio porque "sin plazo" ya
     * significa otra cosa —seguir al catálogo— así que no había manera de decir
     * "callala". Y una regla de permanencia **no es obligatoria**: que el
     * catálogo le ponga un tiempo no quiere decir que esta residencia la quiera.
     */
    val observeOnly: Boolean?

    val severity: String?
    val closureCondition: String?

    data class HysteresisOverride(
        override val id: Identifier,
        override val ruleId: String,
        val transitionKey: String,
        val hysteresisSeconds: Int,
        /* Una transición también puede alertar: entrar al piso no admite espera,
         * y el catálogo del motor lo declara con `alertOnEntry`. Sin gravedad ni
         * cierre, una transición quedaba como media regla. */
        override val severity: String? = null,
        override val closureCondition: String? = null,
        override val observeOnly: Boolean? = null,
    ) : PolicyOverride

    data class DwellOverride(
        override val id: Identifier,
        override val ruleId: String,
        val stateKind: String,
        val warningAfterMinutes: Int?,
        val alertAfterMinutes: Int?,
        override val severity: String? = null,
        override val closureCondition: String? = null,
        override val observeOnly: Boolean? = null,
    ) : PolicyOverride

    data class ComeBackOverride(
        override val id: Identifier,
        override val ruleId: String,
        val baselineState: String,
        val warningAfterMinutes: Int?,
        val alertAfterMinutes: Int?,
        /* Nullables a propósito: null significa que el director no habló de
         * esto y los valores del catálogo quedan en pie. Colapsarlos a un
         * default degradaría una regla CRITICAL a WARNING apenas alguien le
         * cambia un minuto. */
        override val severity: String? = null,
        override val closureCondition: String? = null,
        override val observeOnly: Boolean? = null,
    ) : PolicyOverride
}
