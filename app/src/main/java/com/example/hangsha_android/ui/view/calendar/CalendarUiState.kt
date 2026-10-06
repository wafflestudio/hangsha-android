package com.example.hangsha_android.ui.view.calendar

import com.example.hangsha_android.util.currentHangshaDate
import com.example.hangsha_android.data.repository.model.EventDateRange
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val anchorDate: LocalDate = currentHangshaDate(),
    val period: CalendarPeriod = CalendarPeriod.MONTH,
    val viewMode: CalendarViewMode = CalendarViewMode.CALENDAR,
    val pageStates: Map<CalendarPageKey, CalendarPeriodPage> = emptyMap(),
    val organizationNames: Map<Long, String> = emptyMap(),
    val statusNames: Map<Long, String> = emptyMap(),
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
    val errorMessage: String? = null
) {
    val currentPage: CalendarPeriodPage?
        get() = pageStates[CalendarPageKey.from(period, anchorDate)]

    val visibleRange: EventDateRange
        get() = period.visibleRange(anchorDate)

    val visibleDates: List<LocalDate>
        get() = visibleRange.toDateList()

    val filterSourceEventsByDate: Map<LocalDate, List<CalendarEvent>>
        get() = currentPage?.filterSourceEventsByDate.orEmpty()

    val eventsByDate: Map<LocalDate, List<CalendarEvent>>
        get() = currentPage?.eventsByDate.orEmpty()

    val isLoading: Boolean
        get() = currentPage?.isLoading ?: true

    val contentRange: EventDateRange
        get() = period.contentRange(anchorDate)

    val currentMonth: YearMonth
        get() = YearMonth.from(anchorDate)

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
    if (filters.statusIds.isNotEmpty() && (statusId == null || statusId !in filters.statusIds)) return false
    if (filters.eventTypeIds.isNotEmpty() && (eventTypeId == null || eventTypeId !in filters.eventTypeIds)) return false
    return true
}
