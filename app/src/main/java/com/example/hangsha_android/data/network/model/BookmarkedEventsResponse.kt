package com.example.hangsha_android.data.network.model

data class BookmarkedEventsResponse(
    val page: Int,
    val size: Int,
    val total: Int,
    val items: List<EventSummaryResponse>
)
