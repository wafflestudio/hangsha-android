package com.example.hangsha_android.ui.view.mypage

import com.example.hangsha_android.util.toHangshaDate
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.local.BookmarksLocalDataSource
import com.example.hangsha_android.data.local.ExcludedKeywordsLocalDataSource
import com.example.hangsha_android.data.local.ProfileImageFilePreparer
import com.example.hangsha_android.data.network.model.EventSummaryResponse
import com.example.hangsha_android.data.network.model.MemoResponse
import com.example.hangsha_android.data.repository.AuthRepository
import com.example.hangsha_android.data.repository.BookmarkRepository
import com.example.hangsha_android.data.repository.BugReportRepository
import com.example.hangsha_android.data.repository.MemoRepository
import com.example.hangsha_android.data.repository.UserRepository
import com.example.hangsha_android.ui.view.bookmarks.BookmarkedEventItem
import com.example.hangsha_android.ui.view.event.formatApplicationDeadlineLabel
import com.example.hangsha_android.ui.view.event.formatEventCountdownLabel
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val bugReportRepository: BugReportRepository,
    private val memoRepository: MemoRepository,
    private val authTokenStorage: AuthTokenStorage,
    private val bookmarksLocalDataSource: BookmarksLocalDataSource,
    private val excludedKeywordsLocalDataSource: ExcludedKeywordsLocalDataSource,
    private val profileImageFilePreparer: ProfileImageFilePreparer
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()
    private var isBookmarksPreviewInFlight = false
    private var isMemosPreviewInFlight = false

    init {
        loadMyProfile()
        loadBookmarkedEventPreview()
        loadMemoPreview()
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
                    val sortedInterests = profile.interestCategories
                        .orEmpty()
                        .sortedBy { interest -> interest.priority }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            username = profile.username.orEmpty(),
                            email = profile.email.orEmpty(),
                            profileImageUrl = profile.profileImageUrl,
                            draftUsername = profile.username.orEmpty(),
                            draftProfileImageUrl = profile.profileImageUrl,
                            draftProfileImageUri = null,
                            isProfileImageMarkedForDeletion = false,
                            interests = sortedInterests.map { interest -> interest.name },
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

    fun loadBookmarkedEventPreview() {
        if (isBookmarksPreviewInFlight) {
            return
        }

        isBookmarksPreviewInFlight = true
        viewModelScope.launch {
            val sourceUserId = authTokenStorage.getCurrentUserId()
            try {
                _uiState.update {
                    it.copy(
                        isBookmarksPreviewLoading = it.bookmarkedEvents.isEmpty(),
                        bookmarksPreviewErrorMessage = null
                    )
                }

                runCatching {
                    val response = bookmarkRepository.getMyBookmarks(
                        page = BOOKMARKS_PREVIEW_PAGE,
                        size = BOOKMARKS_PREVIEW_SIZE
                    )
                    if (!response.isSuccessful) {
                        throw HttpException(response)
                    }

                    response.body() ?: throw IllegalStateException("Bookmarks response was empty.")
                }.fold(
                    onSuccess = { body ->
                        val bookmarkedEvents = body.items.map { event -> event.toBookmarkedEventItem() }
                        _uiState.update {
                            it.copy(
                                bookmarkedEvents = bookmarkedEvents,
                                hasMoreBookmarkedEvents = bookmarkedEvents.size >= BOOKMARKS_PREVIEW_SIZE,
                                isBookmarksPreviewLoading = false,
                                bookmarksPreviewErrorMessage = null
                            )
                        }
                        bookmarkRepository.syncKnownRemoteBookmarks(
                            remoteBookmarks = bookmarkedEvents.associate { event -> event.id to event.isBookmarked },
                            sourceUserId = sourceUserId
                        )
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isBookmarksPreviewLoading = false,
                                bookmarksPreviewErrorMessage = mapBookmarksPreviewErrorMessage(error)
                            )
                        }
                    }
                )
            } finally {
                isBookmarksPreviewInFlight = false
            }
        }
    }

    fun loadMemoPreview() {
        if (isMemosPreviewInFlight) {
            return
        }

        isMemosPreviewInFlight = true
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isMemosPreviewLoading = it.memoItems.isEmpty(),
                        memosPreviewErrorMessage = null
                    )
                }

                runCatching {
                    val response = memoRepository.getMemos()
                    if (!response.isSuccessful) {
                        throw HttpException(response)
                    }

                    response.body() ?: throw IllegalStateException("Memos response was empty.")
                }.fold(
                    onSuccess = { body ->
                        _uiState.update {
                            it.copy(
                                memoItems = body.items.map { memo -> memo.toMyPageMemoItem() },
                                isMemosPreviewLoading = false,
                                memosPreviewErrorMessage = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isMemosPreviewLoading = false,
                                memosPreviewErrorMessage = mapMemosPreviewErrorMessage(error)
                            )
                        }
                    }
                )
            } finally {
                isMemosPreviewInFlight = false
            }
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

    fun onBugReportTitleChanged(value: String) {
        _uiState.update {
            it.copy(
                bugReportTitle = value,
                bugReportToastMessage = null
            )
        }
    }

    fun onBugReportContentChanged(value: String) {
        _uiState.update {
            it.copy(
                bugReportContent = value,
                bugReportToastMessage = null
            )
        }
    }

    fun submitBugReport() {
        val current = _uiState.value
        val title = current.bugReportTitle.trim()
        val content = current.bugReportContent.trim()

        if (current.isSubmittingBugReport) {
            return
        }
        if (title.isBlank() || content.isBlank()) {
            _uiState.update {
                it.copy(bugReportToastMessage = "\uC81C\uBAA9\uACFC \uB0B4\uC6A9\uC744 \uBAA8\uB450 \uC785\uB825\uD574 \uC8FC\uC138\uC694.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmittingBugReport = true,
                    bugReportToastMessage = null
                )
            }

            runCatching {
                val response = bugReportRepository.createBugReport(
                    title = title,
                    content = content
                )
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
            }.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            bugReportTitle = "",
                            bugReportContent = "",
                            isSubmittingBugReport = false,
                            bugReportToastMessage = "\uC624\uB958 \uC81C\uBCF4\uAC00 \uC811\uC218\uB418\uC5C8\uC2B5\uB2C8\uB2E4."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmittingBugReport = false,
                            bugReportToastMessage = mapBugReportErrorMessage(error)
                        )
                    }
                }
            )
        }
    }

    fun onBugReportToastConsumed() {
        _uiState.update {
            it.copy(bugReportToastMessage = null)
        }
    }

    fun logout() {
        val refreshToken = authTokenStorage.getRefreshToken()
        val userId = authTokenStorage.getCurrentUserId()
        authTokenStorage.clearTokens()
        _uiState.update {
            it.copy(isLoggedOut = true)
        }

        viewModelScope.launch {
            userId?.let { clearUserCache(it) }
            if (!refreshToken.isNullOrBlank()) {
                runCatching {
                    authRepository.logout(refreshToken)
                }
            }
        }
    }

    fun deleteMyAccount() {
        if (_uiState.value.isDeletingAccount) {
            return
        }

        val userId = authTokenStorage.getCurrentUserId()
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
                    authTokenStorage.clearTokens()
                    userId?.let { clearUserCache(it) }
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

    private suspend fun clearUserCache(userId: Long) {
        bookmarksLocalDataSource.clearUserData(userId)
        excludedKeywordsLocalDataSource.clearUserData(userId)
        // The former shared authenticated keys cannot be associated with a user safely.
        bookmarksLocalDataSource.clearLegacyAuthenticatedData()
        excludedKeywordsLocalDataSource.clearLegacyAuthenticatedData()
    }

    private suspend fun uploadProfileImage(uri: Uri): String {
        val preparedImage = profileImageFilePreparer.prepare(uri)
        try {
            val response = userRepository.uploadMyProfileImage(
                imageFile = preparedImage.file,
                mimeType = preparedImage.mimeType
            )
            if (!response.isSuccessful) {
                throw HttpException(response)
            }

            val url = response.body()?.url
            require(!url.isNullOrBlank()) {
                "프로필 이미지 업로드 응답이 비어 있습니다."
            }
            return url
        } finally {
            profileImageFilePreparer.delete(preparedImage.file)
        }
    }

    private fun validateDraftUsername(username: String): String? {
        val trimmedUsername = username.trim()
        if (trimmedUsername.isBlank()) {
            return "\uB2C9\uB124\uC784\uC744 \uC785\uB825\uD574 \uC8FC\uC138\uC694."
        }

        val maxLength = if (trimmedUsername.any { char -> char.isKorean() }) {
            KOREAN_USERNAME_MAX_LENGTH
        } else {
            ENGLISH_USERNAME_MAX_LENGTH
        }
        return if (trimmedUsername.length > maxLength) {
            "\uB2C9\uB124\uC784\uC740 ${maxLength}\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574 \uC8FC\uC138\uC694."
        } else {
            null
        }
    }

    private fun mapProfileSaveErrorMessage(error: Throwable): String {
        return when (error) {
            is IllegalArgumentException ->
                error.message ?: "프로필 입력 정보를 확인해 주세요."
            else -> mapErrorMessage(error)
        }
    }

    private fun mapAccountDeletionErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."
                403 -> "\uACC4\uC815\uC744 \uC0AD\uC81C\uD560 \uAD8C\uD55C\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."
                in 500..599 -> "\uC11C\uBC84 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
                else -> "\uACC4\uC815\uC744 \uC0AD\uC81C\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4. (${error.code()})"
            }
            is UnknownHostException -> "\uC778\uD130\uB137 \uC5F0\uACB0\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            is SocketTimeoutException -> "\uC694\uCCAD \uC2DC\uAC04\uC774 \uCD08\uACFC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is IOException -> "\uB124\uD2B8\uC6CC\uD06C \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            else -> "\uACC4\uC815\uC744 \uC0AD\uC81C\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
        }
    }

    private fun mapBugReportErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                400 -> "\uC624\uB958 \uC81C\uBCF4 \uB0B4\uC6A9\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
                401 -> "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."
                in 500..599 -> "\uC11C\uBC84 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
                else -> "\uC624\uB958 \uC81C\uBCF4\uB97C \uC81C\uCD9C\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4. (${error.code()})"
            }
            is UnknownHostException -> "\uC778\uD130\uB137 \uC5F0\uACB0\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            is SocketTimeoutException -> "\uC694\uCCAD \uC2DC\uAC04\uC774 \uCD08\uACFC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is IOException -> "\uB124\uD2B8\uC6CC\uD06C \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            else -> "\uC624\uB958 \uC81C\uBCF4\uB97C \uC81C\uCD9C\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
        }
    }

    private fun mapBookmarksPreviewErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."
                in 500..599 -> "\uC11C\uBC84 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
                else -> "\uBD81\uB9C8\uD06C\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4. (${error.code()})"
            }
            is UnknownHostException -> "\uC778\uD130\uB137 \uC5F0\uACB0\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            is SocketTimeoutException -> "\uC694\uCCAD \uC2DC\uAC04\uC774 \uCD08\uACFC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is IOException -> "\uB124\uD2B8\uC6CC\uD06C \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            else -> "\uBD81\uB9C8\uD06C\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
        }
    }

    private fun mapMemosPreviewErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."
                in 500..599 -> "\uC11C\uBC84 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
                else -> "\uBA54\uBAA8\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4. (${error.code()})"
            }
            is UnknownHostException -> "\uC778\uD130\uB137 \uC5F0\uACB0\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            is SocketTimeoutException -> "\uC694\uCCAD \uC2DC\uAC04\uC774 \uCD08\uACFC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is IOException -> "\uB124\uD2B8\uC6CC\uD06C \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            else -> "\uBA54\uBAA8\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
        }
    }

    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "\uC778\uD130\uB137 \uC5F0\uACB0\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            is SocketTimeoutException -> "\uC694\uCCAD \uC2DC\uAC04\uC774 \uCD08\uACFC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is HttpException -> when (error.code()) {
                401 -> "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."
                403 -> "\uD504\uB85C\uD544\uC744 \uBCFC \uAD8C\uD55C\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."
                404 -> "\uD504\uB85C\uD544 \uC815\uBCF4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."
                in 500..599 -> "\uC11C\uBC84 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
                else -> "\uD504\uB85C\uD544\uC744 \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4. (${error.code()})"
            }
            is IOException -> "\uB124\uD2B8\uC6CC\uD06C \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            else -> "\uD504\uB85C\uD544\uC744 \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
        }
    }
}

