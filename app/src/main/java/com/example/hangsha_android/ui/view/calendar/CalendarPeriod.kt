package com.example.hangsha_android.ui.view.calendar

import com.example.hangsha_android.data.repository.model.EventDateRange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class CalendarPeriod {
    DAY,
    WEEK,
    MONTH;

    fun visibleRange(anchorDate: LocalDate): EventDateRange {
        return when (this) {
            DAY -> EventDateRange(anchorDate, anchorDate)
            WEEK -> weekRange(anchorDate)
            MONTH -> YearMonth.from(anchorDate).toCalendarGridRange()
        }
    }

    fun contentRange(anchorDate: LocalDate): EventDateRange {
        return when (this) {
            DAY -> EventDateRange(anchorDate, anchorDate)
            WEEK -> weekRange(anchorDate)
            MONTH -> YearMonth.from(anchorDate).let { month ->
                EventDateRange(month.atDay(1), month.atEndOfMonth())
            }
        }
    }

    fun move(anchorDate: LocalDate, amount: Long): LocalDate {
        return when (this) {
            DAY -> anchorDate.plusDays(amount)
            WEEK -> anchorDate.plusWeeks(amount)
            MONTH -> anchorDate.plusMonths(amount)
        }
    }

    private fun weekRange(anchorDate: LocalDate): EventDateRange {
        val start = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        return EventDateRange(from = start, to = start.plusDays(6))
    }
}
