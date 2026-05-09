package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.EventApi
import com.example.hangsha_android.data.network.model.DayEventsResponse
import com.example.hangsha_android.data.network.model.MonthlyEventsResponse
import com.example.hangsha_android.data.repository.model.EventDateRange
import com.example.hangsha_android.ui.view.calendar.CalendarFilterState
import com.example.hangsha_android.ui.view.dailyevents.DailyEventsFilterState
import java.time.LocalDate
import javax.inject.Inject
import retrofit2.Response

class EventRepository @Inject constructor(
    private val eventApi: EventApi
) {
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
        date: LocalDate,
        filters: DailyEventsFilterState = DailyEventsFilterState()
    ): Response<DayEventsResponse> {
        return eventApi.getDayEvents(
            date = date.toString(),
            bookmarkedOnly = filters.bookmarkedOnly.takeIf { it },
            interestedOnly = filters.interestedOnly.takeIf { it },
            orgIds = filters.orgIds.sorted().takeIf { it.isNotEmpty() },
            statusIds = filters.statusIds.sorted().takeIf { it.isNotEmpty() },
            eventTypeIds = filters.eventTypeIds.sorted().takeIf { it.isNotEmpty() },
            excludedKeywords = filters.excludedKeywords.takeIf { it.isNotEmpty() }
        )
    }
}
