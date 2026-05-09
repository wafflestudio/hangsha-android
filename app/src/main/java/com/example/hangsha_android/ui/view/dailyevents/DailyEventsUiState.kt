package com.example.hangsha_android.ui.view.dailyevents

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

data class DailyEventsUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val filterSourceItems: List<DailyEventItem> = emptyList(),
    val items: List<DailyEventItem> = emptyList(),
    val appliedFilters: DailyEventsFilterState = DailyEventsFilterState(),
    val draftFilters: DailyEventsFilterState = DailyEventsFilterState(),
    val availableFilterOptions: DailyEventsFilterOptions = DailyEventsFilterOptions(),
    val selectedFilterTab: DailyEventsFilterTab = DailyEventsFilterTab.EVENT_TYPE,
    val excludeKeywordInput: String = "",
    val hasAppliedServerFilters: Boolean = false,
    val isFilterSheetVisible: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val hasActiveFilters: Boolean
        get() = appliedFilters.hasActiveFilters

    val filteredItemCount: Int
        get() = previewItems.size

    private val previewItems: List<DailyEventItem>
        get() = if (isFilterSheetVisible) {
            filterSourceItems.applyFilters(draftFilters)
        } else {
            items
        }
}

data class DailyEventItem(
    val id: Long,
    val title: String,
    val organization: String?,
    val displayDate: String,
    val dDayLabel: String,
    val accentColor: Color,
    val isBookmarked: Boolean,
    val isInterested: Boolean,
    val orgId: Long,
    val statusId: Long,
    val eventTypeId: Long,
    val location: String?,
    val tags: String?
)

internal fun List<DailyEventItem>.applyFilters(
    filters: DailyEventsFilterState
): List<DailyEventItem> {
    if (!filters.hasActiveFilters) return this

    return filter { item -> item.matches(filters) }
}

private fun DailyEventItem.matches(filters: DailyEventsFilterState): Boolean {
    if (filters.bookmarkedOnly && !isBookmarked) return false
    if (filters.interestedOnly && !isInterested) return false
    if (filters.orgIds.isNotEmpty() && orgId !in filters.orgIds) return false
    if (filters.statusIds.isNotEmpty() && statusId !in filters.statusIds) return false
    if (filters.eventTypeIds.isNotEmpty() && eventTypeId !in filters.eventTypeIds) return false
    if (filters.excludedKeywords.isNotEmpty()) {
        val searchTargets = listOfNotNull(title, organization, location, tags)
            .joinToString(separator = " ")
            .lowercase()
        if (filters.excludedKeywords.any { keyword -> keyword.lowercase() in searchTargets }) {
            return false
        }
    }
    return true
}
