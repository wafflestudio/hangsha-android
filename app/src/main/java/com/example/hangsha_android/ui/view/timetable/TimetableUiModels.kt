package com.example.hangsha_android.ui.view.timetable

import androidx.compose.ui.graphics.Color

internal data class TimetableUiModel(
    val id: String,
    val name: String,
    val year: Int,
    val semester: String,
    val courses: List<CourseUiModel>
) {
    val totalCredits: Int
        get() = courses.sumOf { course -> course.credit ?: 0 }
}

internal data class CourseUiModel(
    val id: String,
    val title: String,
    val instructor: String?,
    val credit: Int?,
    val color: Color,
    val timeSlots: List<CourseTimeSlot>
) {
    val subtitle: String?
        get() = instructor?.takeIf { it.isNotBlank() }
}

internal data class CourseTimeSlot(
    val weekday: Int,
    val startMinute: Int,
    val endMinute: Int
)

internal data class TimetableEventItem(
    val id: String,
    val eventId: Long,
    val title: String,
    val weekday: Int,
    val startMinute: Int,
    val endMinute: Int,
    val categoryColor: Color
)

