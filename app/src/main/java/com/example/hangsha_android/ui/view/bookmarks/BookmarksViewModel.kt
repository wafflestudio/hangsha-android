package com.example.hangsha_android.ui.view.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.model.EventSummaryResponse
import com.example.hangsha_android.data.repository.BookmarkRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()
    private var isFirstPageRefreshInFlight = false

    init {
        loadFirstPage()
    }

    // 첫 페이지 로드
    fun loadFirstPage() {
        if (isFirstPageRefreshInFlight) {
            return
        }

        isFirstPageRefreshInFlight = true
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        items = emptyList(),
                        isInitialLoading = true,
                        isLoadingNextPage = false,
                        hasMore = true,
                        nextPage = INITIAL_PAGE,
                        errorMessage = null
                    )
                }
                loadPage(page = INITIAL_PAGE)
            } finally {
                isFirstPageRefreshInFlight = false
            }
        }
    }

    // 서버 기준 새로고침
    fun refreshFromServerKeepingScroll() {
        val current = _uiState.value
        if (current.isInitialLoading || current.isLoadingNextPage || isFirstPageRefreshInFlight) {
            return
        }

        isFirstPageRefreshInFlight = true
        _uiState.update {
            it.copy(
                isInitialLoading = it.items.isEmpty(),
                isLoadingNextPage = false,
                hasMore = true,
                nextPage = INITIAL_PAGE,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                loadPage(page = INITIAL_PAGE)
            } finally {
                isFirstPageRefreshInFlight = false
            }
        }
    }

    // 다음 페이지 로드
    fun loadNextPage() {
        val current = _uiState.value
        if (
            current.isInitialLoading ||
            current.isLoadingNextPage ||
            isFirstPageRefreshInFlight ||
            !current.hasMore
        ) {
            return
        }

        _uiState.update {
            it.copy(
                isLoadingNextPage = true,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            loadPage(page = current.nextPage)
        }
    }

    // 북마크 해제
    fun removeBookmark(eventId: Long) {
        val current = _uiState.value
        val removedIndex = current.items.indexOfFirst { item -> item.id == eventId }
        val removedItem = current.items.getOrNull(removedIndex) ?: return

        _uiState.update {
            it.copy(
                items = it.items.filterNot { item -> item.id == eventId },
                errorMessage = null
            )
        }

        viewModelScope.launch {
            runCatching {
                bookmarkRepository.setBookmark(
                    eventId = eventId,
                    isBookmarked = false
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        items = it.items.toMutableList().apply {
                            add(removedIndex.coerceIn(0, size), removedItem)
                        },
                        errorMessage = mapBookmarkErrorMessage(error)
                    )
                }
            }
        }
    }

    // 스크롤 위치 저장
    fun saveScrollPosition(
        firstVisibleItemIndex: Int,
        firstVisibleItemOffset: Int,
        firstVisibleItemId: Long?
    ) {
        _uiState.update {
            it.copy(
                savedScrollIndex = firstVisibleItemIndex,
                savedScrollOffset = firstVisibleItemOffset,
                savedAnchorItemId = firstVisibleItemId
            )
        }
    }

    private suspend fun loadPage(page: Int) {
        val sourceUserId = bookmarkRepository.currentUserId()
        runCatching {
            val response = bookmarkRepository.getMyBookmarks(
                page = page,
                size = PAGE_SIZE
            )
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
            response.body() ?: throw IllegalStateException("Bookmarks response was empty.")
        }.fold(
            onSuccess = { body ->
                val newItems = body.items.map { response -> response.toBookmarkedEventItem() }
                val mergedItems = if (page == INITIAL_PAGE) {
                    newItems
                } else {
                    (_uiState.value.items + newItems).distinctBy { item -> item.id }
                }
                val hasMore = if (body.total > 0) {
                    mergedItems.size < body.total
                } else {
                    newItems.size >= PAGE_SIZE
                }

                _uiState.update {
                    it.copy(
                        items = mergedItems,
                        isInitialLoading = false,
                        isLoadingNextPage = false,
                        hasMore = hasMore,
                        nextPage = page + 1,
                        errorMessage = null
                    )
                }
                bookmarkRepository.syncKnownRemoteBookmarks(
                    remoteBookmarks = newItems.associate { item -> item.id to item.isBookmarked },
                    sourceUserId = sourceUserId
                )
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isLoadingNextPage = false,
                        errorMessage = mapLoadErrorMessage(error)
                    )
                }
            }
        )
    }

    private fun mapLoadErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "로그인이 필요합니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "찜 목록을 불러오지 못했습니다. (${error.code()})"
            }
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            else -> error.message ?: "찜 목록을 불러오지 못했습니다."
        }
    }

    private fun mapBookmarkErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "로그인이 필요합니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "북마크 변경에 실패했습니다. (${error.code()})"
            }
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            else -> error.message ?: "북마크 변경에 실패했습니다."
        }
    }
}

private fun EventSummaryResponse.toBookmarkedEventItem(): BookmarkedEventItem {
    val applyEndDate = parseDate(applyEnd)
    val dDayLabel = applyEndDate?.let { targetDate ->
        val diff = targetDate.toEpochDay() - LocalDate.now().toEpochDay()
        when {
            diff == 0L -> "D-day"
            diff > 0L -> "D-$diff"
            else -> "D$diff"
        }
    } ?: "-"

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

private val FullDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA)
private val MonthDayFormatter = DateTimeFormatter.ofPattern("MM.dd", Locale.KOREA)
private const val INITIAL_PAGE = 1
private const val PAGE_SIZE = 20
