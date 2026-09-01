package com.hub.insights.domain.find

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalTime

/**
 * CopyFormat — el vocabulario visual del sistema.
 *
 * Cada método transforma un número crudo en algo que un director
 * médico pueda leer en un panel sin traducir.
 */
class CopyFormatSpec {

    @Nested
    inner class `Presentar tiempo en minutos` {

        @Test
        fun `minutos puros sin horas`() {
            assertThat(CopyFormat.clock(45)).isEqualTo("45 min")
        }

        @Test
        fun `horas exactas sin minutos`() {
            assertThat(CopyFormat.clock(120)).isEqualTo("2h")
        }

        @Test
        fun `horas y minutos combinados`() {
            assertThat(CopyFormat.clock(97)).isEqualTo("1h 37")
        }

        @Test
        fun `cero minutos`() {
            assertThat(CopyFormat.clock(0)).isEqualTo("0 min")
        }

        @Test
        fun `valores negativos se tratan como absolutos`() {
            assertThat(CopyFormat.clock(-30)).isEqualTo("30 min")
        }
    }

    @Nested
    inner class `Presentar porcentajes` {

        @Test
        fun `proporción se convierte a porcentaje entero`() {
            assertThat(CopyFormat.percent(0.283)).isEqualTo("28%")
        }

        @Test
        fun `cero por ciento`() {
            assertThat(CopyFormat.percent(0.0)).isEqualTo("0%")
        }

        @Test
        fun `cien por ciento`() {
            assertThat(CopyFormat.percent(1.0)).isEqualTo("100%")
        }

        @Test
        fun `redondeo arriba`() {
            assertThat(CopyFormat.percent(0.286)).isEqualTo("29%")
        }
    }

    @Nested
    inner class `Presentar un decimal` {

        @Test
        fun `un decimal con punto`() {
            assertThat(CopyFormat.oneDecimal(2.7)).isEqualTo("2.7")
        }

        @Test
        fun `cero decimal`() {
            assertThat(CopyFormat.oneDecimal(0.0)).isEqualTo("0.0")
        }

        @Test
        fun `redondeo a un decimal`() {
            assertThat(CopyFormat.oneDecimal(2.71828)).isEqualTo("2.7")
        }
    }

    @Nested
    inner class `Extraer nombre de pila` {

        @Test
        fun `nombre completo devuelve el primero`() {
            assertThat(CopyFormat.firstName("María López")).isEqualTo("María")
        }

        @Test
        fun `nombre de una sola palabra`() {
            assertThat(CopyFormat.firstName("Susan")).isEqualTo("Susan")
        }

        @Test
        fun `nombre nulo devuelve nulo`() {
            assertThat(CopyFormat.firstName(null)).isNull()
        }

        @Test
        fun `nombre vacío o en blanco devuelve nulo`() {
            assertThat(CopyFormat.firstName("")).isNull()
            assertThat(CopyFormat.firstName("   ")).isNull()
        }

        @Test
        fun `ignora espacios múltiples`() {
            assertThat(CopyFormat.firstName("  Juan  Carlos  Pérez")).isEqualTo("Juan")
        }
    }

    @Nested
    inner class `Expresar frecuencia` {

        @Test
        fun `una vez`() {
            assertThat(CopyFormat.veces(1)).isEqualTo("una vez")
        }

        @Test
        fun `dos veces`() {
            assertThat(CopyFormat.veces(2)).isEqualTo("dos veces")
        }

        @Test
        fun `tres veces`() {
            assertThat(CopyFormat.veces(3)).isEqualTo("tres veces")
        }

        @Test
        fun `cinco veces`() {
            assertThat(CopyFormat.veces(5)).isEqualTo("cinco veces")
        }

        @Test
        fun `mas de cinco usa el numero`() {
            assertThat(CopyFormat.veces(12)).isEqualTo("12 veces")
        }

        @Test
        fun `cero`() {
            assertThat(CopyFormat.veces(0)).isEqualTo("0 veces")
        }
    }

    @Nested
    inner class `Presentar hora del reloj` {

        @Test
        fun `hora exacta con cero minutos`() {
            assertThat(CopyFormat.clockTime(LocalTime.of(5, 0))).isEqualTo("5:00")
        }

        @Test
        fun `hora y minutos`() {
            assertThat(CopyFormat.clockTime(LocalTime.of(14, 30))).isEqualTo("14:30")
        }

        @Test
        fun `una cifra`() {
            assertThat(CopyFormat.clockTime(LocalTime.of(6, 5))).isEqualTo("6:05")
        }
    }
}
