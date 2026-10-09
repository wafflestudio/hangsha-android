package com.example.hangsha_android.ui.view.calendar.week

import com.example.hangsha_android.ui.view.calendar.calendarEvent
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarWeekEventLayoutTest {
    private val sunday = LocalDate.of(2026, 10, 4)

    @Test
    fun weekOverflowPreservesServerPriorityAndStillSplitsTwoColumns() {
        val events = listOf(90L, 40L, 1L).map { id ->
            calendarEvent(id, start = "2026-10-04T10:00:00", end = "2026-10-04T12:00:00")
        } + calendarEvent(2, start = "2026-10-04T14:00:00", end = "2026-10-04T15:00:00")
        val mapped = CalendarWeekEventMapper.map(sunday, mapOf(sunday to events))
        val layout = positionWeekTimedEvents(sunday, mapped.timed)
        assertEquals(listOf(90L, 40L, 2L), layout.visible.map { it.event.eventId })
        assertEquals(listOf(2, 2, 1), layout.visible.map { it.laneCount })
        assertEquals(listOf(0, 1, 0), layout.visible.map { it.lane })
        assertEquals(1, layout.overflow.single().hiddenCount)
    }

    @Test
    fun dateSpecificServerOrderTakesPriorityOverOtherDates() {
        val first = calendarEvent(90, isPeriod = true,
            applyStart = "2026-10-04T10:00:00", applyEnd = "2026-10-06T12:00:00")
        val second = first.copy(id = 40)
        val third = first.copy(id = 1)
        val mapped = CalendarWeekEventMapper.map(sunday, linkedMapOf(
            sunday to listOf(first, second, third),
            sunday.plusDays(1) to listOf(third, second, first)
        ))
        val visible = positionWeekTimedEvents(sunday, mapped.timed).visible
        assertEquals(setOf(90L, 40L), visible.filter { it.dayIndex == 0 }.map { it.event.eventId }.toSet())
        assertEquals(setOf(1L, 40L), visible.filter { it.dayIndex == 1 }.map { it.event.eventId }.toSet())
    }
}
