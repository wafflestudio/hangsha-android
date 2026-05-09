package com.example.hangsha_android.ui.view.event

import androidx.compose.ui.graphics.Color

val eventYellow = Color(0x99FFCC00)
val eventGreen = Color(0x990BCE84)
val eventSkyBlue = Color(0x9900C0E8)
val eventBlue = Color(0x990091FF)
val eventPurple = Color(0x99A25AFF)
val eventPink = Color(0x80FF2D55)

fun eventTypeColor(eventTypeId: Long): Color {
    return when (eventTypeId) {
        4L -> eventYellow
        5L -> eventGreen
        6L -> eventSkyBlue
        7L -> eventBlue
        8L -> eventPurple
        else -> eventPink
    }
}
