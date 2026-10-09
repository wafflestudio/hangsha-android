package com.example.hangsha_android.ui.view.calendar.filter

import com.example.hangsha_android.data.repository.model.EventFilters
import com.example.hangsha_android.ui.view.calendar.CalendarEvent
import java.time.LocalDate

internal fun EventFilters.resetSelections(): EventFilters {
    return EventFilters(excludedKeywords = excludedKeywords)
}

internal fun Map<LocalDate, List<CalendarEvent>>.applyFilters(
    filters: EventFilters
): Map<LocalDate, List<CalendarEvent>> {
    if (!filters.hasActiveFilters) return this

    return entries
        .mapNotNull { (date, events) ->
            val filteredEvents = events.filter {
                it.matches(filters = filters)
            }
            if (filteredEvents.isEmpty()) {
                null
            } else {
                date to filteredEvents
            }
        }
        .toMap(linkedMapOf())
}

private fun CalendarEvent.matches(
    filters: EventFilters
): Boolean {
    if (filters.orgIds.isNotEmpty() && (orgId == null || orgId !in filters.orgIds)) return false
    if (filters.statusIds.isNotEmpty() && (statusId == null || statusId !in filters.statusIds)) return false
    if (filters.eventTypeIds.isNotEmpty() && (eventTypeId == null || eventTypeId !in filters.eventTypeIds)) return false
    return true
}
