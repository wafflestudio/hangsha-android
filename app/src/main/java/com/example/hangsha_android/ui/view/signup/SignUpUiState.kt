package com.example.hangsha_android.ui.view.signup

data class SignUpUiState(
    val email: String = "",
    val verificationCode: String = "",
    val signupToken: String? = null,
    val verificationCodeExpiresAt: String? = null,
    val signupTokenExpiresAt: String? = null,
    val password: String = "",
    val passwordConfirmation: String = "",
    val isPrivacyPolicyAgreed: Boolean = false,
    val signUpMessage: String? = null,
    val emailErrorMessage: String? = null,
    val verificationCodeErrorMessage: String? = null,
    val isVerificationCodeSending: Boolean = false,
    val isVerificationCodeVerifying: Boolean = false,
    val isSignUpLoading: Boolean = false,
    val isSignUpSuccessful: Boolean = false
) {
    val isSubmitEnabled: Boolean
        get() = email.isNotBlank() &&
            password.isNotBlank() &&
            password.isValidSignUpPassword() &&
            passwordConfirmation.isNotBlank() &&
            password == passwordConfirmation &&
            !signupToken.isNullOrBlank() &&
            isPrivacyPolicyAgreed &&
            !isSignUpLoading &&
            !isVerificationCodeSending &&
            !isVerificationCodeVerifying
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
