package com.example.hangsha_android.ui.view.calendar

import com.example.hangsha_android.ui.view.calendar.day.CalendarDayEventMapper
import com.example.hangsha_android.ui.view.calendar.month.CalendarMonthEventMapper
import com.example.hangsha_android.ui.view.calendar.week.CalendarWeekEventMapper
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarEventMappingTest {
    private val sunday = LocalDate.of(2026, 10, 4)

    @Test
    fun multiDayPeriodKeepsDailyOpeningHoursInDayAndWeekViews() {
        val event = calendarEvent(isPeriod = true,
            applyStart = "2026-10-04T10:00:00", applyEnd = "2026-10-10T18:00:00")
        val week = CalendarWeekEventMapper.map(sunday, mapOf(sunday to listOf(event)))
        assertEquals(7, week.timed.size)
        assertTrue(week.allDay.isEmpty())
        week.timed.forEach {
            val day = CalendarDayEventMapper.map(it.date, listOf(event)).timed.single()
            assertEquals(600, day.startMinute)
            assertEquals(1080, day.endMinute)
            assertEquals(day.startMinute, it.startMinute)
            assertEquals(day.endMinute, it.endMinute)
        }
    }

    @Test
    fun midnightEndDoesNotCreateAnEventOnTheNextDay() {
        val event = calendarEvent(start = "2026-10-04T22:00:00", end = "2026-10-05T00:00:00")
        val week = CalendarWeekEventMapper.map(sunday, mapOf(sunday to listOf(event)))
        assertEquals(1440, week.timed.single().endMinute)
        val nextDay = CalendarDayEventMapper.map(sunday.plusDays(1), listOf(event))
        assertTrue(nextDay.timed.isEmpty())
        assertTrue(nextDay.allDay.isEmpty())
    }

    @Test
    fun fullDayServerTimesStayInAllDayLane() {
        val event = calendarEvent(start = "2026-10-04T00:00:00", end = "2026-10-04T23:59:00")
        assertEquals(1, CalendarDayEventMapper.map(sunday, listOf(event)).allDay.size)
        val week = CalendarWeekEventMapper.map(sunday, mapOf(sunday to listOf(event)))
        assertEquals(1, week.allDay.size)
        assertTrue(week.timed.isEmpty())
    }

    @Test
    fun dateOnlyRangeKeepsInclusiveEndInMonthAndWeekViews() {
        val event = calendarEvent(start = "2026-10-04", end = "2026-10-06")
        val events = mapOf(sunday to listOf(event))
        val month = CalendarMonthEventMapper.map(events).single()
        val week = CalendarWeekEventMapper.map(sunday, events).allDay.single()
        assertEquals(sunday.plusDays(2), month.endDate)
        assertEquals(month.endDate, week.endDate)
        assertEquals(1, CalendarDayEventMapper.map(sunday, listOf(event)).allDay.size)
    }
}
