package com.example.hangsha_android.ui.view.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.BuildConfig
import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.repository.AuthRepository
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authTokenStorage: AuthTokenStorage
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
            email.isBlank() -> onAuthFailure("Please enter your email.")
            password.isBlank() -> onAuthFailure("Please enter your password.")
            else -> viewModelScope.launch {
                onCredentialLoginStarted()

                val result = runCatching {
                    val response = authRepository.login(email = email, password = password)
                    saveAccessTokenFromResponse(response, "login")
                }

                result.fold(
                    onSuccess = {
                        onAuthSuccess("Login succeeded.")
                    },
                    onFailure = { error ->
                        onAuthFailure(error, "login")
                    }
                )
            }
        }
    }

    fun onGoogleLoginConfigMissing() {
        onAuthFailure("GOOGLE_SERVER_CLIENT_ID is not configured.")
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
                loginMessage = "Cleared local Google sign-in history."
            )
        }
    }

    fun loginWithGoogle(serverAuthCode: String?) {
        if (BuildConfig.GOOGLE_SERVER_CLIENT_ID.isBlank()) {
            onGoogleLoginConfigMissing()
            return
        }

        if (serverAuthCode.isNullOrBlank()) {
            onGoogleLoginError("Google login did not return a server auth code.")
            return
        }

        viewModelScope.launch {
            onGoogleLoginStarted()

            val result = runCatching {
                val response = authRepository.loginWithGoogle(serverAuthCode)
                saveAccessTokenFromResponse(response, "Google login")
            }

            result.fold(
                onSuccess = {
                    onAuthSuccess("Google login succeeded.")
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

    private fun saveAccessTokenFromResponse(
        response: retrofit2.Response<com.example.hangsha_android.data.network.model.LoginResponse>,
        actionLabel: String
    ) {
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val accessToken = response.body()?.accessToken
        if (accessToken.isNullOrBlank()) {
            throw IllegalStateException("$actionLabel succeeded but access token was missing.")
        }

        authTokenStorage.saveAccessToken(accessToken)
    }

    private fun onAuthSuccess(message: String) {
        _uiState.update {
            it.copy(
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
            is UnknownHostException -> "No internet connection. Please check your network."
            is SocketTimeoutException -> "The request timed out. Please try again."
            is HttpException -> when (error.code()) {
                400 -> "Invalid $actionLabel request."
                401 -> "Incorrect email or password."
                403 -> "You do not have permission to continue."
                404 -> "Account information could not be found."
                in 500..599 -> "Server error occurred. Please try again later."
                else -> "${actionLabel.replaceFirstChar(Char::titlecase)} failed with code ${error.code()}."
            }
            is IOException -> "Network error occurred. Please try again."
            is IllegalStateException -> error.message
                ?: "${actionLabel.replaceFirstChar(Char::titlecase)} failed."
            else -> error.message ?: "${actionLabel.replaceFirstChar(Char::titlecase)} failed."
        }

        onAuthFailure(message)
    }
}
