package com.example.hangsha_android.ui.view.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.local.BookmarksLocalDataSource
import com.example.hangsha_android.data.local.StoredGuestBookmarkSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GuestBookmarksViewModel @Inject constructor(
    private val bookmarksLocalDataSource: BookmarksLocalDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                bookmarksLocalDataSource.bookmarkedEventIds(userId = null),
                bookmarksLocalDataSource.guestBookmarkSnapshots
            ) { bookmarkedEventIds, bookmarkSnapshots ->
                val current = _uiState.value
                current.copy(
                    items = bookmarkSnapshots.toBookmarkedEventItems(bookmarkedEventIds),
                    isInitialLoading = false,
                    isLoadingNextPage = false,
                    hasMore = false,
                    nextPage = 1,
                    errorMessage = null
                )
            }.collect { nextState ->
                _uiState.update { nextState }
            }
        }
    }

    fun removeBookmark(eventId: Long) {
        viewModelScope.launch {
            bookmarksLocalDataSource.setBookmarked(
                eventId = eventId,
                isBookmarked = false,
                userId = null
            )
        }
    }

    fun retry() = Unit

    fun loadNextPage() = Unit

    fun saveScrollPosition(
        firstVisibleItemIndex: Int,
        firstVisibleItemOffset: Int,
        firstVisibleItemId: Long?
    ) {
        _uiState.update {
            it.copy(
                savedScrollIndex = firstVisibleItemIndex,
                savedScrollOffset = firstVisibleItemOffset,
                savedAnchorItemId = firstVisibleItemId
            )
        }
    }
}

private fun List<StoredGuestBookmarkSnapshot>.toBookmarkedEventItems(
    bookmarkedEventIds: Set<Long>
): List<BookmarkedEventItem> {
    val snapshotsByEventId = associateBy { it.eventId }
    return bookmarkedEventIds.map { eventId ->
        snapshotsByEventId[eventId]?.toBookmarkedEventItem()
            ?: BookmarkedEventItem(
                id = eventId,
                title = "Event #$eventId",
                imageUrl = null,
                eventTypeId = 0L,
                statusId = 0L,
                dDayLabel = "-",
                applyPeriodDisplay = "-",
                organization = null,
                isBookmarked = true
            )
    }.sortedByDescending { item -> snapshotsByEventId[item.id]?.updatedAt.orEmpty() }
}

private fun StoredGuestBookmarkSnapshot.toBookmarkedEventItem(): BookmarkedEventItem {
    return BookmarkedEventItem(
        id = eventId,
        title = title,
        imageUrl = imageUrl,
        eventTypeId = eventTypeId,
        statusId = 0L,
        dDayLabel = dDayLabel.toDdayLabel(),
        applyPeriodDisplay = applyPeriodDisplay ?: "-",
        organization = organization,
        isBookmarked = true
    )
}

private fun String?.toDdayLabel(): String {
    val normalized = this?.trim().orEmpty()
        .removePrefix("Apply")
        .removePrefix("지원")
        .trim()
    if (normalized.isBlank()) {
        return "-"
    }
    return when {
        normalized.equals("D-DAY", ignoreCase = true) -> "D-day"
        normalized.startsWith("D") -> normalized
        else -> normalized
    }
}