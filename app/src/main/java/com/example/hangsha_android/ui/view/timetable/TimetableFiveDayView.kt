package com.example.hangsha_android.ui.view.timetable

import java.time.DayOfWeek

internal val timetableFiveDayViewConfig = TimetableWeekViewConfig(
    visibleDays = listOf(
        TimetableDayOfWeek.MON,
        TimetableDayOfWeek.TUE,
        TimetableDayOfWeek.WED,
        TimetableDayOfWeek.THU,
        TimetableDayOfWeek.FRI
    ),
    firstDayOfWeek = DayOfWeek.MONDAY
)
