package com.example.hangsha_android.data.network.model

data class MonthlyEventsResponse(
    val range: EventDateRangeResponse,
    val byDate: Map<String, EventsByDateResponse>
)
