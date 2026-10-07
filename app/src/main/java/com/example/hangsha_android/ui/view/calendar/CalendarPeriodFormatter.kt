package com.example.hangsha_android.ui.view.calendar

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MonthTitleFormatter =
    DateTimeFormatter.ofPattern("yyyy'년' M'월'", Locale.KOREAN)
private val DayTitleFormatter =
    DateTimeFormatter.ofPattern("yyyy'년' M'월' d'일'", Locale.KOREAN)
private val WeekStartTitleFormatter =
    DateTimeFormatter.ofPattern("yyyy'년' M'월' d'일'", Locale.KOREAN)
private val WeekEndSameMonthFormatter =
    DateTimeFormatter.ofPattern("d'일'", Locale.KOREAN)
private val WeekEndDifferentMonthFormatter =
    DateTimeFormatter.ofPattern("M'월' d'일'", Locale.KOREAN)

internal fun CalendarPeriod.headerTitle(anchorDate: LocalDate): String {
    return when (this) {
        CalendarPeriod.DAY -> anchorDate.format(DayTitleFormatter)
        CalendarPeriod.MONTH -> YearMonth.from(anchorDate).format(MonthTitleFormatter)
        CalendarPeriod.WEEK -> {
            val range = contentRange(anchorDate)
            val endFormatter = if (YearMonth.from(range.from) == YearMonth.from(range.to)) {
                WeekEndSameMonthFormatter
            } else {
                WeekEndDifferentMonthFormatter
            }
            "${range.from.format(WeekStartTitleFormatter)} - ${range.to.format(endFormatter)}"
        }
    }
}
