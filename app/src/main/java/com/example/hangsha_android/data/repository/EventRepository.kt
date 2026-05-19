package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.EventApi
import com.example.hangsha_android.data.network.model.DayEventsResponse
import com.example.hangsha_android.data.network.model.EventDetailResponse
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
    suspend fun getAllEvents(
        range: EventDateRange
    ): Response<MonthlyEventsResponse> {
        return eventApi.getEvents(
            from = range.from.toString(),
            to = range.to.toString()
        )
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
            orgId = filters.orgIds.sorted().takeIf { it.isNotEmpty() },
            statusId = filters.statusIds.sorted().takeIf { it.isNotEmpty() },
            eventTypeId = filters.eventTypeIds.sorted().takeIf { it.isNotEmpty() },
            excludedKeywords = filters.excludedKeywords.takeIf { it.isNotEmpty() }
        )
    }

    suspend fun getAllDayEvents(
        date: LocalDate
    ): Response<DayEventsResponse> {
        return eventApi.getDayEvents(
            date = date.toString()
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
            orgId = filters.orgIds.sorted().takeIf { it.isNotEmpty() },
            statusId = filters.statusIds.sorted().takeIf { it.isNotEmpty() },
            eventTypeId = filters.eventTypeIds.sorted().takeIf { it.isNotEmpty() },
            excludedKeywords = filters.excludedKeywords.takeIf { it.isNotEmpty() }
        )
    }

    suspend fun getEventDetail(
        eventId: Long
    ): Response<EventDetailResponse> {
        return eventApi.getEventDetail(eventId = eventId)
    }

    suspend fun createBookmark(
        eventId: Long
    ): Response<Unit> {
        return eventApi.createBookmark(eventId = eventId)
    }

    suspend fun deleteBookmark(
        eventId: Long
    ): Response<Unit> {
        return eventApi.deleteBookmark(eventId = eventId)
    }

    suspend fun updateBookmark(
        eventId: Long,
        shouldBookmark: Boolean
    ): Response<Unit> {
        return if (shouldBookmark) {
            createBookmark(eventId = eventId)
        } else {
            deleteBookmark(eventId = eventId)
        }
    }
}
