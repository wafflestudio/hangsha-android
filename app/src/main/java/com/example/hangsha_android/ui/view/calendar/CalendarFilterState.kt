package com.example.hangsha_android.ui.view.calendar

data class CalendarFilterState(
    // 북마크한 행사만 보겠다는 단일 토글 옵션
    val bookmarkedOnly: Boolean = false,
    // 관심 표시한 행사만 보겠다는 단일 토글 옵션
    val interestedOnly: Boolean = false,
    // "주최 기관" 탭에서 선택된 항목들
    val orgIds: Set<Long> = emptySet(),
    // "모집 현황" 탭에서 선택된 상태 id 집합
    val statusIds: Set<Long> = emptySet(),
    // "행사 종류" 탭에서 선택된 유형 id 집합
    val eventTypeIds: Set<Long> = emptySet(),
    // "제외" 탭에서 추가한 키워드 목록
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

data class CalendarFilterOptions(
    // 현재 월 데이터에서 발견된 operationMode 목록
    val orgIds: List<Long> = emptyList(),
    // 현재 월 데이터에서 발견된 status id 목록
    val statusIds: List<Long> = emptyList(),
    // 현재 월 데이터에서 발견된 eventType id 목록
    val eventTypeIds: List<Long> = emptyList()
)

enum class CalendarFilterTab(val label: String) {
    EVENT_TYPE("행사 종류"),
    ORGANIZER("주최 기관"),
    RECRUITMENT_STATUS("모집 현황"),
    EXCLUDE("제외")
}
