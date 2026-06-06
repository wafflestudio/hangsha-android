package com.example.hangsha_android.ui.view.mypage

import android.net.Uri

data class MyPageUiState(
    val isLoading: Boolean = true,
    val isSavingProfile: Boolean = false,
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
    val isLoggedOut: Boolean = false,
    val errorMessage: String? = null
)
