package com.example.hangsha_android.ui.view.login

data class LoginUiState(
    val isCredentialLoginLoading: Boolean = false,
    val isGoogleLoginLoading: Boolean = false,
    val isGoogleHistoryClearing: Boolean = false,
    val isLoginSuccessful: Boolean = false,
    val loginMessage: String? = null
) {
    val isAnyLoginLoading: Boolean
        get() = isCredentialLoginLoading || isGoogleLoginLoading
}