private fun Char.isKorean(): Boolean {
    return this in '\uAC00'..'\uD7A3' ||
        this in '\u1100'..'\u11FF' ||
        this in '\u3130'..'\u318F'
}

private fun EventSummaryResponse.toBookmarkedEventItem(): BookmarkedEventItem {
    val applyEndDate = parseDate(applyEnd)
    val eventStartDate = parseDate(eventStart)
    val eventEndDate = parseDate(eventEnd)
    val dDayLabel = formatApplicationDeadlineLabel(applyEndDate)

    return BookmarkedEventItem(
        id = id,
        title = title,
        imageUrl = imageUrl,
        eventTypeId = eventTypeId,
        statusId = statusId,
        dDayLabel = dDayLabel,
        eventDDayLabel = formatEventCountdownLabel(eventStartDate, eventEndDate),
        applyPeriodDisplay = formatPeriod(applyStart, applyEnd),
        organization = organization,
        isBookmarked = true
    )
}

private fun MemoResponse.toMyPageMemoItem(): MyPageMemoItem {
    return MyPageMemoItem(
        id = id,
        eventId = eventId,
        eventTitle = eventTitle,
        content = content,
        tagNames = tags.map { tag -> tag.name },
        updatedDateDisplay = formatMemoDate(updatedAt)
            ?: formatMemoDate(createdAt)
            ?: "-"
    )
}

