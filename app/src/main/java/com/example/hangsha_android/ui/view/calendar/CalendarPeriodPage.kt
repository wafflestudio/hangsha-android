package com.example.hangsha_android.ui.view.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class CalendarPageKey(
    val period: CalendarPeriod,
    val startDate: LocalDate
) {
    companion object {
        fun from(period: CalendarPeriod, anchorDate: LocalDate): CalendarPageKey {
            val startDate = when (period) {
                CalendarPeriod.MONTH -> YearMonth.from(anchorDate).atDay(1)
                CalendarPeriod.WEEK -> anchorDate.with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)
                )
                CalendarPeriod.DAY -> anchorDate
            }
            return CalendarPageKey(period, startDate)
        }
    }
}

data class CalendarPeriodPage(
    val filterSourceEventsByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
    val eventsByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
