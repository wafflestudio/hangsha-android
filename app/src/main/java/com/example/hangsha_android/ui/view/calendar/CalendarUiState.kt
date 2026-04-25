package com.example.hangsha_android.ui.view.calendar

import com.example.hangsha_android.data.repository.model.EventDateRange
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val visibleRange: EventDateRange = currentMonth.toCalendarGridRange(),
    val visibleDates: List<LocalDate> = visibleRange.toDateList(),
    val eventsByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
    val appliedFilters: CalendarFilterState = CalendarFilterState(),
    val draftFilters: CalendarFilterState = CalendarFilterState(),
    val availableFilterOptions: CalendarFilterOptions = CalendarFilterOptions(),
    val isFilterSheetVisible: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val hasActiveFilters: Boolean
        get() = appliedFilters.hasActiveFilters
}
