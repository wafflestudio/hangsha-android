package com.example.hangsha_android.ui.view.event

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplicationDeadlineLabelTest {

    private val today = LocalDate.of(2026, 9, 6)

    @Test
    fun `future deadline shows application d-day`() {
        val deadline = today.plusDays(10)

        assertEquals("지원 D-10", formatApplicationDeadlineLabel(deadline, today))
    }

    @Test
    fun `today deadline shows application d-day`() {
        assertEquals("지원 D-DAY", formatApplicationDeadlineLabel(today, today))
    }

    @Test
    fun `past deadline shows application closed`() {
        val deadline = today.minusDays(10)

        assertEquals("지원 마감", formatApplicationDeadlineLabel(deadline, today))
    }

    @Test
    fun `missing deadline shows fallback`() {
        assertEquals("-", formatApplicationDeadlineLabel(null, today))
    }
}
