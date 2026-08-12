package com.example.hangsha_android.ui.view.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.model.EventSearchHighlightResponse
import com.example.hangsha_android.data.network.model.EventSearchItemResponse
import com.example.hangsha_android.data.network.model.EventSummaryResponse
import com.example.hangsha_android.data.repository.CategoryRepository
import com.example.hangsha_android.data.repository.EventRepository
import com.example.hangsha_android.ui.view.event.eventTypeLabel
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

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    fun onInputChanged(value: String) {
        _uiState.update { it.copy(input = value.take(QUERY_MAX_LENGTH)) }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.value = SearchUiState()
    }

    fun search() {
        val query = _uiState.value.input.trim()
        if (query.isBlank()) {
            clearSearch()
            return
        }
        loadPage(query = query, page = 1, reset = true)
    }

    fun retry() {
        val query = _uiState.value.submittedQuery.ifBlank { _uiState.value.input.trim() }
        if (query.isNotBlank()) loadPage(query = query, page = 1, reset = true)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (!state.canLoadMore || state.submittedQuery.isBlank()) return
        loadPage(query = state.submittedQuery, page = state.page + 1, reset = false)
    }

    private fun loadPage(query: String, page: Int, reset: Boolean) {
        if (reset) searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    submittedQuery = query,
                    isLoading = reset,
                    isLoadingMore = !reset,
                    hasSearched = true,
                    errorMessage = null,
                    items = if (reset) emptyList() else it.items,
                    total = if (reset) 0 else it.total,
                    page = if (reset) 0 else it.page
                )
            }

            runCatching {
                val response = eventRepository.searchEvents(
                    query = query,
                    page = page,
                    size = PAGE_SIZE
                )
                if (!response.isSuccessful) throw HttpException(response)
                response.body() ?: throw IllegalStateException("Search response was empty.")
            }.fold(
                onSuccess = { response ->
                    val names = categoryRepository.eventTypeNames.value
                    val mappedItems = response.items.orEmpty().map { it.toSearchEventItem(names) }
                    _uiState.update { current ->
                        val items = if (reset) mappedItems else current.items + mappedItems
                        current.copy(
                            items = items.distinctBy(SearchEventItem::id),
                            total = response.total,
                            page = response.page,
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage = mapSearchErrorMessage(error)
                        )
                    }
                }
            )
        }
    }

    private fun mapSearchErrorMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is HttpException -> when (error.code()) {
                400 -> "검색어를 확인해 주세요."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "검색 결과를 불러오지 못했습니다. (${error.code()})"
            }
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            else -> "검색 결과를 불러오지 못했습니다."
        }
    }

    private companion object {
        const val QUERY_MAX_LENGTH = 50
        const val PAGE_SIZE = 20
    }
}

private val SearchFullDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA)

private fun EventSearchItemResponse.toSearchEventItem(
    eventTypeNames: Map<Long, String>
): SearchEventItem {
    return event.toSearchEventItem(
        highlight = highlight,
        eventTypeNames = eventTypeNames
    )
}

private fun EventSummaryResponse.toSearchEventItem(
    highlight: EventSearchHighlightResponse?,
    eventTypeNames: Map<Long, String>
): SearchEventItem {
    val applyEndDate = parseSearchDate(applyEnd)
    return SearchEventItem(
        id = id,
        title = title,
        snippet = highlight?.contentSnippet.toPlainSearchText(),
        imageUrl = imageUrl,
        organization = organization,
        eventDateDisplay = formatSearchPeriod(eventStart, eventEnd),
        dDayLabel = applyEndDate.toSearchDDay(),
        eventTypeId = eventTypeId,
        eventTypeLabel = eventTypeNames[eventTypeId] ?: eventTypeLabel(eventTypeId)
    )
}

private fun String?.toPlainSearchText(): String? {
    return this
        ?.replace(Regex("<[^>]*>"), "")
        ?.replace("&nbsp;", " ")
        ?.replace("&amp;", "&")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}

private fun LocalDate?.toSearchDDay(): String {
    val target = this ?: return "-"
    val diff = target.toEpochDay() - LocalDate.now().toEpochDay()
    return when {
        diff == 0L -> "D-DAY"
        diff > 0L -> "D-$diff"
        else -> "D$diff"
    }
}

private fun formatSearchPeriod(startValue: String?, endValue: String?): String {
    val start = parseSearchDate(startValue)
    val end = parseSearchDate(endValue)
    return when {
        start != null && end != null && start == end -> start.format(SearchFullDateFormatter)
        start != null && end != null ->
            "${start.format(SearchFullDateFormatter)} ~ ${end.format(SearchFullDateFormatter)}"
        start != null -> start.format(SearchFullDateFormatter)
        end != null -> end.format(SearchFullDateFormatter)
        else -> "-"
    }
}

private fun parseSearchDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(value).toLocalDate() }.getOrElse {
        runCatching { LocalDateTime.parse(value).toLocalDate() }.getOrElse {
            runCatching { LocalDate.parse(value) }.getOrNull()
        }
    }
}
