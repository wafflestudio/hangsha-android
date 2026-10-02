package com.example.hangsha_android.ui.view.eventdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.model.EventDetailResponse
import com.example.hangsha_android.data.network.model.MemoResponse
import com.example.hangsha_android.data.repository.BookmarkRepository
import com.example.hangsha_android.data.repository.BugReportRepository
import com.example.hangsha_android.data.repository.EventRepository
import com.example.hangsha_android.data.repository.MemoRepository
import com.example.hangsha_android.ui.components.HangshaToastType
import com.example.hangsha_android.ui.navigation.HangshaDestinations
import com.example.hangsha_android.util.toHangshaDate
import com.example.hangsha_android.ui.view.event.eventTypeColor
import com.example.hangsha_android.ui.view.event.eventTypeLabel
import com.example.hangsha_android.ui.view.event.formatApplicationDeadlineLabel
import com.example.hangsha_android.ui.view.event.formatEventCountdownLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val bugReportRepository: BugReportRepository,
    private val memoRepository: MemoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val eventId = savedStateHandle.get<Long>(HangshaDestinations.EventDetail.eventIdArg) ?: -1L

    private val _uiState = MutableStateFlow(EventDetailUiState(eventId = eventId))
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var memoLoadJob: Job? = null

    init {
        viewModelScope.launch {
            bookmarkRepository.bookmarkedEventIds.collect { eventIds ->
                onBookmarkedEventIdsChanged(eventIds)
            }
        }
        loadEventDetail()
        loadMemoForEvent()
    }

    fun retry() {
        loadEventDetail()
        loadMemoForEvent()
    }

    fun toggleBookmark() {
        val currentItem = _uiState.value.item ?: return
        val shouldBookmark = !currentItem.isBookmarked

        _uiState.update { currentState ->
            currentState.copy(
                errorMessage = null,
                item = currentItem.copy(isBookmarked = shouldBookmark)
            )
        }

        viewModelScope.launch {
            runCatching {
                bookmarkRepository.setBookmark(
                    eventId = currentItem.id,
                    isBookmarked = shouldBookmark
                )
            }.onFailure { error ->
                _uiState.update { currentState ->
                    currentState.copy(
                        errorMessage = mapBookmarkErrorMessage(error),
                        item = currentState.item?.copy(isBookmarked = !shouldBookmark)
                    )
                }
            }
        }
    }

    fun openBugReportDialog() {
        _uiState.update {
            it.copy(isBugReportDialogOpen = true, bugReportMessage = null)
        }
    }

    fun dismissBugReportDialog() {
        if (_uiState.value.isSubmittingBugReport) return
        _uiState.update {
            it.copy(
                isBugReportDialogOpen = false,
                bugReportTitle = "",
                bugReportContent = "",
                bugReportMessage = null
            )
        }
    }

    fun onBugReportTitleChanged(value: String) {
        _uiState.update {
            it.copy(
                bugReportTitle = value.take(BUG_REPORT_TITLE_MAX_LENGTH),
                bugReportMessage = null
            )
        }
    }

    fun onBugReportContentChanged(value: String) {
        _uiState.update {
            it.copy(
                bugReportContent = value.take(BUG_REPORT_CONTENT_MAX_LENGTH),
                bugReportMessage = null
            )
        }
    }

    fun submitBugReport() {
        val current = _uiState.value
        val title = current.bugReportTitle.trim()
        val content = current.bugReportContent.trim()
        if (current.isSubmittingBugReport) return
        if (title.isBlank() || content.isBlank()) {
            _uiState.update {
                it.copy(
                    bugReportMessage = "제목과 내용을 모두 입력해주세요.",
                    bugReportToastType = HangshaToastType.Warning
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSubmittingBugReport = true, bugReportMessage = null)
            }
            runCatching {
                val response = bugReportRepository.createBugReport(title, content)
                if (!response.isSuccessful) throw HttpException(response)
            }.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isBugReportDialogOpen = false,
                            bugReportTitle = "",
                            bugReportContent = "",
                            isSubmittingBugReport = false,
                            bugReportMessage = "오류 제보가 접수되었습니다.",
                            bugReportToastType = HangshaToastType.Success
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmittingBugReport = false,
                            bugReportMessage = mapBugReportErrorMessage(error),
                            bugReportToastType = HangshaToastType.Error
                        )
                    }
                }
            )
        }
    }

    fun onBugReportMessageConsumed() {
        _uiState.update { it.copy(bugReportMessage = null) }
    }

    fun openMemoEditor() {
        _uiState.update {
            val savedMemo = it.savedMemo
            if (savedMemo == null) {
                it.copy(isMemoEditorOpen = true)
            } else {
                it.copy(
                    isMemoEditorOpen = true,
                    memoContent = savedMemo.content,
                    memoTagInput = "",
                    memoTagNames = savedMemo.tagNames
                )
            }
        }
    }

    fun onMemoContentChanged(value: String) {
        _uiState.update {
            it.copy(memoContent = value)
        }
    }

    fun onMemoTagInputChanged(value: String) {
        _uiState.update {
            it.copy(memoTagInput = value)
        }
    }

    fun addMemoTag() {
        val tagName = _uiState.value.memoTagInput.trim()
        if (tagName.isBlank()) {
            return
        }

        _uiState.update {
            it.copy(
                memoTagInput = "",
                memoTagNames = (it.memoTagNames + tagName).distinct()
            )
        }
    }

    fun removeMemoTag(tagName: String) {
        _uiState.update {
            it.copy(memoTagNames = it.memoTagNames - tagName)
        }
    }

    fun saveMemo() {
        val currentState = _uiState.value
        val currentItem = currentState.item ?: return
        val content = currentState.memoContent.trim()
        val tagNames = (currentState.memoTagNames + currentState.memoTagInput.trim())
            .filter { it.isNotBlank() }
            .distinct()
        if (currentState.savedMemo == null && content.isBlank()) {
            _uiState.update {
                it.copy(
                    memoSaveMessage = "메모를 입력해주세요.",
                    memoSaveToastType = HangshaToastType.Warning
                )
            }
            return
        }

        if (currentState.isMemoSaving) {
            return
        }

        _uiState.update {
            it.copy(isMemoSaving = true, memoSaveMessage = null)
        }

        viewModelScope.launch {
            runCatching {
                val savedMemo = currentState.savedMemo
                if (savedMemo == null) {
                    memoRepository.createMemo(
                        eventId = currentItem.id,
                        content = content,
                        tagNames = tagNames
                    ).requireBody("Memo response was empty.")
                } else if (content.isBlank() && tagNames.isEmpty()) {
                    val response = memoRepository.deleteMemo(savedMemo.id)
                    if (!response.isSuccessful) {
                        throw HttpException(response)
                    }
                    null
                } else {
                    memoRepository.updateMemo(
                        memoId = savedMemo.id,
                        content = content,
                        tagNames = tagNames
                    ).requireBody("Memo response was empty.")
                }
            }.fold(
                onSuccess = { memo ->
                    _uiState.update {
                        if (memo == null) {
                            it.copy(
                                isMemoEditorOpen = false,
                                memoContent = "",
                                memoTagInput = "",
                                memoTagNames = emptyList(),
                                savedMemo = null,
                                isMemoSaving = false,
                                memoSaveMessage = "메모가 삭제되었습니다.",
                                memoSaveToastType = HangshaToastType.Success
                            )
                        } else {
                            val savedMemo = memo.toEventDetailMemo()
                            it.copy(
                                isMemoEditorOpen = false,
                                memoContent = savedMemo.content,
                                memoTagInput = "",
                                memoTagNames = savedMemo.tagNames,
                                savedMemo = savedMemo,
                                isMemoSaving = false,
                                memoSaveMessage = "메모가 저장되었습니다.",
                                memoSaveToastType = HangshaToastType.Success
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isMemoSaving = false,
                            memoSaveMessage = mapMemoErrorMessage(error),
                            memoSaveToastType = HangshaToastType.Error
                        )
                    }
                }
            )
        }
    }

    fun onMemoSaveMessageConsumed() {
        _uiState.update {
            it.copy(memoSaveMessage = null)
        }
    }

    private fun loadEventDetail() {
        if (eventId <= 0L) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "올바르지 않은 행사 ID입니다.",
                    item = null
                )
            }
            return
        }

        loadJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        loadJob = viewModelScope.launch {
            val sourceUserId = bookmarkRepository.currentUserId()
            runCatching {
                val response = eventRepository.getEventDetail(eventId)
                    .requireBody("Event detail response was empty.")
                response.isBookmarked?.let { isBookmarked ->
                    bookmarkRepository.syncKnownRemoteBookmarks(
                        remoteBookmarks = mapOf(response.id to isBookmarked),
                        sourceUserId = sourceUserId
                    )
                }
                response.toEventDetailItem(
                    bookmarkedEventIds = bookmarkRepository.currentBookmarkedEventIds()
                )
            }.fold(
                onSuccess = { item ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            item = item
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapErrorMessage(error),
                            item = null
                        )
                    }
                }
            )
        }
    }

    private fun loadMemoForEvent() {
        if (eventId <= 0L) {
            return
        }

        memoLoadJob?.cancel()
        memoLoadJob = viewModelScope.launch {
            runCatching {
                val response = memoRepository.getMemoByEvent(eventId)
                if (response.code() == 404) {
                    null
                } else if (!response.isSuccessful) {
                    throw HttpException(response)
                } else {
                    response.body()?.toEventDetailMemo()
                }
            }.onSuccess { memo ->
                _uiState.update {
                    it.copy(
                        savedMemo = memo,
                        memoContent = memo?.content.orEmpty(),
                        memoTagInput = "",
                        memoTagNames = memo?.tagNames.orEmpty()
                    )
                }
            }
        }
    }

    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is HttpException -> when (error.code()) {
                400 -> "행사 요청이 올바르지 않습니다."
                401 -> "로그인이 필요합니다."
                403 -> "이 행사를 볼 권한이 없습니다."
                404 -> "행사 정보를 찾을 수 없습니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "행사를 불러오지 못했습니다. (${error.code()})"
            }
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            is IllegalStateException -> "행사를 불러오지 못했습니다."
            else -> "행사를 불러오지 못했습니다."
        }
    }

    private fun mapBookmarkErrorMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is HttpException -> when (error.code()) {
                400 -> "북마크 요청이 올바르지 않습니다."
                401 -> "로그인이 필요합니다."
                403 -> "이 북마크를 변경할 권한이 없습니다."
                404 -> "행사 정보를 찾을 수 없습니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "북마크를 변경하지 못했습니다. (${error.code()})"
            }
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            else -> "북마크를 변경하지 못했습니다."
        }
    }

    private fun mapBugReportErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                400 -> "오류 제보 내용을 확인해 주세요."
                401 -> "로그인이 필요합니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "오류 제보를 제출하지 못했습니다. (${error.code()})"
            }
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            else -> "오류 제보를 제출하지 못했습니다."
        }
    }

    private fun mapMemoErrorMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is HttpException -> when (error.code()) {
                400 -> "메모 요청이 올바르지 않습니다."
                401 -> "로그인이 필요합니다."
                403 -> "메모를 작성할 권한이 없습니다."
                404 -> "행사 정보를 찾을 수 없습니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "메모를 저장하지 못했습니다. (${error.code()})"
            }
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            is IllegalStateException -> "메모를 저장하지 못했습니다."
            else -> "메모를 저장하지 못했습니다."
        }
    }

    private fun onBookmarkedEventIdsChanged(eventIds: Set<Long>) {
        _uiState.update { currentState ->
            val currentItem = currentState.item ?: return@update currentState
            currentState.copy(
                item = currentItem.copy(isBookmarked = currentItem.id in eventIds)
            )
        }
    }
}

