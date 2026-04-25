package com.example.hangsha_android.ui.view.calendar

import com.example.hangsha_android.data.repository.model.EventDateRange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

fun YearMonth.toCalendarGridRange(): EventDateRange {
    val firstOfMonth = atDay(1)
    val lastOfMonth = atEndOfMonth()
    return EventDateRange(
        from = firstOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)),
        to = lastOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
    )
}

fun EventDateRange.toDateList(): List<LocalDate> {
    return generateSequence(from) { current ->
        current.plusDays(1).takeIf { !it.isAfter(to) }
    }.toList()
}
