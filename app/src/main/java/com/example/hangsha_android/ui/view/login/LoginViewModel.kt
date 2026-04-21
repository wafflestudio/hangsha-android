package com.example.hangsha_android.ui.view.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.BuildConfig
import com.example.hangsha_android.data.network.api.AuthApi
import com.example.hangsha_android.data.network.model.SocialLoginRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApi: AuthApi
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
        val username = currentState.username.trim()
        val password = currentState.password

        when {
            username.isBlank() -> onAuthFailure("Please enter your ID.")
            password.isBlank() -> onAuthFailure("Please enter your password.")
            else -> {
                _uiState.update {
                    it.copy(
                        isCredentialLoginLoading = false,
                        isLoginSuccessful = false,
                        loginMessage = "Server login is not connected yet. UI and ViewModel are now wired."
                    )
                }
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
                val response = authApi.loginWithSocial(
                    SocialLoginRequest(
                        provider = GOOGLE_PROVIDER,
                        code = serverAuthCode,
                        codeVerifier = null
                    )
                )

                if (!response.isSuccessful) {
                    error("Google login failed with code ${response.code()}")
                }
            }

            result.fold(
                onSuccess = {
                    onAuthSuccess("Google login succeeded.")
                },
                onFailure = { error ->
                    onAuthFailure(error.message ?: "Google login failed.")
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

    companion object {
        private const val GOOGLE_PROVIDER = "GOOGLE"
    }
}
