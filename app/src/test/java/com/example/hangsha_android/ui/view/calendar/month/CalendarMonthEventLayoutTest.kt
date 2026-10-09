package com.example.hangsha_android.ui.view.calendar.month

import com.example.hangsha_android.ui.view.calendar.calendarEvent
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarMonthEventLayoutTest {
    private val sunday = LocalDate.of(2026, 10, 4)

    @Test
    fun monthOverflowCountsOnlyHiddenEventsOnTheirDates() {
        val events = (1L..5L).map { calendarEvent(it, start = "2026-10-04", end = "2026-10-06") }
        val layout = CalendarMonthEventLayoutCalculator.positionWeek(
            CalendarMonthEventMapper.map(mapOf(sunday to events)), sunday
        )
        assertEquals(4, layout.visibleEvents.size)
        assertEquals(listOf(1, 1, 1, 0, 0, 0, 0), layout.overflowByDay)
    }
}
