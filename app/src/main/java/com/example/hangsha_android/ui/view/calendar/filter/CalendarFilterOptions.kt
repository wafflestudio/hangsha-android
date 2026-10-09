package com.example.hangsha_android.ui.view.calendar.filter

data class CalendarFilterOptions(
    val orgIds: List<Long> = emptyList(),
    val statusIds: List<Long> = emptyList(),
    val eventTypeIds: List<Long> = emptyList()
)

enum class CalendarFilterTab(val label: String) {
    EVENT_TYPE("행사 종류"),
    ORGANIZER("주최 기관"),
    RECRUITMENT_STATUS("모집 현황"),
    EXCLUDE("제외")
}
