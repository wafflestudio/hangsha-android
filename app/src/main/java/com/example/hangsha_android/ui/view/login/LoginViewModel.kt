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

    fun onGoogleLoginConfigMissing() {
        _uiState.update {
            it.copy(
                isGoogleLoginLoading = false,
                isGoogleHistoryClearing = false,
                loginMessage = "GOOGLE_SERVER_CLIENT_ID is not configured."
            )
        }
    }

    fun onGoogleLoginCancelled() {
        _uiState.update {
            it.copy(
                isGoogleLoginLoading = false,
                isGoogleHistoryClearing = false,
                loginMessage = "Google login was cancelled."
            )
        }
    }

    fun onGoogleLoginError(message: String) {
        _uiState.update {
            it.copy(
                isGoogleLoginLoading = false,
                isGoogleHistoryClearing = false,
                loginMessage = message
            )
        }
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

    fun onGoogleAuthCodeReceived(serverAuthCode: String?) {
        if (BuildConfig.GOOGLE_SERVER_CLIENT_ID.isBlank()) {
            onGoogleLoginConfigMissing()
            return
        }

        if (serverAuthCode.isNullOrBlank()) {
            onGoogleLoginError("Google login did not return a server auth code.")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGoogleLoginLoading = true,
                    isGoogleHistoryClearing = false,
                    isLoginSuccessful = false,
                    loginMessage = null
                )
            }

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

            _uiState.update {
                it.copy(
                    isGoogleLoginLoading = false,
                    isLoginSuccessful = result.isSuccess,
                    loginMessage = result.fold(
                        onSuccess = { "Google login succeeded." },
                        onFailure = { error -> error.message ?: "Google login failed." }
                    )
                )
            }
        }
    }

    fun onLoginSuccessConsumed() {
        _uiState.update {
            it.copy(isLoginSuccessful = false)
        }
    }

    companion object {
        private const val GOOGLE_PROVIDER = "GOOGLE"
    }
}
