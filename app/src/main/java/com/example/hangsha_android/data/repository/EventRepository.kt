package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.network.api.EventApi
import com.example.hangsha_android.data.network.model.DayEventsResponse
import com.example.hangsha_android.data.network.model.EventCountResponse
import com.example.hangsha_android.data.network.model.EventDetailResponse
import com.example.hangsha_android.data.network.model.EventSearchResponse
import com.example.hangsha_android.data.network.model.EventSummaryResponse
import com.example.hangsha_android.data.network.model.MonthlyEventsResponse
import com.example.hangsha_android.data.repository.model.EventDateRange
import com.example.hangsha_android.ui.view.calendar.CalendarFilterState
import com.example.hangsha_android.ui.view.dailyevents.DailyEventsFilterState
import java.time.LocalDate
import javax.inject.Inject
import retrofit2.HttpException
import retrofit2.Response

class EventRepository @Inject constructor(
    private val eventApi: EventApi,
    private val authTokenStorage: AuthTokenStorage
) {
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

    suspend fun getDayEvents(
        date: LocalDate,
        filters: DailyEventsFilterState = DailyEventsFilterState()
    ): DayEventsResponse {
        return collectDayEventPages { page, size ->
            eventApi.getDayEvents(
                date = date.toString(),
                page = page,
                size = size,
                orgId = filters.orgIds.sorted().takeIf { it.isNotEmpty() },
                statusId = filters.statusIds.sorted().takeIf { it.isNotEmpty() },
                eventTypeId = filters.eventTypeIds.sorted().takeIf { it.isNotEmpty() },
                excludedKeywords = filters.excludedKeywords
                    .takeIf { it.isNotEmpty() && isLoggedIn() }
            )
        }
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

internal suspend fun collectDayEventPages(
    loader: suspend (page: Int, size: Int) -> Response<DayEventsResponse>
): DayEventsResponse {
    val uniqueItems = linkedMapOf<Long, EventSummaryResponse>()
    var page = 1
    var total = 0
    var responseDate = ""

    while (true) {
        val response = loader(page, DAY_EVENT_PAGE_SIZE)
        if (!response.isSuccessful) throw HttpException(response)
        val body = response.body()
            ?: throw IllegalStateException("Daily events response was empty.")
        val pageItems = body.items.orEmpty()
        if (responseDate.isBlank()) responseDate = body.date
        total = maxOf(total, body.total.coerceAtLeast(0))
        pageItems.forEach { item -> uniqueItems.putIfAbsent(item.id, item) }

        if (pageItems.size < DAY_EVENT_PAGE_SIZE) break
        check(page < MAX_DAY_EVENT_PAGES) {
            "Daily events pagination exceeded the safety limit."
        }
        page += 1
    }

    return DayEventsResponse(
        page = 1,
        size = uniqueItems.size,
        total = maxOf(total, uniqueItems.size),
        date = responseDate,
        items = uniqueItems.values.toList()
    )
}

private const val DAY_EVENT_PAGE_SIZE = 20
private const val MAX_DAY_EVENT_PAGES = 100
