package com.hub.insights.domain.derive

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BaselineServiceTest {

    private val today = LocalDate.of(2026, 8, 31)
    private val from = LocalDate.of(2026, 8, 18)
    private val to = LocalDate.of(2026, 8, 31)

    @Test
    fun `Jose con admision antigua tiene baseline ready`() {
        val b = BaselineService.of(
            admissionDate = LocalDate.of(2024, 1, 15),
            from = from,
            to = to,
            today = today,
        )
        assertThat(b.ready).isTrue()
        assertThat(b.observedFrom).isEqualTo(from)
    }

    @Test
    fun `Susan alta ayer recorta observedFrom y no esta ready`() {
        val admission = LocalDate.of(2026, 8, 30)
        val b = BaselineService.of(
            admissionDate = admission,
            from = from,
            to = to,
            today = today,
        )
        assertThat(b.ready).isFalse()
        assertThat(b.observedFrom).isEqualTo(admission)
        assertThat(b.observedDays).isEqualTo(1)
    }
}
