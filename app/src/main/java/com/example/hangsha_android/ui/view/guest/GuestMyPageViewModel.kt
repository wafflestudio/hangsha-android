package com.example.hangsha_android.ui.view.guest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.local.BookmarksLocalDataSource
import com.example.hangsha_android.data.local.GuestMemosLocalDataSource
import com.example.hangsha_android.data.local.StoredGuestBookmarkSnapshot
import com.example.hangsha_android.data.local.StoredGuestMemo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GuestMyPageViewModel @Inject constructor(
    private val bookmarksLocalDataSource: BookmarksLocalDataSource,
    private val guestMemosLocalDataSource: GuestMemosLocalDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(GuestMyPageUiState())
    val uiState: StateFlow<GuestMyPageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                bookmarksLocalDataSource.bookmarkedEventIds(userId = null),
                bookmarksLocalDataSource.guestBookmarkSnapshots,
                guestMemosLocalDataSource.memos
            ) { bookmarkedEventIds, bookmarkSnapshots, memos ->
                GuestMyPageUiState(
                    bookmarkItems = bookmarkSnapshots.toGuestBookmarkPreviewItems(bookmarkedEventIds),
                    bookmarkCount = bookmarkedEventIds.size,
                    memoItems = memos.map { it.toGuestMemoPreviewItem() }
                )
            }.collect { nextState ->
                _uiState.update { nextState }
            }
        }
    }

}

data class GuestMyPageUiState(
    val bookmarkItems: List<GuestBookmarkPreviewItem> = emptyList(),
    val bookmarkCount: Int = 0,
    val memoItems: List<GuestMemoPreviewItem> = emptyList()
) {
    val memoCount: Int
        get() = memoItems.size
}

data class GuestBookmarkPreviewItem(
    val eventId: Long,
    val title: String,
    val imageUrl: String?,
    val organization: String?,
    val dDayLabel: String?,
    val applyPeriodDisplay: String?,
    val eventTypeId: Long,
    val updatedAt: String
)

data class GuestMemoPreviewItem(
    val id: Long,
    val eventId: Long,
    val eventTitle: String,
    val content: String,
    val tagNames: List<String>,
    val updatedAt: String
)

private fun List<StoredGuestBookmarkSnapshot>.toGuestBookmarkPreviewItems(
    bookmarkedEventIds: Set<Long>
): List<GuestBookmarkPreviewItem> {
    val snapshotsByEventId = associateBy { it.eventId }
    return bookmarkedEventIds.map { eventId ->
        snapshotsByEventId[eventId]?.toGuestBookmarkPreviewItem()
            ?: GuestBookmarkPreviewItem(
                eventId = eventId,
                title = "Event #$eventId",
                imageUrl = null,
                organization = null,
                dDayLabel = null,
                applyPeriodDisplay = null,
                eventTypeId = 0L,
                updatedAt = ""
            )
    }.sortedByDescending { it.updatedAt }
}

private fun StoredGuestBookmarkSnapshot.toGuestBookmarkPreviewItem(): GuestBookmarkPreviewItem {
    return GuestBookmarkPreviewItem(
        eventId = eventId,
        title = title,
        imageUrl = imageUrl,
        organization = organization,
        dDayLabel = dDayLabel,
        applyPeriodDisplay = applyPeriodDisplay,
        eventTypeId = eventTypeId,
        updatedAt = updatedAt
    )
}

private fun StoredGuestMemo.toGuestMemoPreviewItem(): GuestMemoPreviewItem {
    return GuestMemoPreviewItem(
        id = id,
        eventId = eventId,
        eventTitle = eventTitle,
        content = content,
        tagNames = tagNames,
        updatedAt = updatedAt
    )
}