private fun parseDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) {
        return null
    }

    return runCatching { OffsetDateTime.parse(value).toHangshaDate() }.getOrElse {
        runCatching { LocalDateTime.parse(value).toLocalDate() }.getOrElse {
            runCatching { LocalDate.parse(value) }.getOrNull()
        }
    }
}

private fun formatMemoDate(value: String?): String? {
    val date = parseDate(value)
    return date?.format(FullDateFormatter)
}

private fun formatPeriod(
    startValue: String?,
    endValue: String?
): String {
    val start = parseDate(startValue)
    val end = parseDate(endValue)
    return when {
        start != null && end != null && start.year == end.year ->
            "${start.format(FullDateFormatter)}~${end.format(MonthDayFormatter)}"
        start != null && end != null ->
            "${start.format(FullDateFormatter)}~${end.format(FullDateFormatter)}"
        start != null -> start.format(FullDateFormatter)
        end != null -> end.format(FullDateFormatter)
        else -> "-"
    }
}

private const val ENGLISH_USERNAME_MAX_LENGTH = 20
private const val KOREAN_USERNAME_MAX_LENGTH = 10
private const val BOOKMARKS_PREVIEW_PAGE = 1
private const val BOOKMARKS_PREVIEW_SIZE = 20
private val FullDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA)
private val MonthDayFormatter = DateTimeFormatter.ofPattern("MM.dd", Locale.KOREA)

private data class SavedProfile(
    val username: String,
    val profileImageUrl: String?
)
