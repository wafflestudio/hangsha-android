package com.example.hangsha_android.ui.view.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.BuildConfig
import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.network.model.LoginResponse
import com.example.hangsha_android.data.network.model.SocialLoginResponse
import com.example.hangsha_android.data.repository.AuthRepository
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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authTokenStorage: AuthTokenStorage,
    private val userRepository: UserRepository,
    private val excludedKeywordsRepository: ExcludedKeywordsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(username: String) {
        _uiState.update {
            it.copy(
                username = username,
                loginMessage = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                loginMessage = null
            )
        }
    }

    fun loginWithCredentials() {
        val currentState = _uiState.value
        val email = currentState.username.trim()
        val password = currentState.password

        when {
            email.isBlank() -> onAuthFailure("\uC774\uBA54\uC77C\uC744 \uC785\uB825\uD574 \uC8FC\uC138\uC694.")
            password.isBlank() -> onAuthFailure("\uBE44\uBC00\uBC88\uD638\uB97C \uC785\uB825\uD574 \uC8FC\uC138\uC694.")
            else -> viewModelScope.launch {
                onCredentialLoginStarted()

                val result = runCatching {
                    val response = authRepository.login(email = email, password = password)
                    saveTokensFromResponse(response)
                    activateCurrentUser()
                    loadOrganizationNames()
                    loadExcludedKeywords()
                }

                result.fold(
                    onSuccess = {
                        onAuthSuccess("\uB85C\uADF8\uC778\uB418\uC5C8\uC2B5\uB2C8\uB2E4.")
                    },
                    onFailure = { error ->
                        onAuthFailure(error, "login")
                    }
                )
            }
        }
    }

    fun onGoogleLoginConfigMissing() {
        onAuthFailure("Google \uB85C\uADF8\uC778\uC774 \uC124\uC815\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4.")
    }

    fun onKakaoLoginConfigMissing() {
        onAuthFailure("\uCE74\uCE74\uC624 \uB85C\uADF8\uC778\uC774 \uC124\uC815\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4.")
    }

    fun onNaverLoginConfigMissing() {
        onAuthFailure("\uB124\uC774\uBC84 \uB85C\uADF8\uC778\uC774 \uC124\uC815\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4.")
    }

    fun onGoogleLoginCancelled() {
        onAuthFailure("Google \uB85C\uADF8\uC778\uC774 \uCDE8\uC18C\uB418\uC5C8\uC2B5\uB2C8\uB2E4.")
    }

    fun onGoogleLoginError(message: String) {
        onAuthFailure(message)
    }

    fun onKakaoLoginError(message: String) {
        onAuthFailure(message)
    }

    fun onNaverLoginError(message: String) {
        onAuthFailure(message)
    }

    fun onGoogleHistoryClearStarted() {
        _uiState.update {
            it.copy(
                isGoogleHistoryClearing = true,
                loginMessage = null
            )
        }
    }

    fun onGoogleHistoryCleared() {
        _uiState.update {
            it.copy(
                isGoogleHistoryClearing = false,
                loginMessage = "Google \uB85C\uADF8\uC778 \uAE30\uB85D\uC744 \uC0AD\uC81C\uD588\uC2B5\uB2C8\uB2E4."
            )
        }
    }

    fun loginWithGoogle(serverAuthCode: String?) {
        if (BuildConfig.GOOGLE_SERVER_CLIENT_ID.isBlank()) {
            onGoogleLoginConfigMissing()
            return
        }

        if (serverAuthCode.isNullOrBlank()) {
            onGoogleLoginError("Google \uB85C\uADF8\uC778 \uC815\uBCF4\uB97C \uAC00\uC838\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4.")
            return
        }

        viewModelScope.launch {
            onGoogleLoginStarted()

            val result = runCatching {
                val socialResponse = authRepository.loginWithGoogle(serverAuthCode)
                val sessionResponse = authRepository.createMobileSession(
                    accessToken = getSocialAccessTokenFromResponse(socialResponse)
                )
                saveTokensFromResponse(sessionResponse)
                activateCurrentUser()
                loadOrganizationNames()
                loadExcludedKeywords()
            }

            result.fold(
                onSuccess = {
                    onAuthSuccess("Google \uB85C\uADF8\uC778\uC774 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4.")
                },
                onFailure = { error ->
                    onAuthFailure(error, "Google \uB85C\uADF8\uC778")
                }
            )
        }
    }

    fun loginWithKakao(accessToken: String?) {
        loginWithSocialAccessToken(
            accessToken = accessToken,
            actionLabel = "\uCE74\uCE74\uC624 \uB85C\uADF8\uC778",
            onStarted = ::onKakaoLoginStarted,
            login = authRepository::loginWithKakao
        )
    }

    fun loginWithNaver(accessToken: String?) {
        loginWithSocialAccessToken(
            accessToken = accessToken,
            actionLabel = "\uB124\uC774\uBC84 \uB85C\uADF8\uC778",
            onStarted = ::onNaverLoginStarted,
            login = authRepository::loginWithNaver
        )
    }

    fun onLoginSuccessConsumed() {
        _uiState.update {
            it.copy(isLoginSuccessful = false)
        }
    }

    fun continueAsGuest() {
        authTokenStorage.clearTokens()
        _uiState.update {
            it.copy(
                isCredentialLoginLoading = false,
                isGoogleLoginLoading = false,
                isKakaoLoginLoading = false,
                isNaverLoginLoading = false,
                isGoogleHistoryClearing = false,
                isLoginSuccessful = false,
                loginMessage = null
            )
        }
    }

    private fun loginWithSocialAccessToken(
        accessToken: String?,
        actionLabel: String,
        onStarted: () -> Unit,
        login: suspend (String) -> Response<SocialLoginResponse>
    ) {
        if (accessToken.isNullOrBlank()) {
            onAuthFailure("${actionLabel} \uC815\uBCF4\uB97C \uAC00\uC838\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4.")
            return
        }

        viewModelScope.launch {
            onStarted()

            val result = runCatching {
                val socialResponse = login(accessToken)
                val sessionResponse = authRepository.createMobileSession(
                    accessToken = getSocialAccessTokenFromResponse(socialResponse)
                )
                saveTokensFromResponse(sessionResponse)
                activateCurrentUser()
                loadOrganizationNames()
                loadExcludedKeywords()
            }

            result.fold(
                onSuccess = {
                    onAuthSuccess("${actionLabel}\uC774 \uC644\uB8CC\uB418\uC5C8\uC2B5\uB2C8\uB2E4.")
                },
                onFailure = { error ->
                    onAuthFailure(error, actionLabel)
                }
            )
        }
    }

    private fun onGoogleLoginStarted() {
        _uiState.update {
            it.copy(
                isGoogleLoginLoading = true,
                isKakaoLoginLoading = false,
                isNaverLoginLoading = false,
                isGoogleHistoryClearing = false,
                isLoginSuccessful = false,
                loginMessage = null
            )
        }
    }

    private fun onKakaoLoginStarted() {
        _uiState.update {
            it.copy(
                isGoogleLoginLoading = false,
                isKakaoLoginLoading = true,
                isNaverLoginLoading = false,
                isGoogleHistoryClearing = false,
                isLoginSuccessful = false,
                loginMessage = null
            )
        }
    }

    private fun onNaverLoginStarted() {
        _uiState.update {
            it.copy(
                isGoogleLoginLoading = false,
                isKakaoLoginLoading = false,
                isNaverLoginLoading = true,
                isGoogleHistoryClearing = false,
                isLoginSuccessful = false,
                loginMessage = null
            )
        }
    }

    private fun onCredentialLoginStarted() {
        _uiState.update {
            it.copy(
                isCredentialLoginLoading = true,
                isGoogleLoginLoading = false,
                isKakaoLoginLoading = false,
                isNaverLoginLoading = false,
                isGoogleHistoryClearing = false,
                isLoginSuccessful = false,
                loginMessage = null
            )
        }
    }

    private fun saveTokensFromResponse(response: Response<LoginResponse>) {
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val authTokens = response.body()
        val accessToken = authTokens?.accessToken
        val refreshToken = authTokens?.refreshToken
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            throw IllegalStateException("\uC778\uC99D \uC751\uB2F5\uC5D0 \uB85C\uADF8\uC778 \uC815\uBCF4\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.")
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

    private fun getSocialAccessTokenFromResponse(response: Response<SocialLoginResponse>): String {
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val accessToken = response.body()?.accessToken
        if (accessToken.isNullOrBlank()) {
            throw IllegalStateException("\uC18C\uC15C \uB85C\uADF8\uC778 \uC751\uB2F5\uC5D0 \uC778\uC99D \uC815\uBCF4\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.")
        }
        return accessToken
    }

    private suspend fun loadOrganizationNames() {
        runCatching {
            userRepository.ensureOrganizationNamesLoaded()
        }
    }

    private suspend fun loadExcludedKeywords() {
        runCatching {
            excludedKeywordsRepository.refreshExcludedKeywords()
        }
    }

    private fun onAuthSuccess(message: String) {
        _uiState.update {
            it.copy(
                isCredentialLoginLoading = false,
                isGoogleLoginLoading = false,
                isKakaoLoginLoading = false,
                isNaverLoginLoading = false,
                isLoginSuccessful = true,
                loginMessage = message
            )
        }
    }

    private fun onAuthFailure(message: String) {
        _uiState.update {
            it.copy(
                isCredentialLoginLoading = false,
                isGoogleLoginLoading = false,
                isKakaoLoginLoading = false,
                isNaverLoginLoading = false,
                isGoogleHistoryClearing = false,
                isLoginSuccessful = false,
                loginMessage = message
            )
        }
    }

    private fun onAuthFailure(error: Throwable, actionLabel: String) {
        authTokenStorage.clearTokens()
        val message = when (error) {
            is UnknownHostException -> "\uC778\uD130\uB137 \uC5F0\uACB0\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            is SocketTimeoutException -> "\uC694\uCCAD \uC2DC\uAC04\uC774 \uCD08\uACFC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is HttpException -> when (error.code()) {
                400 -> "\uC785\uB825\uD55C \uC815\uBCF4\uB97C \uD655\uC778\uD574 \uC8FC\uC138\uC694."
                401 -> if (actionLabel == "login") {
                    "\uC774\uBA54\uC77C \uB610\uB294 \uBE44\uBC00\uBC88\uD638\uAC00 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."
                } else {
                    "${actionLabel}\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."
                }
                403 -> "\uACC4\uC18D \uC9C4\uD589\uD560 \uAD8C\uD55C\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."
                404 -> "\uACC4\uC815 \uC815\uBCF4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."
                in 500..599 -> "\uC11C\uBC84 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
                else -> "\uB85C\uADF8\uC778\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4. (${error.code()})"
            }
            is IOException -> "\uB124\uD2B8\uC6CC\uD06C \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is IllegalStateException -> "\uB85C\uADF8\uC778\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."
            else -> "\uB85C\uADF8\uC778\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."
        }

        onAuthFailure(message)
    }

}
