package com.example.hangsha_android.ui.view.onboarding

import android.net.Uri

data class OnboardingUiState(
    val username: String = "",
    val profileImageUrl: String? = null,
    val profileImageUri: Uri? = null,
    val isProfileImageMarkedForDeletion: Boolean = false,
    val usernameErrorMessage: String? = null,
    val onboardingMessage: String? = null,
    val isSavingProfile: Boolean = false,
    val isProfileSaved: Boolean = false
) {
    val isSubmitEnabled: Boolean
        get() = username.isNotBlank() &&
            usernameErrorMessage == null &&
            !isSavingProfile
}
