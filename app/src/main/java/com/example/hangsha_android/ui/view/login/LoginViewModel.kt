package com.example.hangsha_android.ui.view.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.BuildConfig
import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.repository.AuthRepository
import com.example.hangsha_android.data.repository.ExcludedKeywordsRepository
import com.example.hangsha_android.data.repository.UserRepository
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
            email.isBlank() -> onAuthFailure("이메일을 입력해주세요.")
            password.isBlank() -> onAuthFailure("비밀번호를 입력해주세요.")
            else -> viewModelScope.launch {
                onCredentialLoginStarted()

                val result = runCatching {
                    val response = authRepository.login(email = email, password = password)
                    saveAccessTokenFromResponse(response)
                    loadOrganizationNames()
                    loadExcludedKeywords()
                }

                result.fold(
                    onSuccess = {
                        onAuthSuccess("로그인되었습니다.")
                    },
                    onFailure = { error ->
                        onAuthFailure(error, "login")
                    }
                )
            }
        }
    }

    fun onGoogleLoginConfigMissing() {
        onAuthFailure("Google 로그인이 아직 설정되지 않았습니다.")
    }

    fun onGoogleLoginCancelled() {
        onAuthFailure("Google 로그인이 취소되었습니다.")
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
                loginMessage = "Google 로그인 기록을 지웠습니다."
            )
        }
    }

    fun loginWithGoogle(serverAuthCode: String?) {
        if (BuildConfig.GOOGLE_SERVER_CLIENT_ID.isBlank()) {
            onGoogleLoginConfigMissing()
            return
        }

        if (serverAuthCode.isNullOrBlank()) {
            onGoogleLoginError("Google 로그인 정보를 가져오지 못했습니다.")
            return
        }

        viewModelScope.launch {
            onGoogleLoginStarted()

            val result = runCatching {
                val response = authRepository.loginWithGoogle(serverAuthCode)
                saveAccessTokenFromResponse(response)
                loadOrganizationNames()
                loadExcludedKeywords()
            }

            result.fold(
                onSuccess = {
                    onAuthSuccess("Google 로그인이 완료되었습니다.")
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
        response: retrofit2.Response<com.example.hangsha_android.data.network.model.LoginResponse>
    ) {
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val accessToken = response.body()?.accessToken
        if (accessToken.isNullOrBlank()) {
            throw IllegalStateException("로그인 응답에 필요한 토큰이 없습니다.")
        }

        authTokenStorage.saveAccessToken(accessToken)
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
            is UnknownHostException -> "인터넷 연결을 확인해주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해주세요."
            is HttpException -> when (error.code()) {
                400 -> "입력한 정보를 확인해주세요."
                401 -> if (actionLabel == "login") {
                    "이메일 또는 비밀번호가 일치하지 않습니다."
                } else {
                    "Google 로그인에 실패했습니다."
                }
                403 -> "계속 진행할 권한이 없습니다."
                404 -> "계정 정보를 찾을 수 없습니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                else -> "로그인에 실패했습니다. (${error.code()})"
            }
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해주세요."
            is IllegalStateException -> error.message
                ?: "로그인에 실패했습니다."
            else -> error.message ?: "로그인에 실패했습니다."
        }

        onAuthFailure(message)
    }
}
