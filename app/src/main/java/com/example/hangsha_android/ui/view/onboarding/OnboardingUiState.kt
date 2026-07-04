package com.example.hangsha_android.ui.view.onboarding

data class OnboardingUiState(
    val username: String = "",
    val usernameErrorMessage: String? = null,
    val onboardingMessage: String? = null,
    val isSavingUsername: Boolean = false,
    val isUsernameSaved: Boolean = false
) {
    val isSubmitEnabled: Boolean
        get() = username.isNotBlank() &&
            usernameErrorMessage == null &&
            !isSavingUsername
}
