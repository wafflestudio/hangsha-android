package com.example.hangsha_android.ui.view.timetable

import java.time.DayOfWeek

internal val timetableSevenDayViewConfig = TimetableWeekViewConfig(
    visibleDays = listOf(
        TimetableDayOfWeek.SUN,
        TimetableDayOfWeek.MON,
        TimetableDayOfWeek.TUE,
        TimetableDayOfWeek.WED,
        TimetableDayOfWeek.THU,
        TimetableDayOfWeek.FRI,
        TimetableDayOfWeek.SAT
    ),
    firstDayOfWeek = DayOfWeek.SUNDAY
)
