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
    private var hasAttemptedAutoLogin = false

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

    fun tryAutoLogin() {
        if (hasAttemptedAutoLogin) {
            return
        }
        hasAttemptedAutoLogin = true

        val refreshToken = authTokenStorage.getRefreshToken()
        if (refreshToken.isNullOrBlank()) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAutoLoginLoading = true,
                    loginMessage = null
                )
            }

            val result = runCatching {
                val response = authRepository.refresh(refreshToken)
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
                saveTokensFromResponse(response)
                loadOrganizationNames()
                loadExcludedKeywords()
            }

            result.fold(
                onSuccess = {
                    onAuthSuccess("Logged in automatically.")
                },
                onFailure = { error ->
                    onAutoLoginFailure(error)
                }
            )
        }
    }

    fun loginWithCredentials() {
        val currentState = _uiState.value
        val email = currentState.username.trim()
        val password = currentState.password

        when {
            email.isBlank() -> onAuthFailure("Please enter your email.")
            password.isBlank() -> onAuthFailure("Please enter your password.")
            else -> viewModelScope.launch {
                onCredentialLoginStarted()

                val result = runCatching {
                    val response = authRepository.login(email = email, password = password)
                    saveTokensFromResponse(response)
                    loadOrganizationNames()
                    loadExcludedKeywords()
                }

                result.fold(
                    onSuccess = {
                        onAuthSuccess("Logged in successfully.")
                    },
                    onFailure = { error ->
                        onAuthFailure(error, "login")
                    }
                )
            }
        }
    }

    fun onGoogleLoginConfigMissing() {
        onAuthFailure("Google login is not configured yet.")
    }

    fun onGoogleLoginCancelled() {
        onAuthFailure("Google login was cancelled.")
    }

    fun onGoogleLoginError(message: String) {
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
                loginMessage = "Google sign-in history was cleared."
            )
        }
    }

    fun loginWithGoogle(serverAuthCode: String?) {
        if (BuildConfig.GOOGLE_SERVER_CLIENT_ID.isBlank()) {
            onGoogleLoginConfigMissing()
            return
        }

        if (serverAuthCode.isNullOrBlank()) {
            onGoogleLoginError("Could not retrieve Google login information.")
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
                loadOrganizationNames()
                loadExcludedKeywords()
            }

            result.fold(
                onSuccess = {
                    onAuthSuccess("Google login completed successfully.")
                },
                onFailure = { error ->
                    onAuthFailure(error, "Google login")
                }
            )
        }
    }

    fun onLoginSuccessConsumed() {
        _uiState.update {
            it.copy(isLoginSuccessful = false)
        }
    }

    private fun onGoogleLoginStarted() {
        _uiState.update {
            it.copy(
                isGoogleLoginLoading = true,
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
            throw IllegalStateException("Authentication response did not include auth tokens.")
        }

        authTokenStorage.saveTokens(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    private fun getSocialAccessTokenFromResponse(response: Response<SocialLoginResponse>): String {
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val accessToken = response.body()?.accessToken
        if (accessToken.isNullOrBlank()) {
            throw IllegalStateException("Social login response did not include an access token.")
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
                isAutoLoginLoading = false,
                isCredentialLoginLoading = false,
                isGoogleLoginLoading = false,
                isLoginSuccessful = true,
                loginMessage = message
            )
        }
    }

    private fun onAuthFailure(message: String) {
        _uiState.update {
            it.copy(
                isAutoLoginLoading = false,
                isCredentialLoginLoading = false,
                isGoogleLoginLoading = false,
                isGoogleHistoryClearing = false,
                isLoginSuccessful = false,
                loginMessage = message
            )
        }
    }

    private fun onAuthFailure(error: Throwable, actionLabel: String) {
        val message = when (error) {
            is UnknownHostException -> "Please check your internet connection."
            is SocketTimeoutException -> "The request timed out. Please try again."
            is HttpException -> when (error.code()) {
                400 -> "Please check the information you entered."
                401 -> if (actionLabel == "login") {
                    "Your email or password is incorrect."
                } else {
                    "Google login failed."
                }
                403 -> "You do not have permission to continue."
                404 -> "Account information could not be found."
                in 500..599 -> "A server error occurred. Please try again later."
                else -> "Login failed. (${error.code()})"
            }
            is IOException -> "A network error occurred. Please try again."
            is IllegalStateException -> error.message ?: "Login failed."
            else -> error.message ?: "Login failed."
        }

        onAuthFailure(message)
    }

    private fun onAutoLoginFailure(error: Throwable) {
        when (error) {
            is HttpException -> {
                if (error.code() == 404) {
                    authTokenStorage.clearTokens()
                    _uiState.update {
                        it.copy(
                            isAutoLoginLoading = false,
                            isLoginSuccessful = false,
                            loginMessage = null
                        )
                    }
                    return
                }

                if (error.code() == 401) {
                    authTokenStorage.clearTokens()
                    _uiState.update {
                        it.copy(
                            isAutoLoginLoading = false,
                            isLoginSuccessful = false,
                            loginMessage = "Your session has expired. Please log in again."
                        )
                    }
                    return
                }
            }
        }

        onAuthFailure(error, "auto login")
    }
}
