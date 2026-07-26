package com.example.hangsha_android.ui.view.login

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isAutoLoginLoading: Boolean = false,
    val isCredentialLoginLoading: Boolean = false,
    val isGoogleLoginLoading: Boolean = false,
    val isKakaoLoginLoading: Boolean = false,
    val isNaverLoginLoading: Boolean = false,
    val isGoogleHistoryClearing: Boolean = false,
    val isLoginSuccessful: Boolean = false,
    val loginMessage: String? = null
) {
    val isAnyLoginLoading: Boolean
        get() = isAutoLoginLoading ||
            isCredentialLoginLoading ||
            isGoogleLoginLoading ||
            isKakaoLoginLoading ||
            isNaverLoginLoading
}