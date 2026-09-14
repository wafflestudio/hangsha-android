package com.example.hangsha_android.ui.view.timetable

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TimetableWeekViewConfigTest {
    @Test
    fun fiveDayView_startsOnMondayAndExcludesWeekendCourses() {
        val sunday = LocalDate.of(2026, 8, 16)

        assertEquals(LocalDate.of(2026, 8, 10), timetableFiveDayViewConfig.startOfWeek(sunday))
        assertEquals(0, timetableFiveDayViewConfig.columnForCourseWeekday(0))
        assertEquals(-1, timetableFiveDayViewConfig.columnForCourseWeekday(5))
        assertEquals(-1, timetableFiveDayViewConfig.columnForCourseWeekday(6))
    }

    @Test
    fun sevenDayView_startsOnSundayAndOrdersSundayThroughSaturday() {
        val wednesday = LocalDate.of(2026, 8, 19)

        assertEquals(LocalDate.of(2026, 8, 16), timetableSevenDayViewConfig.startOfWeek(wednesday))
        assertEquals(0, timetableSevenDayViewConfig.columnForCourseWeekday(6))
        assertEquals(1, timetableSevenDayViewConfig.columnForCourseWeekday(0))
        assertEquals(6, timetableSevenDayViewConfig.columnForCourseWeekday(5))
    }
}
