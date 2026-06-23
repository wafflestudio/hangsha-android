package com.example.hangsha_android.ui.view.mypage

import android.net.Uri
import com.example.hangsha_android.ui.view.bookmarks.BookmarkedEventItem

data class MyPageUiState(
    val isLoading: Boolean = true,
    val isBookmarksPreviewLoading: Boolean = true,
    val isSavingProfile: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val interests: List<String> = emptyList(),
    val isEditingProfile: Boolean = false,
    val draftUsername: String = "",
    val draftProfileImageUrl: String? = null,
    val draftProfileImageUri: Uri? = null,
    val isProfileImageMarkedForDeletion: Boolean = false,
    val usernameErrorMessage: String? = null,
    val profileSaveErrorMessage: String? = null,
    val profileSaveToastMessage: String? = null,
    val bugReportTitle: String = "",
    val bugReportContent: String = "",
    val isSubmittingBugReport: Boolean = false,
    val bugReportToastMessage: String? = null,
    val accountDeletionErrorMessage: String? = null,
    val isLoggedOut: Boolean = false,
    val bookmarkedEvents: List<BookmarkedEventItem> = emptyList(),
    val hasMoreBookmarkedEvents: Boolean = false,
    val bookmarksPreviewErrorMessage: String? = null,
    val errorMessage: String? = null
)
