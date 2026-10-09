package com.example.hangsha_android.ui.view.calendar

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarEventTimeTest {
    @Test
    fun offsetAndRegionTimesUseSeoulDateAcrossMidnight() {
        val expected = LocalDateTime.of(2026, 10, 5, 1, 0)
        listOf("2026-10-04T16:00:00Z", "2026-10-04T12:00:00-04:00[America/New_York]").forEach {
            val parsed = requireNotNull(parseCalendarEventTime(it))
            assertEquals(expected, parsed.dateTime)
            assertFalse(parsed.isDateOnly)
            assertEquals(expected.toLocalDate(), parseCalendarEventDate(it))
        }
    }

    @Test
    fun dateOnlyAndLocalMidnightRemainDistinguishable() {
        val date = requireNotNull(parseCalendarEventTime(" 2026-10-04 "))
        val midnight = requireNotNull(parseCalendarEventTime("2026-10-04T00:00:00"))
        assertEquals(date.dateTime, midnight.dateTime)
        assertTrue(date.isDateOnly)
        assertFalse(midnight.isDateOnly)
    }

    @Test
    fun localDateTimeKeepsItsWallClockTime() {
        assertEquals(LocalDateTime.of(2026, 10, 4, 10, 30),
            parseCalendarEventTime("2026-10-04T10:30:00")?.dateTime)
    }

    @Test
    fun missingAndMalformedValuesAreAbsent() {
        listOf(null, "", " ", "not a date", "2026-02-30").forEach {
            assertNull(parseCalendarEventTime(it))
        }
    }

    @Test
    fun periodPrefersApplicationRangeAndOtherEventsPreferEventRange() {
        val event = calendarEvent(
            start = "2026-10-04T10:00:00", end = "2026-10-04T18:00:00",
            applyStart = "2026-10-01", applyEnd = "2026-10-03"
        )
        assertEquals(LocalDate.of(2026, 10, 4), event.timelineRange()?.start?.dateTime?.toLocalDate())
        assertEquals(LocalDate.of(2026, 10, 1),
            event.copy(isPeriodEvent = true).timelineRange()?.start?.dateTime?.toLocalDate())
    }

    @Test
    fun incompletePrimaryRangeFallsBackToCompleteSecondaryRange() {
        val event = calendarEvent(
            start = "2026-10-04T10:00:00", end = "2026-10-04T18:00:00",
            isPeriod = true, applyStart = "2026-10-01"
        )
        assertEquals(10, event.timelineRange()?.start?.dateTime?.hour)
        assertNull(event.copy(eventEnd = null).timelineRange())
    }
}
