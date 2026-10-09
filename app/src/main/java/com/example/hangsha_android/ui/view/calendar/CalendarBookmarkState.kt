package com.example.hangsha_android.ui.view.calendar

import com.example.hangsha_android.ui.view.calendar.filter.applyFilters
import java.time.LocalDate

internal fun Map<LocalDate, List<CalendarEvent>>.withBookmarkState(
    bookmarkedEventIds: Set<Long>
): Map<LocalDate, List<CalendarEvent>> {
    return mapValues { (_, events) ->
        events.map { event ->
            event.copy(isBookmarked = event.id in bookmarkedEventIds)
        }
    }
}

internal fun Map<LocalDate, List<CalendarEvent>>.withBookmarkState(
    eventId: Long,
    isBookmarked: Boolean
): Map<LocalDate, List<CalendarEvent>> {
    return mapValues { (_, events) ->
        events.map { event ->
            if (event.id == eventId) {
                event.copy(isBookmarked = isBookmarked)
            } else {
                event
            }
        }
    }
}

internal fun CalendarUiState.withUpdatedBookmark(
    eventId: Long,
    isBookmarked: Boolean
): CalendarUiState {
    return copy(
        pageStates = pageStates.mapValues { (_, page) ->
            page.copy(
                filterSourceEventsByDate = page.filterSourceEventsByDate.withBookmarkState(
                    eventId = eventId,
                    isBookmarked = isBookmarked
                ),
                eventsByDate = page.eventsByDate.withBookmarkState(
                    eventId = eventId,
                    isBookmarked = isBookmarked
                ).applyFilters(appliedFilters)
            )
        }
    )
}
