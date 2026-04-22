package com.example.hangsha_android.ui.view.signup

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val passwordConfirmation: String = ""
) {
    val isSubmitEnabled: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && passwordConfirmation.isNotBlank()
}

