package com.example.hangsha_android.ui.view.timetable

import androidx.compose.runtime.Composable

@Composable
fun TimetableScreen(onEventClick: (Long) -> Unit) {
    TimetableScreenContentHost(onEventClick = onEventClick)
}