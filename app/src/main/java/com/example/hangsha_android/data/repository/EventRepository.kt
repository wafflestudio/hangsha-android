package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.model.DayEventsResponse
import com.example.hangsha_android.data.network.api.EventApi
import com.example.hangsha_android.data.network.model.MonthlyEventsResponse
import com.example.hangsha_android.data.repository.model.EventDateRange
import com.example.hangsha_android.ui.view.calendar.CalendarFilterState
import javax.inject.Inject
import java.time.LocalDate
import retrofit2.Response

class EventRepository @Inject constructor(
    private val eventApi: EventApi
) {
    companion object {
        private const val RECRUITING_STATUS_ID = 2L
    }

    suspend fun getEvents(
        range: EventDateRange,
        filters: CalendarFilterState = CalendarFilterState()
    ): Response<MonthlyEventsResponse> {
        return eventApi.getEvents(
            from = range.from.toString(),
            to = range.to.toString(),
            bookmarkedOnly = filters.bookmarkedOnly.takeIf { it },
            interestedOnly = filters.interestedOnly.takeIf { it },
            orgIds = filters.orgIds.sorted().takeIf { it.isNotEmpty() },
            statusIds = filters.statusIds.sorted().takeIf { it.isNotEmpty() },
            eventTypeIds = filters.eventTypeIds.sorted().takeIf { it.isNotEmpty() },
            excludedKeywords = filters.excludedKeywords.takeIf { it.isNotEmpty() }
        )
    }

    suspend fun getDayEvents(
        date: LocalDate
    ): Response<DayEventsResponse> {
        return eventApi.getDayEvents(
            date = date.toString(),
            statusIds = listOf(RECRUITING_STATUS_ID)
        )
    }
}
