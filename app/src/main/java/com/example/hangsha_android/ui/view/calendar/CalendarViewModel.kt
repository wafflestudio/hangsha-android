package com.example.hangsha_android.ui.view.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.model.EventCountResponse
import com.example.hangsha_android.data.network.model.EventSummaryResponse
import com.example.hangsha_android.data.network.model.MonthlyEventsResponse
import com.example.hangsha_android.data.repository.BookmarkRepository
import com.example.hangsha_android.data.repository.CategoryRepository
import com.example.hangsha_android.data.repository.EventRepository
import com.example.hangsha_android.data.repository.ExcludedKeywordsRepository
import com.example.hangsha_android.data.repository.model.CategoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import retrofit2.Response

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val categoryRepository: CategoryRepository,
    private val excludedKeywordsRepository: ExcludedKeywordsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CalendarUiState()
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var filterCountJob: Job? = null

    init {
        viewModelScope.launch {
            categoryRepository.eventTypeNames.collect { eventTypeNames ->
                _uiState.update { state ->
                    state.copy(
                        eventTypeNames = eventTypeNames,
                        availableFilterOptions = state.availableFilterOptions.copy(
                            eventTypeIds = eventTypeNames.keys.toList()
                        )
                    )
                }
            }
        }
        viewModelScope.launch {
            runCatching { categoryRepository.ensureCategoryCatalogLoaded() }
        }
        viewModelScope.launch {
            categoryRepository.organizationNames.collect { organizationNames ->
                _uiState.update { state ->
                    state.copy(
                        organizationNames = organizationNames,
                        availableFilterOptions = state.availableFilterOptions.copy(
                            orgIds = organizationNames.keys.toList()
                        )
                    )
                }
            }
        }
        viewModelScope.launch {
            categoryRepository.eventStatusNames.collect { statusNames ->
                _uiState.update { state ->
                    state.copy(
                        statusNames = statusNames,
                        availableFilterOptions = state.availableFilterOptions.copy(
                            statusIds = statusNames.keys.toList()
                        )
                    )
                }
            }
        }
        viewModelScope.launch {
            categoryRepository.loadedCategoryTypes.collect { loadedTypes ->
                normalizeFiltersForLoadedCatalog(loadedTypes)
            }
        }
        viewModelScope.launch {
            excludedKeywordsRepository.excludedKeywords.collect { keywords ->
                onExcludedKeywordsChanged(keywords)
            }
        }
        viewModelScope.launch {
            bookmarkRepository.bookmarkedEventIds.collect { eventIds ->
                onBookmarkedEventIdsChanged(eventIds)
            }
        }
        loadPeriod(
            anchorDate = _uiState.value.anchorDate,
            period = _uiState.value.period
        )
    }

    fun showPreviousPeriod() {
        val state = _uiState.value
        loadPeriod(
            anchorDate = state.period.move(state.anchorDate, -1),
            period = state.period
        )
    }

    fun showNextPeriod() {
        val state = _uiState.value
        loadPeriod(
            anchorDate = state.period.move(state.anchorDate, 1),
            period = state.period
        )
    }

    fun setPeriod(period: CalendarPeriod) {
        val state = _uiState.value
        if (state.period == period) return
        loadPeriod(
            anchorDate = state.anchorDate,
            period = period
        )
    }

    fun setViewMode(viewMode: CalendarViewMode) {
        _uiState.update { state ->
            if (state.viewMode == viewMode) state else state.copy(viewMode = viewMode)
        }
    }

    fun toggleBookmark(eventId: Long) {
        val currentState = _uiState.value
        val targetEvent = currentState.filterSourceEventsByDate.values
            .asSequence()
            .flatten()
            .firstOrNull { event -> event.id == eventId }
            ?: currentState.eventsByDate.values
                .asSequence()
                .flatten()
                .firstOrNull { event -> event.id == eventId }
            ?: return
        val shouldBookmark = !targetEvent.isBookmarked

        _uiState.update { state ->
            state.withUpdatedBookmark(
                eventId = eventId,
                isBookmarked = shouldBookmark
            ).copy(errorMessage = null)
        }

        viewModelScope.launch {
            runCatching {
                bookmarkRepository.setBookmark(
                    eventId = eventId,
                    isBookmarked = shouldBookmark
                )
            }.onFailure { error ->
                _uiState.update { state ->
                    state.withUpdatedBookmark(
                        eventId = eventId,
                        isBookmarked = !shouldBookmark
                    ).copy(errorMessage = mapBookmarkErrorMessage(error))
                }
            }
        }
    }

    fun retry() {
        val state = _uiState.value
        loadPeriod(
            anchorDate = state.anchorDate,
            period = state.period,
            filters = state.appliedFilters,
            hasAppliedServerFilters = state.hasAppliedServerFilters
        )
    }

    fun restoreAppliedFilters(
        filters: CalendarFilterState,
        hasAppliedServerFilters: Boolean
    ) {
        val normalizedFilters = filters.copy(
            excludedKeywords = excludedKeywordsRepository.currentExcludedKeywords()
        ).normalizedAgainstCatalog(categoryRepository.loadedCategoryTypes.value)
        val currentState = _uiState.value
        if (
            currentState.appliedFilters == normalizedFilters &&
            currentState.hasAppliedServerFilters == hasAppliedServerFilters
        ) {
            return
        }

        _uiState.update {
            it.copy(
                appliedFilters = normalizedFilters,
                draftFilters = normalizedFilters,
                hasAppliedServerFilters = normalizedFilters.hasActiveFilters,
                selectedFilterTab = CalendarFilterTab.EVENT_TYPE,
                excludeKeywordInput = "",
                isFilterSheetVisible = false,
                errorMessage = null
            )
        }
        loadPeriod(
            anchorDate = currentState.anchorDate,
            period = currentState.period,
            filters = normalizedFilters,
            hasAppliedServerFilters = normalizedFilters.hasActiveFilters
        )
    }

    fun openFilterSheet() {
        val currentKeywords = excludedKeywordsRepository.currentExcludedKeywords()
        _uiState.update {
            it.copy(
                isFilterSheetVisible = true,
                draftFilters = it.appliedFilters.copy(excludedKeywords = currentKeywords),
                selectedFilterTab = CalendarFilterTab.EVENT_TYPE,
                excludeKeywordInput = ""
            )
        }
        requestFilterCount()
    }

    fun dismissFilterSheet() {
        filterCountJob?.cancel()
        _uiState.update {
            it.copy(
                isFilterSheetVisible = false,
                draftFilters = it.appliedFilters,
                selectedFilterTab = CalendarFilterTab.EVENT_TYPE,
                excludeKeywordInput = "",
                filteredEventCount = null,
                isFilterCountLoading = false
            )
        }
    }

    fun clearDraftFilters() {
        _uiState.update {
            it.copy(
                draftFilters = it.draftFilters.resetSelections(),
                excludeKeywordInput = ""
            )
        }
        requestFilterCount()
    }
    fun selectFilterTab(tab: CalendarFilterTab) {
        _uiState.update { it.copy(selectedFilterTab = tab) }
    }

    fun toggleDraftOrgId(orgId: Long) {
        _uiState.update {
            it.copy(
                draftFilters = it.draftFilters.copy(
                    orgIds = it.draftFilters.orgIds.toggle(orgId)
                )
            )
        }
        requestFilterCount()
    }

    fun toggleDraftStatus(statusId: Long) {
        _uiState.update {
            it.copy(
                draftFilters = it.draftFilters.copy(
                    statusIds = it.draftFilters.statusIds.toggle(statusId)
                )
            )
        }
        requestFilterCount()
    }

    fun toggleDraftEventType(eventTypeId: Long) {
        _uiState.update {
            it.copy(
                draftFilters = it.draftFilters.copy(
                    eventTypeIds = it.draftFilters.eventTypeIds.toggle(eventTypeId)
                )
            )
        }
        requestFilterCount()
    }
    fun updateExcludeKeywordInput(value: String) {
        _uiState.update { it.copy(excludeKeywordInput = value) }
    }

    fun addDraftExcludeKeyword() {
        val keyword = _uiState.value.excludeKeywordInput.trim()
        if (keyword.isBlank()) return

        if (keyword in _uiState.value.draftFilters.excludedKeywords) {
            _uiState.update { it.copy(excludeKeywordInput = "") }
            return
        }

        viewModelScope.launch {
            runCatching {
                excludedKeywordsRepository.addExcludedKeyword(keyword)
            }.onSuccess {
                _uiState.update { it.copy(excludeKeywordInput = "") }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = mapExcludedKeywordErrorMessage(error)) }
            }
        }
    }

    fun removeDraftExcludeKeyword(keyword: String) {
        viewModelScope.launch {
            runCatching {
                excludedKeywordsRepository.removeExcludedKeyword(keyword)
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = mapExcludedKeywordErrorMessage(error)) }
            }
        }
    }

    fun applyDraftFilters() {
        filterCountJob?.cancel()
        val state = _uiState.value
        val appliedFilters = state.draftFilters
        _uiState.update {
            it.copy(
                appliedFilters = appliedFilters,
                draftFilters = appliedFilters,
                hasAppliedServerFilters = true,
                selectedFilterTab = CalendarFilterTab.EVENT_TYPE,
                excludeKeywordInput = "",
                isFilterSheetVisible = false,
                filteredEventCount = null,
                isFilterCountLoading = false,
                errorMessage = null
            )
        }
        loadPeriod(
            anchorDate = state.anchorDate,
            period = state.period,
            filters = appliedFilters,
            hasAppliedServerFilters = true
        )
    }
    private fun requestFilterCount() {
        val state = _uiState.value
        if (!state.isFilterSheetVisible) return

        val anchorDate = state.anchorDate
        val period = state.period
        val contentRange = state.contentRange
        val filters = state.draftFilters
        filterCountJob?.cancel()
        _uiState.update {
            it.copy(
                filteredEventCount = null,
                isFilterCountLoading = true
            )
        }

        filterCountJob = viewModelScope.launch {
            try {
                delay(FILTER_COUNT_DEBOUNCE_MS)
                val count = withTimeout(FILTER_COUNT_TIMEOUT_MS) {
                    eventRepository.getEventCount(
                        range = contentRange,
                        filters = filters
                    ).requireCount()
                }
                _uiState.update { current ->
                    if (
                        current.isFilterSheetVisible &&
                        current.anchorDate == anchorDate &&
                        current.period == period &&
                        current.draftFilters == filters
                    ) {
                        current.copy(
                            filteredEventCount = count,
                            isFilterCountLoading = false
                        )
                    } else {
                        current
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.update { current ->
                    if (
                        current.isFilterSheetVisible &&
                        current.anchorDate == anchorDate &&
                        current.period == period &&
                        current.draftFilters == filters
                    ) {
                        current.copy(
                            filteredEventCount = null,
                            isFilterCountLoading = false
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }
    // 현재 월의 전체 source 데이터를 먼저 가져오고,
    // 그다음 화면 표시용 데이터만 분기해서 구성한다.
    private fun loadPeriod(
        anchorDate: LocalDate,
        period: CalendarPeriod,
        filters: CalendarFilterState = _uiState.value.appliedFilters,
        hasAppliedServerFilters: Boolean = _uiState.value.hasAppliedServerFilters,
        preserveFilterSheetState: Boolean = false
    ) {
        val visibleRange = period.visibleRange(anchorDate)
        val visibleDates = visibleRange.toDateList()

        loadJob?.cancel()
        _uiState.update {
            it.copy(
                anchorDate = anchorDate,
                period = period,
                visibleRange = visibleRange,
                visibleDates = visibleDates,
                appliedFilters = filters,
                hasAppliedServerFilters = hasAppliedServerFilters,
                isLoading = true,
                errorMessage = null,
                isFilterSheetVisible = if (preserveFilterSheetState) it.isFilterSheetVisible else false,
                draftFilters = if (preserveFilterSheetState) it.draftFilters else filters,
                selectedFilterTab = if (preserveFilterSheetState) it.selectedFilterTab else CalendarFilterTab.EVENT_TYPE,
                excludeKeywordInput = if (preserveFilterSheetState) it.excludeKeywordInput else ""
            )
        }

        loadJob = viewModelScope.launch {
            val sourceUserId = bookmarkRepository.currentUserId()
            runCatching {
                val response = eventRepository.getEvents(
                    range = visibleRange,
                    filters = filters
                )
                val body = response.requireBody("Events response was empty.")
                bookmarkRepository.syncKnownRemoteBookmarks(body.toBookmarkMap(), sourceUserId)
                val visibleEventsByDate = body.toCalendarEventsByDate()
                val filterOptions = buildFilterOptions()

                CalendarPeriodLoadResult(
                    filterSourceEventsByDate = visibleEventsByDate,
                    visibleEventsByDate = visibleEventsByDate,
                    filterOptions = filterOptions
                )
            }.fold(
                onSuccess = { result ->
                    _uiState.update {
                        val bookmarkIds = bookmarkRepository.currentBookmarkedEventIds()
                        val filterSourceEventsByDate = result.filterSourceEventsByDate
                            .withBookmarkState(bookmarkIds)
                        val visibleEventsByDate = result.visibleEventsByDate
                            .withBookmarkState(bookmarkIds)
                        it.copy(
                            filterSourceEventsByDate = filterSourceEventsByDate,
                            eventsByDate = visibleEventsByDate.applyFilters(
                                filters = filters
                            ),
                            availableFilterOptions = result.filterOptions,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            filterSourceEventsByDate = emptyMap(),
                            eventsByDate = emptyMap(),
                            availableFilterOptions = CalendarFilterOptions(),
                            isLoading = false,
                            errorMessage = mapErrorMessage(error)
                        )
                    }
                }
            )
        }
    }

    // 새 카테고리 목록 API의 ID만 행사 조회 필터로 사용한다.
    private fun buildFilterOptions(): CalendarFilterOptions {
        return CalendarFilterOptions(
            orgIds = categoryRepository.organizations.value.map { item -> item.key.id },
            statusIds = categoryRepository.eventStatuses.value.map { item -> item.key.id },
            eventTypeIds = categoryRepository.eventTypes.value.map { item -> item.key.id }
        )
    }

    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is HttpException -> when (error.code()) {
                400 -> "행사 요청이 올바르지 않습니다."
                401 -> "로그인이 필요합니다."
                403 -> "행사 목록을 볼 권한이 없습니다."
                404 -> "행사 정보를 찾을 수 없습니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "행사 목록을 불러오지 못했습니다. (${error.code()})"
            }
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            is IllegalStateException -> "행사 목록을 불러오지 못했습니다."
            else -> "행사 목록을 불러오지 못했습니다."
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

    private fun mapExcludedKeywordErrorMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is HttpException -> when (error.code()) {
                400 -> "제외 키워드 요청이 올바르지 않습니다."
                401 -> "로그인이 필요합니다."
                403 -> "제외 키워드를 변경할 권한이 없습니다."
                404 -> "제외 키워드 정보를 찾을 수 없습니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "제외 키워드를 변경하지 못했습니다. (${error.code()})"
            }
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            is IllegalStateException -> "제외 키워드를 변경하지 못했습니다."
            else -> "제외 키워드를 변경하지 못했습니다."
        }
    }

    private fun onExcludedKeywordsChanged(keywords: List<String>) {
        val previousState = _uiState.value
        if (
            previousState.appliedFilters.excludedKeywords == keywords &&
            previousState.draftFilters.excludedKeywords == keywords
        ) {
            return
        }

        val updatedAppliedFilters = previousState.appliedFilters.copy(excludedKeywords = keywords)
        val updatedDraftFilters = previousState.draftFilters.copy(excludedKeywords = keywords)
        _uiState.update {
            it.copy(
                appliedFilters = updatedAppliedFilters,
                draftFilters = updatedDraftFilters,
                hasAppliedServerFilters = updatedAppliedFilters.hasActiveFilters,
                errorMessage = null
            )
        }
        loadPeriod(
            anchorDate = previousState.anchorDate,
            period = previousState.period,
            filters = updatedAppliedFilters,
            hasAppliedServerFilters = updatedAppliedFilters.hasActiveFilters,
            preserveFilterSheetState = true
        )
        requestFilterCount()
    }
    private fun onBookmarkedEventIdsChanged(eventIds: Set<Long>) {
        _uiState.update {
            val filterSourceEventsByDate = it.filterSourceEventsByDate.withBookmarkState(eventIds)
            val eventsByDate = it.eventsByDate.withBookmarkState(eventIds)
                .applyFilters(
                    filters = it.appliedFilters
                )

            it.copy(
                filterSourceEventsByDate = filterSourceEventsByDate,
                eventsByDate = eventsByDate
            )
        }
    }

    private fun normalizeFiltersForLoadedCatalog(loadedTypes: Set<CategoryType>) {
        val state = _uiState.value
        val applied = state.appliedFilters.normalizedAgainstCatalog(loadedTypes)
        val draft = state.draftFilters.normalizedAgainstCatalog(loadedTypes)
        if (applied == state.appliedFilters && draft == state.draftFilters) return

        _uiState.update {
            it.copy(
                appliedFilters = applied,
                draftFilters = draft,
                hasAppliedServerFilters = applied.hasActiveFilters
            )
        }
        loadPeriod(
            anchorDate = state.anchorDate,
            period = state.period,
            filters = applied,
            hasAppliedServerFilters = applied.hasActiveFilters,
            preserveFilterSheetState = true
        )
    }

    private fun CalendarFilterState.normalizedAgainstCatalog(
        loadedTypes: Set<CategoryType>
    ): CalendarFilterState {
        return copy(
            orgIds = if (CategoryType.ORGANIZATION in loadedTypes) {
                orgIds intersect categoryRepository.organizations.value.map { it.key.id }.toSet()
            } else {
                orgIds
            },
            statusIds = if (CategoryType.EVENT_STATUS in loadedTypes) {
                statusIds intersect categoryRepository.eventStatuses.value.map { it.key.id }.toSet()
            } else {
                statusIds
            },
            eventTypeIds = if (CategoryType.EVENT_TYPE in loadedTypes) {
                eventTypeIds intersect categoryRepository.eventTypes.value.map { it.key.id }.toSet()
            } else {
                eventTypeIds
            }
        )
    }
}

private data class CalendarPeriodLoadResult(
    val filterSourceEventsByDate: Map<LocalDate, List<CalendarEvent>>,
    val visibleEventsByDate: Map<LocalDate, List<CalendarEvent>>,
    val filterOptions: CalendarFilterOptions
)

private fun MonthlyEventsResponse.toCalendarEventsByDate(): Map<LocalDate, List<CalendarEvent>> {
    return byDate.entries
        .mapNotNull { (dateString, response) ->
            val date = runCatching { LocalDate.parse(dateString) }.getOrNull()
                ?: return@mapNotNull null

            date to response.events.map { event ->
                event.toCalendarEvent(date)
            }
        }
        .sortedBy { it.first }
        .toMap(linkedMapOf())
}

private fun MonthlyEventsResponse.toBookmarkMap(): Map<Long, Boolean> {
    return byDate.values
        .flatMap { it.events }
        .mapNotNull { event ->
            event.isBookmarked?.let { isBookmarked -> event.id to isBookmarked }
        }
        .toMap()
}

private fun EventSummaryResponse.toCalendarEvent(date: LocalDate): CalendarEvent {
    return CalendarEvent(
        id = id,
        date = date,
        title = title,
        imageUrl = imageUrl,
        operationMode = operationMode,
        statusId = statusId,
        eventTypeId = eventTypeId,
        orgId = orgId,
        applyStart = applyStart,
        applyEnd = applyEnd,
        eventStart = eventStart,
        eventEnd = eventEnd,
        isPeriodEvent = isPeriodEvent,
        capacity = capacity,
        applyCount = applyCount,
        organization = organization,
        location = location,
        applyLink = applyLink,
        tags = tags,
        isInterested = isInterested == true,
        matchedInterestPriority = matchedInterestPriority,
        isBookmarked = isBookmarked == true
        )
}

private fun Map<LocalDate, List<CalendarEvent>>.withBookmarkState(
    bookmarkedEventIds: Set<Long>
): Map<LocalDate, List<CalendarEvent>> {
    return mapValues { (_, events) ->
        events.map { event ->
            event.copy(isBookmarked = event.id in bookmarkedEventIds)
        }
    }
}

private fun Map<LocalDate, List<CalendarEvent>>.withBookmarkState(
    eventId: Long,
    isBookmarked: Boolean
): Map<LocalDate, List<CalendarEvent>> {
    return mapValues { (_, events) ->
        events.map { event ->
            if (event.id == eventId) {
                event.copy(isBookmarked = isBookmarked)
            } else {
                event
            }
        }
    }
}

private fun CalendarUiState.withUpdatedBookmark(
    eventId: Long,
    isBookmarked: Boolean
): CalendarUiState {
    return copy(
        filterSourceEventsByDate = filterSourceEventsByDate.withBookmarkState(
            eventId = eventId,
            isBookmarked = isBookmarked
        ),
        eventsByDate = eventsByDate.withBookmarkState(
            eventId = eventId,
            isBookmarked = isBookmarked
        ).applyFilters(appliedFilters)
    )
}

private fun Response<EventCountResponse>.requireCount(): Int {
    if (!isSuccessful) {
        throw HttpException(this)
    }

    return body()?.count?.coerceAtLeast(0)
        ?: throw IllegalStateException("Event count response was empty.")
}
private fun Response<MonthlyEventsResponse>.requireBody(
    emptyMessage: String
): MonthlyEventsResponse {
    if (!isSuccessful) {
        throw HttpException(this)
    }

    return body() ?: throw IllegalStateException(emptyMessage)
}

private fun <T> Set<T>.toggle(value: T): Set<T> {
    return if (value in this) {
        this - value
    } else {
        this + value
    }
}

private const val FILTER_COUNT_DEBOUNCE_MS = 300L
private const val FILTER_COUNT_TIMEOUT_MS = 3_000L
