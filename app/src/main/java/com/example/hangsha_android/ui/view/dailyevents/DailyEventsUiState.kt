package com.example.hangsha_android.ui.view.dailyevents

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

data class DailyEventsUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val filterSourceItems: List<DailyEventItem> = emptyList(),
    val items: List<DailyEventItem> = emptyList(),
    val organizationNames: Map<Long, String> = emptyMap(),
    val eventTypeNames: Map<Long, String> = emptyMap(),
    val appliedFilters: DailyEventsFilterState = DailyEventsFilterState(),
    val draftFilters: DailyEventsFilterState = DailyEventsFilterState(),
    val availableFilterOptions: DailyEventsFilterOptions = DailyEventsFilterOptions(),
    val selectedFilterTab: DailyEventsFilterTab = DailyEventsFilterTab.EVENT_TYPE,
    val excludeKeywordInput: String = "",
    val filteredItemCount: Int? = null,
    val isFilterCountLoading: Boolean = false,
    val hasAppliedServerFilters: Boolean = appliedFilters.hasActiveFilters,
    val isFilterSheetVisible: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val hasActiveFilters: Boolean
        get() = appliedFilters.hasActiveFilters
}

data class DailyEventItem(
    val id: Long,
    val title: String,
    val imageUrl: String?,
    val organization: String?,
    val eventEndDisplay: String,
    val applyPeriodDisplay: String,
    val dDayLabel: String,
    val accentColor: Color,
    val isBookmarked: Boolean,
    val isInterested: Boolean,
    val orgId: Long?,
    val statusId: Long,
    val eventTypeId: Long,
    val location: String?,
    val tags: String?
)

internal fun List<DailyEventItem>.applyFilters(
    filters: DailyEventsFilterState
): List<DailyEventItem> {
    if (!filters.hasActiveFilters) return this

    return filter { item ->
        item.matches(filters = filters)
    }
}

private fun DailyEventItem.matches(
    filters: DailyEventsFilterState
): Boolean {
    if (filters.orgIds.isNotEmpty() && (orgId == null || orgId !in filters.orgIds)) return false
    if (filters.statusIds.isNotEmpty() && statusId !in filters.statusIds) return false
    if (filters.eventTypeIds.isNotEmpty() && eventTypeId !in filters.eventTypeIds) return false
    return true
}
