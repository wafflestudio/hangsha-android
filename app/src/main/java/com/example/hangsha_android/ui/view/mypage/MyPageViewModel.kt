package com.example.hangsha_android.ui.view.mypage

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.network.model.EventSummaryResponse
import com.example.hangsha_android.data.network.model.MemoResponse
import com.example.hangsha_android.data.repository.AuthRepository
import com.example.hangsha_android.data.repository.BookmarkRepository
import com.example.hangsha_android.data.repository.BugReportRepository
import com.example.hangsha_android.data.repository.MemoRepository
import com.example.hangsha_android.data.repository.UserRepository
import com.example.hangsha_android.ui.view.bookmarks.BookmarkedEventItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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
    @param:ApplicationContext private val appContext: Context
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
                            username = profile.username,
                            email = profile.email,
                            profileImageUrl = profile.profileImageUrl,
                            draftUsername = profile.username,
                            draftProfileImageUrl = profile.profileImageUrl,
                            draftProfileImageUri = null,
                            isProfileImageMarkedForDeletion = false,
                            interests = sortedInterests.map { interest -> interest.category.name },
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
                            bookmarkedEvents.associate { event -> event.id to event.isBookmarked }
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
                it.copy(bugReportToastMessage = "Please enter both a title and description.")
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
                            bugReportToastMessage = "Your bug report was submitted."
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
        viewModelScope.launch {
            val refreshToken = authTokenStorage.getRefreshToken()
            if (!refreshToken.isNullOrBlank()) {
                runCatching {
                    authRepository.logout(refreshToken)
                }
            }

            authTokenStorage.clearTokens()
            _uiState.update {
                it.copy(isLoggedOut = true)
            }
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
                    authTokenStorage.clearTokens()
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
            return "Please enter a username."
        }

        val maxLength = if (trimmedUsername.any { char -> char.isKorean() }) {
            KOREAN_USERNAME_MAX_LENGTH
        } else {
            ENGLISH_USERNAME_MAX_LENGTH
        }
        return if (trimmedUsername.length > maxLength) {
            "Username must be $maxLength characters or less."
        } else {
            null
        }
    }

    private fun mapProfileSaveErrorMessage(error: Throwable): String {
        return when (error) {
            is IllegalArgumentException -> error.message ?: "Please check your profile input."
            else -> mapErrorMessage(error)
        }
    }

    private fun mapAccountDeletionErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "Login is required."
                403 -> "You do not have permission to delete this account."
                in 500..599 -> "A server error occurred. Please try again later."
                else -> "Failed to delete the account. (${error.code()})"
            }
            is UnknownHostException -> "Please check your internet connection."
            is SocketTimeoutException -> "The request timed out. Please try again."
            is IOException -> "A network error occurred. Please try again."
            else -> error.message ?: "Failed to delete the account."
        }
    }

    private fun mapBugReportErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                400 -> "Please check your bug report content."
                401 -> "Login is required."
                in 500..599 -> "A server error occurred. Please try again later."
                else -> "Failed to submit the bug report. (${error.code()})"
            }
            is UnknownHostException -> "Please check your internet connection."
            is SocketTimeoutException -> "The request timed out. Please try again."
            is IOException -> "A network error occurred. Please try again."
            else -> error.message ?: "Failed to submit the bug report."
        }
    }

    private fun mapBookmarksPreviewErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "Login is required."
                in 500..599 -> "A server error occurred. Please try again later."
                else -> "Failed to load bookmarks. (${error.code()})"
            }
            is UnknownHostException -> "Please check your internet connection."
            is SocketTimeoutException -> "The request timed out. Please try again."
            is IOException -> "A network error occurred. Please try again."
            else -> error.message ?: "Failed to load bookmarks."
        }
    }

    private fun mapMemosPreviewErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "Login is required."
                in 500..599 -> "A server error occurred. Please try again later."
                else -> "Failed to load memos. (${error.code()})"
            }
            is UnknownHostException -> "Please check your internet connection."
            is SocketTimeoutException -> "The request timed out. Please try again."
            is IOException -> "A network error occurred. Please try again."
            else -> error.message ?: "Failed to load memos."
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

private fun EventSummaryResponse.toBookmarkedEventItem(): BookmarkedEventItem {
    val applyEndDate = parseDate(applyEnd)
    val dDayLabel = applyEndDate?.let { targetDate ->
        val diff = targetDate.toEpochDay() - LocalDate.now().toEpochDay()
        when {
            diff == 0L -> "Apply D-day"
            diff > 0L -> "Apply D-$diff"
            else -> "Apply D$diff"
        }
    } ?: "Apply -"

    return BookmarkedEventItem(
        id = id,
        title = title,
        imageUrl = imageUrl,
        eventTypeId = eventTypeId,
        statusId = statusId,
        dDayLabel = dDayLabel,
        applyPeriodDisplay = formatPeriod(applyStart, applyEnd),
        organization = organization,
        isBookmarked = isBookmarked
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

    return runCatching { OffsetDateTime.parse(value).toLocalDate() }.getOrElse {
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
