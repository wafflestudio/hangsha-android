package com.example.hangsha_android.ui.view.calendar

data class CalendarFilterState(
    val bookmarkedOnly: Boolean = false,
    val interestedOnly: Boolean = false,
    val operationModes: Set<String> = emptySet(),
    val statusIds: Set<Long> = emptySet(),
    val eventTypeIds: Set<Long> = emptySet()
) {
    val hasActiveFilters: Boolean
        get() = bookmarkedOnly ||
            interestedOnly ||
            operationModes.isNotEmpty() ||
            statusIds.isNotEmpty() ||
            eventTypeIds.isNotEmpty()

    fun matches(event: CalendarEvent): Boolean {
        if (bookmarkedOnly && !event.isBookmarked) return false
        if (interestedOnly && !event.isInterested) return false
        if (operationModes.isNotEmpty() && event.operationMode !in operationModes) return false
        if (statusIds.isNotEmpty() && event.statusId !in statusIds) return false
        if (eventTypeIds.isNotEmpty() && event.eventTypeId !in eventTypeIds) return false
        return true
    }
}

data class CalendarFilterOptions(
    val operationModes: List<String> = emptyList(),
    val statusIds: List<Long> = emptyList(),
    val eventTypeIds: List<Long> = emptyList()
)
