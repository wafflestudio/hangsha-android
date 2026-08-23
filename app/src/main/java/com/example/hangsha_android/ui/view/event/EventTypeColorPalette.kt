package com.example.hangsha_android.ui.view.event

import androidx.compose.ui.graphics.Color

private val WebEventOrange = Color(0x99FF8C28)
private val WebEventGold = Color(0x99BA9E31)
private val WebEventGreen = Color(0x990BCE83)
private val WebEventSkyBlue = Color(0x9900C1E8)
private val WebEventBlue = Color(0x990088FF)
private val WebEventPurple = Color(0x99A25AFF)
private val WebEventPink = Color(0x99FF2D53)

private val WebFilterOrange = Color(0x26FF8C28)
private val WebFilterYellow = Color(0x26FFCC00)
private val WebFilterGreen = Color(0x260BCE83)
private val WebFilterSkyBlue = Color(0x2600C1E8)
private val WebFilterBlue = Color(0x260088FF)
private val WebFilterPurple = Color(0x26A25AFF)
private val WebFilterPink = Color(0x26FF2D53)

fun eventTypeColor(eventTypeId: Long?): Color {
    return when (eventTypeId) {
        1L -> WebEventOrange
        2L -> WebEventGold
        3L -> WebEventGreen
        4L -> WebEventSkyBlue
        5L -> WebEventBlue
        6L -> WebEventPurple
        7L -> WebEventPink
        else -> WebEventPink
    }
}

fun eventTypeFilterColor(eventTypeId: Long): Color {
    return when (eventTypeId) {
        1L -> WebFilterOrange
        2L -> WebFilterYellow
        3L -> WebFilterGreen
        4L -> WebFilterSkyBlue
        5L -> WebFilterBlue
        6L -> WebFilterPurple
        7L -> WebFilterPink
        else -> WebFilterPink
    }
}
