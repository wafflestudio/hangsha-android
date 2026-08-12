package com.example.hangsha_android.ui.view.dailyevents

import com.example.hangsha_android.data.repository.model.RECRUITING_STATUS_ID

data class DailyEventsFilterState(
    val bookmarkedOnly: Boolean = false,
    val interestedOnly: Boolean = false,
    val orgIds: Set<Long> = emptySet(),
    val statusIds: Set<Long> = setOf(RECRUITING_STATUS_ID),
    val eventTypeIds: Set<Long> = emptySet(),
    val excludedKeywords: List<String> = emptyList()
) {
    val hasActiveFilters: Boolean
        get() = bookmarkedOnly ||
            interestedOnly ||
            orgIds.isNotEmpty() ||
            statusIds.isNotEmpty() ||
            eventTypeIds.isNotEmpty() ||
            excludedKeywords.isNotEmpty()
}

internal fun DailyEventsFilterState.resetSelections(): DailyEventsFilterState {
    return DailyEventsFilterState(excludedKeywords = excludedKeywords)
}

data class DailyEventsFilterOptions(
    val orgIds: List<Long> = emptyList(),
    val statusIds: List<Long> = emptyList(),
    val eventTypeIds: List<Long> = emptyList()
)

enum class DailyEventsFilterTab(val label: String) {
    EVENT_TYPE("행사 종류"),
    ORGANIZER("주최 기관"),
    RECRUITMENT_STATUS("모집 현황"),
    EXCLUDE("제외")
}
