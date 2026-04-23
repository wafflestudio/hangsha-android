package com.example.hangsha_android.ui.view.signup

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val signUpMessage: String? = null,
    val isSignUpLoading: Boolean = false,
    val isSignUpSuccessful: Boolean = false
) {
    val isSubmitEnabled: Boolean
        get() = email.isNotBlank() &&
            password.isNotBlank() &&
            passwordConfirmation.isNotBlank() &&
            password == passwordConfirmation &&
            !isSignUpLoading
}
