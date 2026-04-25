package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.EventApi
import com.example.hangsha_android.data.network.model.MonthlyEventsResponse
import com.example.hangsha_android.data.repository.model.EventDateRange
import javax.inject.Inject
import retrofit2.Response

class EventRepository @Inject constructor(
    private val eventApi: EventApi
) {
    suspend fun getEvents(range: EventDateRange): Response<MonthlyEventsResponse> {
        return eventApi.getEvents(
            from = range.from.toString(),
            to = range.to.toString()
        )
    }
}
