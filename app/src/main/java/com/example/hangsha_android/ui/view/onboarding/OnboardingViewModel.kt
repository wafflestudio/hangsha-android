package com.example.hangsha_android.ui.view.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(username: String) {
        _uiState.update {
            it.copy(
                username = username,
                usernameErrorMessage = validateUsername(username),
                onboardingMessage = null,
                isUsernameSaved = false
            )
        }
    }

    fun saveUsername() {
        val currentState = _uiState.value
        val usernameError = validateUsername(currentState.username)
        if (usernameError != null) {
            _uiState.update { it.copy(usernameErrorMessage = usernameError) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingUsername = true,
                    onboardingMessage = null,
                    isUsernameSaved = false
                )
            }

            runCatching {
                val response = userRepository.updateMyUsername(currentState.username)
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
            }.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            username = currentState.username.trim(),
                            isSavingUsername = false,
                            isUsernameSaved = true,
                            usernameErrorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingUsername = false,
                            isUsernameSaved = false,
                            onboardingMessage = mapOnboardingErrorMessage(error)
                        )
                    }
                }
            )
        }
    }

    fun onOnboardingMessageConsumed() {
        _uiState.update {
            it.copy(onboardingMessage = null)
        }
    }

    fun onUsernameSavedConsumed() {
        _uiState.update {
            it.copy(isUsernameSaved = false)
        }
    }

    private fun validateUsername(username: String): String? {
        val trimmedUsername = username.trim()
        if (trimmedUsername.isBlank()) {
            return "사용자 이름을 입력해 주세요."
        }

        val maxLength = if (trimmedUsername.any { char -> char.isKorean() }) {
            KOREAN_USERNAME_MAX_LENGTH
        } else {
            ENGLISH_USERNAME_MAX_LENGTH
        }
        return if (trimmedUsername.length > maxLength) {
            "사용자 이름은 ${maxLength}자 이하여야 합니다."
        } else {
            null
        }
    }

    private fun mapOnboardingErrorMessage(error: Throwable): String {
        return when (error) {
            is IllegalArgumentException -> error.message ?: "사용자 이름을 확인해 주세요."
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is HttpException -> when (error.code()) {
                400 -> "사용자 이름을 확인해 주세요."
                401 -> "로그인이 필요합니다."
                403 -> "사용자 이름을 수정할 권한이 없습니다."
                409 -> "이미 사용 중인 사용자 이름입니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "사용자 이름 저장에 실패했습니다. (${error.code()})"
            }
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            else -> error.message ?: "사용자 이름 저장에 실패했습니다."
        }
    }
}

private fun Char.isKorean(): Boolean {
    return this in '\uAC00'..'\uD7A3' ||
        this in '\u1100'..'\u11FF' ||
        this in '\u3130'..'\u318F'
}

private const val ENGLISH_USERNAME_MAX_LENGTH = 20
private const val KOREAN_USERNAME_MAX_LENGTH = 10
