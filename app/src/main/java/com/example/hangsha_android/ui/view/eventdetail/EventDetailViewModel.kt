package com.example.hangsha_android.ui.view.eventdetail

import com.example.hangsha_android.data.local.StoredGuestBookmarkSnapshot

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.model.EventDetailResponse
import com.example.hangsha_android.data.network.model.MemoResponse
import com.example.hangsha_android.data.repository.BookmarkRepository
import com.example.hangsha_android.data.repository.EventRepository
import com.example.hangsha_android.data.repository.MemoRepository
import com.example.hangsha_android.ui.navigation.HangshaDestinations
import com.example.hangsha_android.ui.view.event.eventTypeColor
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
                    isBookmarked = shouldBookmark,
                    guestSnapshot = if (shouldBookmark) currentItem.toGuestBookmarkSnapshot() else null
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
                it.copy(memoSaveMessage = "메모를 입력해주세요.")
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
                        tagNames = tagNames,
                        eventTitle = currentItem.title
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
                                memoSaveMessage = "메모가 삭제되었습니다."
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
                                memoSaveMessage = "메모가 저장되었습니다."
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isMemoSaving = false,
                            memoSaveMessage = mapMemoErrorMessage(error)
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
                    errorMessage = "\uC62C\uBC14\uB974\uC9C0 \uC54A\uC740 \uD589\uC0AC ID\uC785\uB2C8\uB2E4.",
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
                bookmarkRepository.syncKnownRemoteBookmarks(
                    remoteBookmarks = mapOf(response.id to response.isBookmarked),
                    sourceUserId = sourceUserId
                )
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
            is UnknownHostException -> "\uC778\uD130\uB137 \uC5F0\uACB0\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            is SocketTimeoutException -> "\uC694\uCCAD \uC2DC\uAC04\uC774 \uCD08\uACFC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is HttpException -> when (error.code()) {
                400 -> "\uD589\uC0AC \uC694\uCCAD\uC774 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."
                401 -> "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."
                403 -> "\uC774 \uD589\uC0AC\uB97C \uBCFC \uAD8C\uD55C\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."
                404 -> "\uD589\uC0AC \uC815\uBCF4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."
                in 500..599 -> "\uC11C\uBC84 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
                else -> "\uD589\uC0AC\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4. (${error.code()})"
            }
            is IOException -> "\uB124\uD2B8\uC6CC\uD06C \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is IllegalStateException -> "\uD589\uC0AC\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
            else -> "\uD589\uC0AC\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
        }
    }

    private fun mapBookmarkErrorMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "\uC778\uD130\uB137 \uC5F0\uACB0\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            is SocketTimeoutException -> "\uC694\uCCAD \uC2DC\uAC04\uC774 \uCD08\uACFC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is HttpException -> when (error.code()) {
                400 -> "\uBD81\uB9C8\uD06C \uC694\uCCAD\uC774 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."
                401 -> "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."
                403 -> "\uC774 \uBD81\uB9C8\uD06C\uB97C \uBCC0\uACBD\uD560 \uAD8C\uD55C\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."
                404 -> "\uD589\uC0AC \uC815\uBCF4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."
                in 500..599 -> "\uC11C\uBC84 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
                else -> "\uBD81\uB9C8\uD06C\uB97C \uBCC0\uACBD\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4. (${error.code()})"
            }
            is IOException -> "\uB124\uD2B8\uC6CC\uD06C \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            else -> "\uBD81\uB9C8\uD06C\uB97C \uBCC0\uACBD\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
        }
    }

    private fun mapMemoErrorMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "\uC778\uD130\uB137 \uC5F0\uACB0\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            is SocketTimeoutException -> "\uC694\uCCAD \uC2DC\uAC04\uC774 \uCD08\uACFC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is HttpException -> when (error.code()) {
                400 -> "\uBA54\uBAA8 \uC694\uCCAD\uC774 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."
                401 -> "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."
                403 -> "\uBA54\uBAA8\uB97C \uC791\uC131\uD560 \uAD8C\uD55C\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."
                404 -> "\uD589\uC0AC \uC815\uBCF4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."
                in 500..599 -> "\uC11C\uBC84 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
                else -> "\uBA54\uBAA8\uB97C \uC800\uC7A5\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4. (${error.code()})"
            }
            is IOException -> "\uB124\uD2B8\uC6CC\uD06C \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is IllegalStateException -> "\uBA54\uBAA8\uB97C \uC800\uC7A5\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
            else -> "\uBA54\uBAA8\uB97C \uC800\uC7A5\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
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

private val DetailDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.KOREA)
private val DetailDateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.KOREA)
private val DetailFullDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA)
private val DetailMonthDayFormatter = DateTimeFormatter.ofPattern("MM.dd", Locale.KOREA)

private fun EventDetailItem.toGuestBookmarkSnapshot(): StoredGuestBookmarkSnapshot {
    return StoredGuestBookmarkSnapshot(
        eventId = id,
        title = title,
        imageUrl = imageUrl,
        organization = organization,
        dDayLabel = dDayLabel,
        applyPeriodDisplay = applyPeriodDisplay,
        eventTypeId = 0L,
        updatedAt = OffsetDateTime.now().toString()
    )
}
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
    val eventEndDate = parseEventDate(eventEnd)
    val dDayLabel = eventEndDate?.let { targetDate ->
        val diff = targetDate.toEpochDay() - LocalDate.now().toEpochDay()
        when {
            diff == 0L -> "D-DAY"
            diff > 0L -> "D-$diff"
            else -> "D$diff"
        }
    } ?: "-"

    return EventDetailItem(
        id = id,
        title = title,
        imageUrl = imageUrl,
        organization = organization,
        location = location,
        eventEndDisplay = formatEventEnd(eventEnd)
            ?: eventEndDate?.format(DetailDateFormatter)
            ?: "-",
        applyPeriodDisplay = formatPeriod(applyStart, applyEnd),
        dDayLabel = dDayLabel,
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

private fun formatEventEnd(value: String?): String? {
    if (value.isNullOrBlank()) {
        return null
    }

    return runCatching {
        OffsetDateTime.parse(value).toLocalDateTime().format(DetailDateFormatter)
    }.getOrElse {
        runCatching { LocalDateTime.parse(value).format(DetailDateFormatter) }.getOrElse {
            runCatching { LocalDate.parse(value).format(DetailDateFormatter) }.getOrNull()
        }
    }
}

private fun parseEventDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) {
        return null
    }

    return runCatching { OffsetDateTime.parse(value).toLocalDate() }.getOrElse {
        runCatching { LocalDateTime.parse(value).toLocalDate() }.getOrElse {
            runCatching { LocalDate.parse(value) }.getOrNull()
        }
    }
}

private fun eventTypeLabel(eventTypeId: Long): String {
    return when (eventTypeId) {
        4L -> "교육(특강/세미나)"
        5L -> "공모전/경진대회"
        6L -> "창업/현장실습"
        7L -> "사회공헌(봉사)"
        8L -> "취업/진로상담"
        39L -> "OpenLNL"
        else -> "기타"
    }
}

private fun formatDateTime(value: String?): String? {
    if (value.isNullOrBlank()) {
        return null
    }

    return runCatching {
        OffsetDateTime.parse(value).toLocalDateTime().format(DetailDateTimeFormatter)
    }.getOrElse {
        runCatching { LocalDateTime.parse(value).format(DetailDateTimeFormatter) }.getOrElse {
            runCatching { LocalDate.parse(value).format(DetailDateFormatter) }.getOrNull()
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
