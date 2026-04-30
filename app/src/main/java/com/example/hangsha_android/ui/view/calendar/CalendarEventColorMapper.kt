package com.example.hangsha_android.ui.view.calendar

import androidx.compose.ui.graphics.Color
import java.util.Locale

internal object CalendarEventColorMapper {
    val dayRed = Color(0xFFFF2D55) // 일요일 빨간색

    val eventYellow = Color(0xFFF6DC6E)
    val eventPurple = Color(0xFFB79AF5)
    val eventBlue = Color(0xFF5DB8F6)
    val eventGreen = Color(0xFF62D0AB)
    val eventPink = Color(0xFFF3A4BC)

    fun colorFor(event: CalendarEvent): Color {
        if (event.isBookmarked) return eventPink
        if (event.isInterested) return eventPurple

        val mode = event.operationMode.lowercase(Locale.ROOT)
        return when {
            "offline" in mode -> eventGreen
            "online" in mode -> eventBlue
            event.statusId % 3L == 0L -> eventPurple
            event.eventTypeId % 4L == 0L -> eventBlue
            event.orgId % 5L == 0L -> eventGreen
            else -> eventYellow
        }
    }
}
