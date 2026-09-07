package com.example.hangsha_android.ui.view.search

data class SearchUiState(
    val input: String = "",
    val submittedQuery: String = "",
    val items: List<SearchEventItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null
) {
    val canLoadMore: Boolean
        get() = items.size < total && !isLoading && !isLoadingMore
}

data class SearchEventItem(
    val id: Long,
    val title: String,
    val snippet: String?,
    val imageUrl: String?,
    val organization: String?,
    val eventDateDisplay: String,
    val dDayLabel: String,
    val eventDDayLabel: String,
    val eventTypeId: Long?,
    val eventTypeLabel: String
)
