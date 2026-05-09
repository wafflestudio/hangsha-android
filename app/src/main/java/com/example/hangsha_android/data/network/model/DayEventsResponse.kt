package com.example.hangsha_android.data.network.model

data class DayEventsResponse(
    val page: Int,
    val size: Int,
    val total: Int,
    val date: String,
    val items: List<EventSummaryResponse>
)
