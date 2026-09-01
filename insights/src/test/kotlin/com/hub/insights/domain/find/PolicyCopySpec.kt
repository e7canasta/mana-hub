package com.hub.insights.domain.find

import com.hub.insights.inbound.HubOverrideEntry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * PolicyCopy — las reglas de alarma traducidas a lenguaje hablado.
 *
 * Cada nivel de riesgo tiene un catálogo de estados con tiempos de aviso.
 * Los overrides del director reemplazan los defaults del catálogo.
 * El lenguaje resultante es lo que aparece en el panel del director médico.
 */
class PolicyCopySpec {

    @Nested
    inner class `Nombre del nivel de riesgo` {

        @Test
        fun `riesgo de caida se llama Riesgo de caída`() {
            assertThat(PolicyCopy.levelLabel("high")).isEqualTo("Riesgo de caída")
        }

        @Test
        fun `fall_risk tambien es Riesgo de caída`() {
            assertThat(PolicyCopy.levelLabel("FALL_RISK")).isEqualTo("Riesgo de caída")
        }

        @Test
        fun `deambulacion nocturna`() {
            assertThat(PolicyCopy.levelLabel("medium")).isEqualTo("Deambulación nocturna")
        }

        @Test
        fun `night_wandering es deambulacion nocturna`() {
            assertThat(PolicyCopy.levelLabel("NIGHT_WANDERING")).isEqualTo("Deambulación nocturna")
        }

        @Test
        fun `vigilancia baja`() {
            assertThat(PolicyCopy.levelLabel("low")).isEqualTo("Vigilancia baja")
        }

        @Test
        fun `critico`() {
            assertThat(PolicyCopy.levelLabel("critical")).isEqualTo("Crítico")
        }

        @Test
        fun `nivel desconocido se devuelve tal cual`() {
            assertThat(PolicyCopy.levelLabel("custom_level")).isEqualTo("custom_level")
        }

        @Test
        fun `nivel nulo devuelve nulo`() {
            assertThat(PolicyCopy.levelLabel(null)).isNull()
        }
    }

    @Nested
    inner class `Reglas habladas por nivel` {

        @Test
        fun `riesgo de caida usa el catalogo fallRisk`() {
            val rules = PolicyCopy.spokenRules("high", emptyMap())
            val bedEdge = rules.single { it.state == "BED_EDGE" }
            assertThat(bedEdge.warningAfterMinutes).isEqualTo(1)
            assertThat(bedEdge.alertAfterMinutes).isEqualTo(2)
            assertThat(bedEdge.manual).isFalse()
        }

        @Test
        fun `deambulacion nocturna usa nightWandering`() {
            val rules = PolicyCopy.spokenRules("medium", emptyMap())
            val sitting = rules.single { it.state == "SITTING_IN_BED" }
            assertThat(sitting.warningAfterMinutes).isEqualTo(20)
            assertThat(sitting.alertAfterMinutes).isEqualTo(30)
        }

        @Test
        fun `critico usa critical`() {
            val rules = PolicyCopy.spokenRules("critical", emptyMap())
            val absent = rules.single { it.state == "ABSENT" }
            assertThat(absent.warningAfterMinutes).isEqualTo(2)
            assertThat(absent.alertAfterMinutes).isEqualTo(5)
        }

        @Test
        fun `nivel sin catalogo devuelve reglas sin tiempos`() {
            val rules = PolicyCopy.spokenRules("custom", emptyMap())
            assertThat(rules).hasSize(4)
            assertThat(rules.all { it.warningAfterMinutes == null }).isTrue()
        }

        @Test
        fun `siempre hay 4 reglas - una por estado mostrado`() {
            val rules = PolicyCopy.spokenRules("high", emptyMap())
            assertThat(rules.map { it.state }).containsExactly(
                "SITTING_IN_BED", "BED_EDGE", "STANDING", "ABSENT",
            )
        }
    }

