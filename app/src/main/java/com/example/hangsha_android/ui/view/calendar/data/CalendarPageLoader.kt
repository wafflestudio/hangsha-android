package com.example.hangsha_android.ui.view.calendar.data

import com.example.hangsha_android.data.network.model.EventSummaryResponse
import com.example.hangsha_android.data.network.model.MonthlyEventsResponse
import com.example.hangsha_android.data.repository.BookmarkRepository
import com.example.hangsha_android.data.repository.EventRepository
import com.example.hangsha_android.data.repository.model.EventFilters
import com.example.hangsha_android.ui.view.calendar.CalendarEvent
import com.example.hangsha_android.ui.view.calendar.CalendarPageKey
import com.example.hangsha_android.ui.view.calendar.CalendarPeriod
import com.example.hangsha_android.ui.view.calendar.CalendarPeriodPage
import com.example.hangsha_android.ui.view.calendar.filter.applyFilters
import com.example.hangsha_android.ui.view.calendar.withBookmarkState
import java.time.LocalDate
import retrofit2.HttpException
import retrofit2.Response

/** Fetches and maps a page; job lifetimes and cache publication belong to the ViewModel. */
internal class CalendarPageLoader(
    private val eventRepository: EventRepository,
    private val bookmarkRepository: BookmarkRepository
) {
    suspend fun load(key: CalendarPageKey, filters: EventFilters): CalendarPeriodPage {
        val sourceUserId = bookmarkRepository.currentUserId()
        val sourceEventsResponse = if (key.period == CalendarPeriod.DAY) {
            val body = eventRepository.getDayEvents(key.startDate, filters)
            bookmarkRepository.syncKnownRemoteBookmarks(
                body.items.toBookmarkMap(), sourceUserId
            )
            mapOf(key.startDate to body.items.map { it.toCalendarEvent(key.startDate) })
        } else {
            val response = eventRepository.getEvents(
                range = key.period.visibleRange(key.startDate),
                filters = filters
            )
            val body = response.requireBody("Events response was empty.")
            bookmarkRepository.syncKnownRemoteBookmarks(body.toBookmarkMap(), sourceUserId)
            body.toCalendarEventsByDate()
        }
        val sourceEvents = sourceEventsResponse
            .withBookmarkState(bookmarkRepository.currentBookmarkedEventIds())
        return CalendarPeriodPage(
            filterSourceEventsByDate = sourceEvents,
            eventsByDate = sourceEvents.applyFilters(filters),
            isLoading = false
        )
    }
}

private fun MonthlyEventsResponse.toCalendarEventsByDate(): Map<LocalDate, List<CalendarEvent>> {
    return byDate.entries
        .mapNotNull { (dateString, response) ->
            val date = runCatching { LocalDate.parse(dateString) }.getOrNull()
                ?: return@mapNotNull null

            date to response.events.map { event ->
                event.toCalendarEvent(date)
            }
        }
        .sortedBy { it.first }
        .toMap(linkedMapOf())
}

private fun MonthlyEventsResponse.toBookmarkMap(): Map<Long, Boolean> {
    return byDate.values
        .flatMap { it.events }
        .mapNotNull { event ->
            event.isBookmarked?.let { isBookmarked -> event.id to isBookmarked }
        }
        .toMap()
}

private fun List<EventSummaryResponse>.toBookmarkMap(): Map<Long, Boolean> =
    mapNotNull { event ->
        event.isBookmarked?.let { isBookmarked -> event.id to isBookmarked }
    }.toMap()

private fun EventSummaryResponse.toCalendarEvent(date: LocalDate): CalendarEvent {
    return CalendarEvent(
        id = id,
        date = date,
        title = title,
        imageUrl = imageUrl,
        operationMode = operationMode,
        statusId = statusId,
        eventTypeId = eventTypeId,
        orgId = orgId,
        applyStart = applyStart,
        applyEnd = applyEnd,
        eventStart = eventStart,
        eventEnd = eventEnd,
        isPeriodEvent = isPeriodEvent,
        capacity = capacity,
        applyCount = applyCount,
        organization = organization,
        location = location,
        applyLink = applyLink,
        tags = tags,
        isInterested = isInterested == true,
        matchedInterestPriority = matchedInterestPriority,
        isBookmarked = isBookmarked == true
    )
}

private fun Response<MonthlyEventsResponse>.requireBody(
    emptyMessage: String
): MonthlyEventsResponse {
    if (!isSuccessful) {
        throw HttpException(this)
    }

    return body() ?: throw IllegalStateException(emptyMessage)
}
