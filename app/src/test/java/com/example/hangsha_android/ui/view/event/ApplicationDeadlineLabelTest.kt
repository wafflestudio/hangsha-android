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

    @Test
    fun `future event shows event d-day from its start date`() {
        val eventStart = today.plusDays(10)

        assertEquals("행사 D-10", formatEventCountdownLabel(eventStart, eventStart, today))
    }

    @Test
    fun `event start date shows event d-day`() {
        assertEquals("행사 D-DAY", formatEventCountdownLabel(today, today.plusDays(2), today))
    }

    @Test
    fun `event between start and end dates shows in progress`() {
        assertEquals(
            "행사 진행중",
            formatEventCountdownLabel(today.minusDays(1), today.plusDays(1), today)
        )
    }

    @Test
    fun `event past its end date shows ended`() {
        assertEquals(
            "행사 종료",
            formatEventCountdownLabel(today.minusDays(2), today.minusDays(1), today)
        )
    }

    @Test
    fun `missing event start shows fallback`() {
        assertEquals("-", formatEventCountdownLabel(null, today.plusDays(1), today))
    }

    @Test
    fun `countdown resolver defaults to application label and toggles when both exist`() {
        val application = resolveCountdownLabel("지원 D-3", "행사 D-10", showEvent = false)
        val event = resolveCountdownLabel("지원 D-3", "행사 D-10", showEvent = true)

        assertEquals("지원 D-3", application.text)
        assertEquals(true, application.canToggle)
        assertEquals("행사 D-10", event.text)
        assertEquals(true, event.canToggle)
    }

    @Test
    fun `countdown resolver uses only available label without toggle`() {
        val eventOnly = resolveCountdownLabel("-", "행사 D-10", showEvent = false)
        val applicationOnly = resolveCountdownLabel("지원 D-3", "-", showEvent = true)

        assertEquals("행사 D-10", eventOnly.text)
        assertEquals(false, eventOnly.canToggle)
        assertEquals("지원 D-3", applicationOnly.text)
        assertEquals(false, applicationOnly.canToggle)
    }
}
