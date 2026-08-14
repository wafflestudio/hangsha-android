package com.example.hangsha_android.ui.view.calendar

import com.example.hangsha_android.data.repository.model.EventDateRange
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val visibleRange: EventDateRange = currentMonth.toCalendarGridRange(),
    val visibleDates: List<LocalDate> = visibleRange.toDateList(),
    val filterSourceEventsByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
    val eventsByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
    val organizationNames: Map<Long, String> = emptyMap(),
    val eventTypeNames: Map<Long, String> = emptyMap(),
    val appliedFilters: CalendarFilterState = CalendarFilterState(),
    val draftFilters: CalendarFilterState = CalendarFilterState(),
    val availableFilterOptions: CalendarFilterOptions = CalendarFilterOptions(),
    val selectedFilterTab: CalendarFilterTab = CalendarFilterTab.EVENT_TYPE,
    val excludeKeywordInput: String = "",
    val filteredEventCount: Int? = null,
    val isFilterCountLoading: Boolean = false,
    val hasAppliedServerFilters: Boolean = appliedFilters.hasActiveFilters,
    val isFilterSheetVisible: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val hasActiveFilters: Boolean
        get() = appliedFilters.hasActiveFilters
}

internal fun Map<LocalDate, List<CalendarEvent>>.applyFilters(
    filters: CalendarFilterState
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
    filters: CalendarFilterState
): Boolean {
    if (filters.orgIds.isNotEmpty() && (orgId == null || orgId !in filters.orgIds)) return false
    if (filters.statusIds.isNotEmpty() && statusId !in filters.statusIds) return false
    if (filters.eventTypeIds.isNotEmpty() && eventTypeId !in filters.eventTypeIds) return false
    return true
}
