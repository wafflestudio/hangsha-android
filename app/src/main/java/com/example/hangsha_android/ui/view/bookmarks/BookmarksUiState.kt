package com.example.hangsha_android.ui.view.bookmarks

data class BookmarksUiState(
    val items: List<BookmarkedEventItem> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isLoadingNextPage: Boolean = false,
    val hasMore: Boolean = true,
    val nextPage: Int = 1,
    val errorMessage: String? = null,
    val savedScrollIndex: Int = 0,
    val savedScrollOffset: Int = 0,
    val savedAnchorItemId: Long? = null
)

data class BookmarkedEventItem(
    val id: Long,
    val title: String,
    val imageUrl: String?,
    val eventTypeId: Long,
    val statusId: Long,
    val dDayLabel: String,
    val applyPeriodDisplay: String,
    val organization: String?,
    val isBookmarked: Boolean
)