    @Nested
    inner class `Override del director` {

        @Test
        fun `override de BED_EDGE reemplaza el default`() {
            val overrides = mapOf(
                "BED_EDGE" to HubOverrideEntry(warningAfterMinutes = 3, alertAfterMinutes = 5),
            )
            val rules = PolicyCopy.spokenRules("high", overrides)
            val bedEdge = rules.single { it.state == "BED_EDGE" }
            assertThat(bedEdge.warningAfterMinutes).isEqualTo(3)
            assertThat(bedEdge.alertAfterMinutes).isEqualTo(5)
            assertThat(bedEdge.manual).isTrue()
        }

        @Test
        fun `override parcial solo cambia lo indicado`() {
            val overrides = mapOf(
                "FALL_RISK_SITTING" to HubOverrideEntry(warningAfterMinutes = 10, alertAfterMinutes = null),
            )
            val rules = PolicyCopy.spokenRules("high", overrides)
            val sitting = rules.single { it.state == "SITTING_IN_BED" }
            assertThat(sitting.warningAfterMinutes).isEqualTo(10)
            assertThat(sitting.alertAfterMinutes).isEqualTo(20) // fallback al catalogo base
            assertThat(sitting.manual).isTrue()
            // Los demas estados no cambian
            val bedEdge = rules.single { it.state == "BED_EDGE" }
            assertThat(bedEdge.warningAfterMinutes).isEqualTo(1)
            assertThat(bedEdge.manual).isFalse()
        }

        @Test
        fun `override con UNKNOWN afecta ABSENT`() {
            val overrides = mapOf(
                "UNKNOWN" to HubOverrideEntry(warningAfterMinutes = 1, alertAfterMinutes = 2),
            )
            val rules = PolicyCopy.spokenRules("high", overrides)
            val absent = rules.single { it.state == "ABSENT" }
            assertThat(absent.warningAfterMinutes).isEqualTo(1)
            assertThat(absent.manual).isTrue()
        }
    }

    @Nested
    inner class `Lineas habladas - formato legible` {

        @Test
        fun `regla con aviso y escala`() {
            val rules = PolicyCopy.spokenRules("high", emptyMap())
            val bedEdge = rules.single { it.state == "BED_EDGE" }
            val line = bedEdge.line()
            assertThat(line).contains("Al borde de la cama:")
            assertThat(line).contains("avisa a los 1 min")
            assertThat(line).contains("escala a los 2")
        }

        @Test
        fun `regla sin aviso de permanencia`() {
            val rules = PolicyCopy.spokenRules("custom", emptyMap())
            val sitting = rules.single { it.state == "SITTING_IN_BED" }
            val line = sitting.line()
            assertThat(line).contains("sin aviso de permanencia")
        }

        @Test
        fun `regla con ajuste manual`() {
            val overrides = mapOf(
                "BED_EDGE" to HubOverrideEntry(warningAfterMinutes = 1, alertAfterMinutes = 3),
            )
            val rules = PolicyCopy.spokenRules("high", overrides)
            val bedEdge = rules.single { it.state == "BED_EDGE" }
            assertThat(bedEdge.line()).contains("Ajuste manual")
        }

        @Test
        fun `spokenLines devuelve todas las lineas`() {
            val lines = PolicyCopy.spokenLines("high", emptyMap())
            assertThat(lines).hasSize(4)
            assertThat(lines.any { it.contains("Sentado en cama") }).isTrue()
            assertThat(lines.any { it.contains("Al borde de la cama") }).isTrue()
            assertThat(lines.any { it.contains("De pie") }).isTrue()
            assertThat(lines.any { it.contains("Sin observación") }).isTrue()
        }
    }

    @Nested
    inner class `Tiempo de aviso en el borde de la cama` {

        @Test
        fun `riesgo de caida default es 1 minuto`() {
            assertThat(PolicyCopy.bedEdgeWarningMinutes("high", emptyMap())).isEqualTo(1)
        }

        @Test
        fun `override cambia el tiempo`() {
            val overrides = mapOf(
                "BED_EDGE" to HubOverrideEntry(warningAfterMinutes = 3, alertAfterMinutes = 5),
            )
            assertThat(PolicyCopy.bedEdgeWarningMinutes("high", overrides)).isEqualTo(3)
        }

        @Test
        fun `nivel sin catalogo devuelve null`() {
            assertThat(PolicyCopy.bedEdgeWarningMinutes("custom", emptyMap())).isNull()
        }
    }
}
