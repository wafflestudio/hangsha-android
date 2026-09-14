package com.example.hangsha_android.ui.view.timetable

import java.time.DayOfWeek
import java.time.LocalDate

internal data class TimetableWeekViewConfig(
    val visibleDays: List<TimetableDayOfWeek>,
    val firstDayOfWeek: DayOfWeek
) {
    val dayCount: Int
        get() = visibleDays.size

    fun startOfWeek(date: LocalDate): LocalDate {
        val daysFromStart = (date.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        return date.minusDays(daysFromStart.toLong())
    }

    fun columnForCourseWeekday(weekday: Int): Int {
        return visibleDays.indexOfFirst { day -> day.weekday == weekday }
    }
}
