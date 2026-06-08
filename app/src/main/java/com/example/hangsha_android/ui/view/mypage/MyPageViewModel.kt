package com.example.hangsha_android.ui.view.mypage

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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
class MyPageViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authTokenStorage: AuthTokenStorage,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    init {
        loadMyProfile()
    }

    fun loadMyProfile() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            runCatching {
                val response = userRepository.getMyProfile()
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }

                response.body() ?: throw IllegalStateException("Profile response was empty.")
            }.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            username = profile.username,
                            email = profile.email,
                            profileImageUrl = profile.profileImageUrl,
                            draftUsername = profile.username,
                            draftProfileImageUrl = profile.profileImageUrl,
                            draftProfileImageUri = null,
                            isProfileImageMarkedForDeletion = false,
                            interests = profile.interestCategories
                                .orEmpty()
                                .sortedBy { interest -> interest.priority }
                                .map { interest -> interest.category.name },
                            usernameErrorMessage = null,
                            profileSaveErrorMessage = null,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapErrorMessage(error)
                        )
                    }
                }
            )
        }
    }

    fun startProfileEdit() {
        _uiState.update {
            it.copy(
                isEditingProfile = true,
                draftUsername = it.username,
                draftProfileImageUrl = it.profileImageUrl,
                draftProfileImageUri = null,
                isProfileImageMarkedForDeletion = false,
                usernameErrorMessage = null,
                profileSaveErrorMessage = null
            )
        }
    }

    fun onDraftUsernameChanged(value: String) {
        _uiState.update {
            it.copy(
                draftUsername = value,
                usernameErrorMessage = validateDraftUsername(value)
            )
        }
    }

    fun onDraftProfileImageSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                draftProfileImageUri = uri,
                isProfileImageMarkedForDeletion = false,
                profileSaveErrorMessage = null
            )
        }
    }

    fun markDraftProfileImageDeleted() {
        _uiState.update {
            it.copy(
                draftProfileImageUri = null,
                draftProfileImageUrl = null,
                isProfileImageMarkedForDeletion = true,
                profileSaveErrorMessage = null
            )
        }
    }

    fun saveProfileEdit() {
        val current = _uiState.value
        val usernameError = validateDraftUsername(current.draftUsername)
        if (usernameError != null) {
            _uiState.update { it.copy(usernameErrorMessage = usernameError) }
            return
        }

        viewModelScope.launch {
            val previousUsername = current.username
            val previousProfileImageUrl = current.profileImageUrl
            val optimisticProfileImageUrl = when {
                current.draftProfileImageUri != null -> current.draftProfileImageUri.toString()
                current.isProfileImageMarkedForDeletion -> null
                else -> current.draftProfileImageUrl
            }

            _uiState.update {
                it.copy(
                    isSavingProfile = true,
                    isEditingProfile = false,
                    username = current.draftUsername.trim(),
                    profileImageUrl = optimisticProfileImageUrl,
                    draftUsername = current.draftUsername.trim(),
                    draftProfileImageUrl = optimisticProfileImageUrl,
                    draftProfileImageUri = null,
                    isProfileImageMarkedForDeletion = false,
                    usernameErrorMessage = null,
                    profileSaveErrorMessage = null,
                    profileSaveToastMessage = null
                )
            }

            runCatching {
                val profileImageUrl = when {
                    current.draftProfileImageUri != null -> uploadProfileImage(current.draftProfileImageUri)
                    current.isProfileImageMarkedForDeletion -> null
                    else -> current.draftProfileImageUrl
                }

                val response = userRepository.updateMyProfile(
                    username = current.draftUsername,
                    profileImageUrl = profileImageUrl
                )
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
                val updatedProfile = response.body()
                SavedProfile(
                    username = updatedProfile?.username ?: current.draftUsername.trim(),
                    profileImageUrl = updatedProfile?.profileImageUrl ?: profileImageUrl
                )
            }.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            isSavingProfile = false,
                            isEditingProfile = false,
                            username = profile.username,
                            profileImageUrl = profile.profileImageUrl,
                            draftUsername = profile.username,
                            draftProfileImageUrl = profile.profileImageUrl,
                            draftProfileImageUri = null,
                            isProfileImageMarkedForDeletion = false,
                            usernameErrorMessage = null,
                            profileSaveErrorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingProfile = false,
                            username = previousUsername,
                            profileImageUrl = previousProfileImageUrl,
                            draftUsername = previousUsername,
                            draftProfileImageUrl = previousProfileImageUrl,
                            draftProfileImageUri = null,
                            isProfileImageMarkedForDeletion = false,
                            profileSaveErrorMessage = null,
                            profileSaveToastMessage = mapProfileSaveErrorMessage(error)
                        )
                    }
                }
            )
        }
    }

    fun onProfileSaveToastConsumed() {
        _uiState.update {
            it.copy(profileSaveToastMessage = null)
        }
    }

    fun logout() {
        authTokenStorage.clearAccessToken()
        _uiState.update {
            it.copy(isLoggedOut = true)
        }
    }

    fun deleteMyAccount() {
        if (_uiState.value.isDeletingAccount) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDeletingAccount = true,
                    accountDeletionErrorMessage = null
                )
            }

            runCatching {
                val response = userRepository.deleteMyAccount()
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
            }.fold(
                onSuccess = {
                    authTokenStorage.clearAccessToken()
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            isLoggedOut = true,
                            accountDeletionErrorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            accountDeletionErrorMessage = mapAccountDeletionErrorMessage(error)
                        )
                    }
                }
            )
        }
    }

    fun onLogoutNavigationConsumed() {
        _uiState.update {
            it.copy(isLoggedOut = false)
        }
    }

    private suspend fun uploadProfileImage(uri: Uri): String {
        val imageFile = copyUriToCacheFile(uri)
        val mimeType = appContext.contentResolver.getType(uri)
        val response = userRepository.uploadMyProfileImage(
            imageFile = imageFile,
            mimeType = mimeType
        )
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val url = response.body()?.url
        require(!url.isNullOrBlank()) {
            "Profile image upload response was empty."
        }
        return url
    }

    private fun copyUriToCacheFile(uri: Uri): File {
        val extension = when (appContext.contentResolver.getType(uri)) {
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            else -> ".jpg"
        }
        val file = File.createTempFile("profile-image-", extension, appContext.cacheDir)
        appContext.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) {
                "Could not open selected image."
            }
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun validateDraftUsername(username: String): String? {
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

    private fun mapProfileSaveErrorMessage(error: Throwable): String {
        return when (error) {
            is IllegalArgumentException -> error.message ?: "프로필 입력값을 확인해 주세요."
            else -> mapErrorMessage(error)
        }
    }

    private fun mapAccountDeletionErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "로그인이 필요합니다."
                403 -> "회원 탈퇴 권한이 없습니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "회원 탈퇴에 실패했습니다. (${error.code()})"
            }
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            else -> error.message ?: "회원 탈퇴에 실패했습니다."
        }
    }

    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "No internet connection. Please check your network."
            is SocketTimeoutException -> "The request timed out. Please try again."
            is HttpException -> when (error.code()) {
                401 -> "Login is required."
                403 -> "You do not have permission to view this profile."
                404 -> "Profile information could not be found."
                in 500..599 -> "Server error occurred. Please try again later."
                else -> "Failed to load profile with code ${error.code()}."
            }
            is IOException -> "Network error occurred. Please try again."
            else -> error.message ?: "Failed to load profile."
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

private data class SavedProfile(
    val username: String,
    val profileImageUrl: String?
)
