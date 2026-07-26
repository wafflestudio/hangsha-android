package com.example.hangsha_android.ui.view.guest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.local.BookmarksLocalDataSource
import com.example.hangsha_android.data.local.GuestMemosLocalDataSource
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
                guestMemosLocalDataSource.memos
            ) { bookmarkedEventIds, memos ->
                GuestMyPageUiState(
                    bookmarkedEventIds = bookmarkedEventIds,
                    memoItems = memos.map { it.toGuestMemoPreviewItem() }
                )
            }.collect { nextState ->
                _uiState.update { nextState }
            }
        }
    }
}

data class GuestMyPageUiState(
    val bookmarkedEventIds: Set<Long> = emptySet(),
    val memoItems: List<GuestMemoPreviewItem> = emptyList()
) {
    val bookmarkCount: Int
        get() = bookmarkedEventIds.size

    val memoCount: Int
        get() = memoItems.size
}

data class GuestMemoPreviewItem(
    val id: Long,
    val eventId: Long,
    val eventTitle: String,
    val content: String,
    val tagNames: List<String>,
    val updatedAt: String
)

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