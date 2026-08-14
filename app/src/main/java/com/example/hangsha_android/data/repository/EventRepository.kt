package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.network.api.EventApi
import com.example.hangsha_android.data.network.model.DayEventsResponse
import com.example.hangsha_android.data.network.model.EventCountResponse
import com.example.hangsha_android.data.network.model.EventDetailResponse
import com.example.hangsha_android.data.network.model.EventSearchResponse
import com.example.hangsha_android.data.network.model.MonthlyEventsResponse
import com.example.hangsha_android.data.repository.model.EventDateRange
import com.example.hangsha_android.ui.view.calendar.CalendarFilterState
import com.example.hangsha_android.ui.view.dailyevents.DailyEventsFilterState
import java.time.LocalDate
import javax.inject.Inject
import retrofit2.Response

class EventRepository @Inject constructor(
    private val eventApi: EventApi,
    private val authTokenStorage: AuthTokenStorage
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
            orgId = filters.orgIds.sorted().takeIf { it.isNotEmpty() },
            statusId = filters.statusIds.sorted().takeIf { it.isNotEmpty() },
            eventTypeId = filters.eventTypeIds.sorted().takeIf { it.isNotEmpty() },
            excludedKeywords = filters.excludedKeywords.takeIf { it.isNotEmpty() && isLoggedIn() }
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
            orgId = filters.orgIds.sorted().takeIf { it.isNotEmpty() },
            statusId = filters.statusIds.sorted().takeIf { it.isNotEmpty() },
            eventTypeId = filters.eventTypeIds.sorted().takeIf { it.isNotEmpty() },
            excludedKeywords = filters.excludedKeywords.takeIf { it.isNotEmpty() && isLoggedIn() }
        )
    }


    suspend fun getEventCount(
        range: EventDateRange,
        filters: CalendarFilterState
    ): Response<EventCountResponse> {
        return eventApi.getEventCount(
            from = range.from.toString(),
            to = range.to.toString(),
            statusId = filters.statusIds.sorted().takeIf { it.isNotEmpty() },
            eventTypeId = filters.eventTypeIds.sorted().takeIf { it.isNotEmpty() },
            orgId = filters.orgIds.sorted().takeIf { it.isNotEmpty() },
            applyExcludeKeywords = true
        )
    }

    suspend fun getDayEventCount(
        date: LocalDate,
        filters: DailyEventsFilterState
    ): Response<EventCountResponse> {
        return eventApi.getEventCount(
            from = date.toString(),
            to = date.toString(),
            statusId = filters.statusIds.sorted().takeIf { it.isNotEmpty() },
            eventTypeId = filters.eventTypeIds.sorted().takeIf { it.isNotEmpty() },
            orgId = filters.orgIds.sorted().takeIf { it.isNotEmpty() },
            applyExcludeKeywords = true
        )
    }
    suspend fun searchEvents(
        query: String,
        page: Int,
        size: Int
    ): Response<EventSearchResponse> {
        return eventApi.searchEvents(
            query = query.trim(),
            page = page,
            size = size
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

    private fun isLoggedIn(): Boolean {
        return authTokenStorage.hasAuthenticatedUser()
    }
}
