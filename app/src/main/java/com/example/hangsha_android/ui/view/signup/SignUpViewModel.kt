package com.example.hangsha_android.ui.view.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.network.model.LoginResponse
import com.example.hangsha_android.data.repository.AuthRepository
import com.example.hangsha_android.data.repository.CategoryRepository
import com.example.hangsha_android.data.repository.ExcludedKeywordsRepository
import com.example.hangsha_android.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authTokenStorage: AuthTokenStorage,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val excludedKeywordsRepository: ExcludedKeywordsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                verificationCode = "",
                signupToken = null,
                verificationCodeExpiresAt = null,
                signupTokenExpiresAt = null,
                isVerificationCodeSending = false,
                isVerificationCodeVerifying = false,
                isSignUpSuccessful = false,
                signUpMessage = null
            )
        }
    }

    fun onVerificationCodeChanged(verificationCode: String) {
        _uiState.update {
            it.copy(
                verificationCode = verificationCode,
                signupToken = null,
                signupTokenExpiresAt = null,
                isSignUpSuccessful = false,
                signUpMessage = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                isSignUpSuccessful = false,
                signUpMessage = null
            )
        }
    }

    fun onPasswordConfirmationChanged(passwordConfirmation: String) {
        _uiState.update {
            it.copy(
                passwordConfirmation = passwordConfirmation,
                isSignUpSuccessful = false,
                signUpMessage = null
            )
        }
    }

    fun onPrivacyPolicyAgreementChanged(isAgreed: Boolean) {
        _uiState.update {
            it.copy(
                isPrivacyPolicyAgreed = isAgreed,
                isSignUpSuccessful = false,
                signUpMessage = null
            )
        }
    }

    fun sendVerificationCode() {
        val email = _uiState.value.email.trim()
        if (!email.isValidEmailAddress()) {
            onSignUpFailure("\uC62C\uBC14\uB978 \uC774\uBA54\uC77C \uC8FC\uC18C\uB97C \uC785\uB825\uD574 \uC8FC\uC138\uC694.")
            return
        }

        _uiState.update {
            it.copy(
                isVerificationCodeSending = true,
                verificationCode = "",
                signupToken = null,
                verificationCodeExpiresAt = null,
                signupTokenExpiresAt = null,
                isSignUpSuccessful = false,
                signUpMessage = null
            )
        }

        viewModelScope.launch {
            runCatching {
                val response = authRepository.sendEmailVerificationCode(email)
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
                response.body()?.expiresAt
                    ?: throw IllegalStateException("\uC778\uC99D\uBC88\uD638 \uBC1C\uC1A1 \uC751\uB2F5\uC5D0 \uB9CC\uB8CC \uC2DC\uAC01\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.")
            }.fold(
                onSuccess = { expiresAt ->
                    _uiState.update {
                        if (it.email.trim() != email) it else it.copy(
                            isVerificationCodeSending = false,
                            verificationCodeExpiresAt = expiresAt,
                            signUpMessage = null
                        )
                    }
                },
                onFailure = ::onEmailVerificationFailure
            )
        }
    }

    fun verifyVerificationCode() {
        val email = _uiState.value.email.trim()
        val code = _uiState.value.verificationCode.trim()
        if (!email.isValidEmailAddress()) {
            onSignUpFailure("\uC62C\uBC14\uB978 \uC774\uBA54\uC77C \uC8FC\uC18C\uB97C \uC785\uB825\uD574 \uC8FC\uC138\uC694.")
            return
        }
        if (code.isBlank()) {
            onSignUpFailure("\uC774\uBA54\uC77C\uB85C \uBC1B\uC740 \uC778\uC99D\uBC88\uD638\uB97C \uC785\uB825\uD574 \uC8FC\uC138\uC694.")
            return
        }

        _uiState.update {
            it.copy(
                isVerificationCodeVerifying = true,
                signupToken = null,
                signupTokenExpiresAt = null,
                isSignUpSuccessful = false,
                signUpMessage = null
            )
        }

        viewModelScope.launch {
            runCatching {
                val response = authRepository.verifyEmailVerificationCode(email, code)
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
                response.body()?.also { verification ->
                    check(verification.signupToken.isNotBlank()) {
                        "\uC774\uBA54\uC77C \uC778\uC99D \uC751\uB2F5\uC5D0 signupToken\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."
                    }
                } ?: throw IllegalStateException("\uC774\uBA54\uC77C \uC778\uC99D \uC751\uB2F5\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.")
            }.fold(
                onSuccess = { verification ->
                    _uiState.update {
                        if (it.email.trim() != email) it else it.copy(
                            isVerificationCodeVerifying = false,
                            signupToken = verification.signupToken,
                            signupTokenExpiresAt = verification.expiresAt,
                            signUpMessage = null
                        )
                    }
                },
                onFailure = ::onEmailVerificationFailure
            )
        }
    }

    fun signUp() {
        val currentState = _uiState.value

        if (!currentState.isSubmitEnabled) {
            val message = if (!currentState.isPrivacyPolicyAgreed) {
                "\uAC1C\uC778\uC815\uBCF4 \uC218\uC9D1\u00B7\uC774\uC6A9 \uB3D9\uC758\uAC00 \uD544\uC694\uD569\uB2C8\uB2E4."
            } else if (currentState.signupToken.isNullOrBlank()) {
                "\uC774\uBA54\uC77C \uC778\uC99D\uC744 \uC644\uB8CC\uD574 \uC8FC\uC138\uC694."
            } else {
                "\uC774\uBA54\uC77C\uACFC \uBE44\uBC00\uBC88\uD638\uB97C \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            }
            onSignUpFailure(message)
            return
        }

        _uiState.update {
            it.copy(
                isSignUpLoading = true,
                isSignUpSuccessful = false,
                signUpMessage = null
            )
        }

        viewModelScope.launch {
            runCatching {
                val response = authRepository.register(
                    email = currentState.email.trim(),
                    password = currentState.password,
                    signupToken = currentState.signupToken.orEmpty(),
                    username = ""
                )

                saveTokensFromResponse(response)
                activateCurrentUser()
                loadOrganizationNames()
                loadExcludedKeywords()
            }.fold(
                onSuccess = {
                    onSignUpSuccess()
                },
                onFailure = { error ->
                    authTokenStorage.clearTokens()
                    onSignUpFailure(error)
                }
            )
        }
    }

    private fun onEmailVerificationFailure(error: Throwable) {
        val message = when (error) {
            is UnknownHostException -> "\uC778\uD130\uB137 \uC5F0\uACB0\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            is SocketTimeoutException -> "\uC694\uCCAD \uC2DC\uAC04\uC774 \uCD08\uACFC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is HttpException -> when (error.code()) {
                400 -> "\uC778\uC99D\uBC88\uD638\uB97C \uD655\uC778\uD574 \uC8FC\uC138\uC694."
                404 -> "\uC694\uCCAD\uD55C \uC774\uBA54\uC77C \uC778\uC99D \uC815\uBCF4\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4."
                409 -> "\uC774\uBBF8 \uC0AC\uC6A9 \uC911\uC778 \uC774\uBA54\uC77C\uC785\uB2C8\uB2E4."
                410 -> "\uC778\uC99D\uBC88\uD638\uAC00 \uB9CC\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uC7AC\uBC1C\uC1A1\uD574 \uC8FC\uC138\uC694."
                429 -> "\uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC694\uCCAD\uD574 \uC8FC\uC138\uC694."
                in 500..599 -> "\uC11C\uBC84 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
                else -> "\uC774\uBA54\uC77C \uC778\uC99D\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4. (${error.code()})"
            }
            is IOException -> "\uB124\uD2B8\uC6CC\uD06C \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            else -> "\uC774\uBA54\uC77C \uC778\uC99D \uC911 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4."
        }

        _uiState.update {
            it.copy(
                isVerificationCodeSending = false,
                isVerificationCodeVerifying = false,
                isSignUpSuccessful = false,
                signUpMessage = message
            )
        }
    }

    fun onSignUpFailure(message: String) {
        _uiState.update {
            it.copy(
                isSignUpLoading = false,
                isSignUpSuccessful = false,
                signUpMessage = message
            )
        }
    }

    fun onSignUpSuccessConsumed() {
        _uiState.update {
            it.copy(isSignUpSuccessful = false)
        }
    }

    fun onSignUpMessageConsumed() {
        _uiState.update {
            it.copy(signUpMessage = null)
        }
    }

    fun onSignUpFailure(error: Throwable) {
        val message = when (error) {
            is UnknownHostException -> "\uC778\uD130\uB137 \uC5F0\uACB0\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            is SocketTimeoutException -> "\uC694\uCCAD \uC2DC\uAC04\uC774 \uCD08\uACFC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is HttpException -> when (error.code()) {
                400 -> "\uD68C\uC6D0\uAC00\uC785 \uC694\uCCAD\uC774 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."
                401 -> "\uD68C\uC6D0\uAC00\uC785 \uC778\uC99D\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."
                403 -> "\uD68C\uC6D0\uAC00\uC785 \uAD8C\uD55C\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."
                409 -> "\uC774\uBBF8 \uC0AC\uC6A9 \uC911\uC778 \uC774\uBA54\uC77C\uC785\uB2C8\uB2E4."
                in 500..599 -> "\uC11C\uBC84 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
                else -> "\uD68C\uC6D0\uAC00\uC785\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4. (${error.code()})"
            }
            is IOException -> "\uB124\uD2B8\uC6CC\uD06C \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            else -> "\uD68C\uC6D0\uAC00\uC785 \uC911 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4."
        }

        onSignUpFailure(message)
    }

    private fun saveTokensFromResponse(response: Response<LoginResponse>) {
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val authTokens = response.body()
        val accessToken = authTokens?.accessToken
        val refreshToken = authTokens?.refreshToken
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            throw IllegalStateException("\uD68C\uC6D0\uAC00\uC785 \uC751\uB2F5\uC5D0 \uC778\uC99D \uC815\uBCF4\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.")
        }

        authTokenStorage.saveTokens(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    private suspend fun activateCurrentUser() {
        val response = userRepository.getMyProfile()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val userId = response.body()?.id
            ?: throw IllegalStateException("\uD504\uB85C\uD544 \uC751\uB2F5\uC5D0 \uC0AC\uC6A9\uC790 ID\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.")
        authTokenStorage.setCurrentUserId(userId)
    }

    private suspend fun loadOrganizationNames() {
        runCatching {
            categoryRepository.ensureCategoryCatalogLoaded()
        }
    }

    private suspend fun loadExcludedKeywords() {
        runCatching {
            excludedKeywordsRepository.refreshExcludedKeywords()
        }
    }

    private fun onSignUpSuccess() {
        _uiState.update {
            it.copy(
                isSignUpLoading = false,
                isSignUpSuccessful = true,
                signUpMessage = "\uD68C\uC6D0\uAC00\uC785\uC774 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."
            )
        }
    }
}

private fun String.isValidEmailAddress(): Boolean {
    val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    return matches(emailPattern)
}