private const val BUG_REPORT_TITLE_MAX_LENGTH = 100
private const val BUG_REPORT_CONTENT_MAX_LENGTH = 1_000

private val DetailFullDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA)
private val DetailMonthDayFormatter = DateTimeFormatter.ofPattern("MM.dd", Locale.KOREA)

private fun MemoResponse.toEventDetailMemo(): EventDetailMemo {
    return EventDetailMemo(
        id = id,
        eventId = eventId,
        content = content,
        tagNames = tags.map { it.name }
    )
}

private fun EventDetailResponse.toEventDetailItem(
    bookmarkedEventIds: Set<Long>
): EventDetailItem {
    val applyEndDate = parseEventDate(applyEnd)
    val eventStartDate = parseEventDate(eventStart)
    val eventEndDate = parseEventDate(eventEnd)
    val dDayLabel = formatApplicationDeadlineLabel(applyEndDate)

    return EventDetailItem(
        id = id,
        title = title,
        imageUrl = imageUrl,
        organization = organization,
        location = location,
        eventPeriodDisplay = formatPeriod(eventStart, eventEnd),
        applyPeriodDisplay = formatPeriod(applyStart, applyEnd),
        dDayLabel = dDayLabel,
        eventDDayLabel = formatEventCountdownLabel(eventStartDate, eventEndDate),
        eventTypeLabel = eventTypeLabel(eventTypeId),
        eventTypeColor = eventTypeColor(eventTypeId),
        applyLink = applyLink,
        detail = detail,
        isBookmarked = id in bookmarkedEventIds
    )
}

private fun formatPeriod(start: String?, end: String?): String {
    val startDate = parseEventDate(start)
    val endDate = parseEventDate(end)

    return when {
        startDate != null && endDate != null && startDate.year == endDate.year ->
            "${startDate.format(DetailFullDateFormatter)}~${endDate.format(DetailMonthDayFormatter)}"
        startDate != null && endDate != null ->
            "${startDate.format(DetailFullDateFormatter)}~${endDate.format(DetailFullDateFormatter)}"
        startDate != null -> startDate.format(DetailFullDateFormatter)
        endDate != null -> endDate.format(DetailFullDateFormatter)
        else -> "-"
    }
}

private fun parseEventDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) {
        return null
    }

    return runCatching { OffsetDateTime.parse(value).toHangshaDate() }.getOrElse {
        runCatching { LocalDateTime.parse(value).toLocalDate() }.getOrElse {
            runCatching { LocalDate.parse(value) }.getOrNull()
        }
    }
}

private fun <T> Response<T>.requireBody(
    emptyMessage: String
): T {
    if (!isSuccessful) {
        throw HttpException(this)
    }

    return body() ?: throw IllegalStateException(emptyMessage)
}
