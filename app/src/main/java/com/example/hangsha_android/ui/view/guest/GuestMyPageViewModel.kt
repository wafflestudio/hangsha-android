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
                bookmarksLocalDataSource.bookmarkedEventIds(isLoggedIn = false),
                bookmarksLocalDataSource.guestBookmarkSnapshots,
                guestMemosLocalDataSource.memos
            ) { bookmarkedEventIds, bookmarkSnapshots, memos ->
                val current = _uiState.value
                GuestMyPageUiState(
                    bookmarkItems = bookmarkSnapshots.toGuestBookmarkPreviewItems(bookmarkedEventIds),
                    bookmarkCount = bookmarkedEventIds.size,
                    memoItems = memos.map { it.toGuestMemoPreviewItem() },
                    bugReportTitle = current.bugReportTitle,
                    bugReportContent = current.bugReportContent,
                    isSubmittingBugReport = current.isSubmittingBugReport,
                    bugReportToastMessage = current.bugReportToastMessage
                )
            }.collect { nextState ->
                _uiState.update { nextState }
            }
        }
    }

    fun onBugReportTitleChanged(value: String) {
        _uiState.update {
            it.copy(
                bugReportTitle = value,
                bugReportToastMessage = null
            )
        }
    }

    fun onBugReportContentChanged(value: String) {
        _uiState.update {
            it.copy(
                bugReportContent = value,
                bugReportToastMessage = null
            )
        }
    }

    fun submitBugReport() {
        _uiState.update {
            it.copy(bugReportToastMessage = "Please log in to submit a bug report.")
        }
    }

    fun onBugReportToastConsumed() {
        _uiState.update {
            it.copy(bugReportToastMessage = null)
        }
    }
}

data class GuestMyPageUiState(
    val bookmarkItems: List<GuestBookmarkPreviewItem> = emptyList(),
    val bookmarkCount: Int = 0,
    val memoItems: List<GuestMemoPreviewItem> = emptyList(),
    val bugReportTitle: String = "",
    val bugReportContent: String = "",
    val isSubmittingBugReport: Boolean = false,
    val bugReportToastMessage: String? = null
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