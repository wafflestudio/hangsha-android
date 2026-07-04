package com.example.hangsha_android.ui.view.signup

data class SignUpUiState(
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val signUpMessage: String? = null,
    val isSignUpLoading: Boolean = false,
    val isSignUpSuccessful: Boolean = false
) {
    val isSubmitEnabled: Boolean
        get() = email.isNotBlank() &&
            username.isNotBlank() &&
            password.isNotBlank() &&
            password.isValidSignUpPassword() &&
            passwordConfirmation.isNotBlank() &&
            password == passwordConfirmation &&
            !isSignUpLoading
}

private fun String.isValidSignUpPassword(): Boolean {
    val hasLetter = any(Char::isLetter)
    val hasDigit = any(Char::isDigit)
    val hasSpecial = any { !it.isLetterOrDigit() && !it.isWhitespace() }

    return length >= 8 &&
        hasLetter &&
        hasDigit &&
        hasSpecial &&
        none(Char::isWhitespace)
}
