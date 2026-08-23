package com.example.hangsha_android.ui.view.timetable

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

internal enum class TimetableTimelineEventKind {
    PERIOD,
    ALL_DAY
}

internal data class TimetableTimelineEventItem(
    val id: String,
    val eventId: Long,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val categoryColor: Color,
    val kind: TimetableTimelineEventKind
)

internal data class TimetableWeekEvents(
    val timed: List<TimetableEventItem> = emptyList(),
    val period: List<TimetableTimelineEventItem> = emptyList(),
    val allDay: List<TimetableTimelineEventItem> = emptyList()
)
