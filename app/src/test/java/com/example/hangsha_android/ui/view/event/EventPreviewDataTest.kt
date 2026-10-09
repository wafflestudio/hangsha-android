package com.example.hangsha_android.ui.view.event

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventPreviewDataTest {
    @Test
    fun timedEventConvertsUtcToKoreaTime() {
        assertEquals("행사 일정 · 2026.10.09 14:00–16:00", formatPreviewSchedule(
            "2026-10-09T05:00:00Z", "2026-10-09T07:00:00Z", null, null, false
        ))
    }

    @Test
    fun periodEventShowsApplicationRangeInsteadOfEventDate() {
        assertEquals("신청 기간 · 2026.10.01 – 2026.10.08", formatPreviewSchedule(
            "2026-10-09", "2026-10-09", "2026-10-01", "2026-10-08", true
        ))
    }

    @Test
    fun fallbackToEventDatesDoesNotMislabelThemAsApplicationDates() {
        assertEquals("행사 일정 · 2026.10.09 · 종일", formatPreviewSchedule(
            "2026-10-09", "2026-10-09", "bad", null, true
        ))
    }

    @Test
    fun endOnlyApplicationDateIsShownAsDeadline() {
        assertEquals("신청 기간 · 2026.10.08 18:00까지", formatPreviewSchedule(
            null, null, null, "2026-10-08T18:00:00", true
        ))
    }

    @Test
    fun fullDaySentinelDoesNotExpose2359AsAnEventEndTime() {
        assertEquals("행사 일정 · 2026.10.09 · 종일", formatPreviewSchedule(
            "2026-10-09T00:00:00", "2026-10-09T23:59:00", null, null, false
        ))
    }

    @Test
    fun crossYearRangeKeepsYearsVisible() {
        assertEquals("신청 기간 · 2026.12.28 – 2027.01.03", formatPreviewSchedule(
            null, null, "2026-12-28", "2027-01-03", true
        ))
    }

    @Test
    fun missingOrInvalidDatesProduceNoInventedSchedule() {
        assertNull(formatPreviewSchedule("", "invalid", null, null, false))
    }
}
