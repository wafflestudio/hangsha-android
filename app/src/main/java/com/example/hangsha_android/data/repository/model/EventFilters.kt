package com.example.hangsha_android.data.repository.model

data class EventFilters(
    val orgIds: Set<Long> = emptySet(),
    val statusIds: Set<Long> = setOf(RECRUITING_STATUS_ID),
    val eventTypeIds: Set<Long> = emptySet(),
    val excludedKeywords: List<String> = emptyList()
) {
    val hasActiveFilters: Boolean
        get() = orgIds.isNotEmpty() ||
            statusIds.isNotEmpty() ||
            eventTypeIds.isNotEmpty() ||
            excludedKeywords.isNotEmpty()
}
