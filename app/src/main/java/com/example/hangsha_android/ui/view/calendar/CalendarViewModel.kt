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
import com.example.hangsha_android.data.repository.UserRepository
import com.example.hangsha_android.data.repository.model.EventDateRange
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.YearMonth
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
    private val userRepository: UserRepository,
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
            userRepository.organizationNames.collect { organizationNames ->
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
            runCatching { userRepository.ensureOrganizationNamesLoaded() }
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
        loadMonth(_uiState.value.currentMonth)
    }

    fun showPreviousMonth() {
        loadMonth(_uiState.value.currentMonth.minusMonths(1))
    }

    fun showNextMonth() {
        loadMonth(_uiState.value.currentMonth.plusMonths(1))
    }

    fun retry() {
        loadMonth(
            month = _uiState.value.currentMonth,
            filters = _uiState.value.appliedFilters,
            hasAppliedServerFilters = _uiState.value.hasAppliedServerFilters
        )
    }

    fun restoreAppliedFilters(
        filters: CalendarFilterState,
        hasAppliedServerFilters: Boolean
    ) {
        val normalizedFilters = filters.copy(
            excludedKeywords = excludedKeywordsRepository.currentExcludedKeywords()
        )
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
        loadMonth(
            month = currentState.currentMonth,
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
        loadMonth(
            month = state.currentMonth,
            filters = appliedFilters,
            hasAppliedServerFilters = true
        )
    }
    private fun requestFilterCount() {
        val state = _uiState.value
        if (!state.isFilterSheetVisible) return

        val month = state.currentMonth
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
                        range = EventDateRange(
                            from = month.atDay(1),
                            to = month.atEndOfMonth()
                        ),
                        filters = filters
                    ).requireCount()
                }
                _uiState.update { current ->
                    if (
                        current.isFilterSheetVisible &&
                        current.currentMonth == month &&
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
                        current.currentMonth == month &&
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
    private fun loadMonth(
        month: YearMonth,
        filters: CalendarFilterState = _uiState.value.appliedFilters,
        hasAppliedServerFilters: Boolean = _uiState.value.hasAppliedServerFilters,
        preserveFilterSheetState: Boolean = false
    ) {
        val visibleRange = month.toCalendarGridRange()
        val visibleDates = visibleRange.toDateList()

        loadJob?.cancel()
        _uiState.update {
            it.copy(
                currentMonth = month,
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
                val sourceResponse = eventRepository.getAllEvents(visibleRange)
                val sourceBody = sourceResponse.requireBody("Events response was empty.")
                bookmarkRepository.syncKnownRemoteBookmarks(sourceBody.toBookmarkMap(), sourceUserId)
                val sourceEventsByDate = sourceBody.toCalendarEventsByDate()
                val filterOptions = buildFilterOptions(sourceEventsByDate)
                val visibleEventsByDate = if (hasAppliedServerFilters) {
                    val filteredResponse = eventRepository.getEvents(
                        range = visibleRange,
                        filters = filters
                    )
                    val filteredBody = filteredResponse
                        .requireBody("Filtered events response was empty.")
                    bookmarkRepository.syncKnownRemoteBookmarks(filteredBody.toBookmarkMap(), sourceUserId)
                    filteredBody.toCalendarEventsByDate()
                } else {
                    val prioritizedResponse = eventRepository.getEvents(
                        range = visibleRange,
                        filters = CalendarFilterState()
                    )
                    val prioritizedBody = prioritizedResponse
                        .requireBody("Prioritized events response was empty.")
                    bookmarkRepository.syncKnownRemoteBookmarks(prioritizedBody.toBookmarkMap(), sourceUserId)
                    val prioritizedEventsByDate = prioritizedBody.toCalendarEventsByDate()
                    sourceEventsByDate.reorderedBy(prioritizedEventsByDate)
                }

                CalendarMonthLoadResult(
                    filterSourceEventsByDate = sourceEventsByDate,
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

    // 전체 source 데이터에서 현재 월에 노출 가능한 필터 항목을 추출 (행사 개수 세기 용도)
    private fun buildFilterOptions(
        eventsByDate: Map<LocalDate, List<CalendarEvent>>
    ): CalendarFilterOptions {
        val events = eventsByDate.values.flatten()
        return CalendarFilterOptions(
            orgIds = userRepository.organizationNames.value.keys
                .takeIf { it.isNotEmpty() }
                ?.toList()
                ?: events.mapNotNull { it.orgId }
                    .distinct()
                    .sorted(),
            statusIds = events.map { it.statusId }
                .distinct()
                .sorted(),
            eventTypeIds = categoryRepository.eventTypeNames.value.keys
                .takeIf { it.isNotEmpty() }
                ?.toList()
                ?: events.map { it.eventTypeId }
                    .distinct()
                    .sorted()
        )
    }

    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "\uC778\uD130\uB137 \uC5F0\uACB0\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            is SocketTimeoutException -> "\uC694\uCCAD \uC2DC\uAC04\uC774 \uCD08\uACFC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is HttpException -> when (error.code()) {
                400 -> "\uD589\uC0AC \uC694\uCCAD\uC774 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."
                401 -> "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."
                403 -> "\uD589\uC0AC \uBAA9\uB85D\uC744 \uBCFC \uAD8C\uD55C\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."
                404 -> "\uD589\uC0AC \uC815\uBCF4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."
                in 500..599 -> "\uC11C\uBC84 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
                else -> "\uD589\uC0AC \uBAA9\uB85D\uC744 \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4. (${error.code()})"
            }
            is IOException -> "\uB124\uD2B8\uC6CC\uD06C \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is IllegalStateException -> "\uD589\uC0AC \uBAA9\uB85D\uC744 \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
            else -> "\uD589\uC0AC \uBAA9\uB85D\uC744 \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
        }
    }

    private fun mapExcludedKeywordErrorMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "\uC778\uD130\uB137 \uC5F0\uACB0\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            is SocketTimeoutException -> "\uC694\uCCAD \uC2DC\uAC04\uC774 \uCD08\uACFC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is HttpException -> when (error.code()) {
                400 -> "\uC81C\uC678 \uD0A4\uC6CC\uB4DC \uC694\uCCAD\uC774 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."
                401 -> "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."
                403 -> "\uC81C\uC678 \uD0A4\uC6CC\uB4DC\uB97C \uBCC0\uACBD\uD560 \uAD8C\uD55C\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."
                404 -> "\uC81C\uC678 \uD0A4\uC6CC\uB4DC \uC815\uBCF4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."
                in 500..599 -> "\uC11C\uBC84 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
                else -> "\uC81C\uC678 \uD0A4\uC6CC\uB4DC\uB97C \uBCC0\uACBD\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4. (${error.code()})"
            }
            is IOException -> "\uB124\uD2B8\uC6CC\uD06C \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is IllegalStateException -> "\uC81C\uC678 \uD0A4\uC6CC\uB4DC\uB97C \uBCC0\uACBD\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
            else -> "\uC81C\uC678 \uD0A4\uC6CC\uB4DC\uB97C \uBCC0\uACBD\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
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
        loadMonth(
            month = previousState.currentMonth,
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
}

private data class CalendarMonthLoadResult(
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
        .associate { event -> event.id to event.isBookmarked }
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
        isInterested = isInterested,
        matchedInterestPriority = matchedInterestPriority,
        isBookmarked = isBookmarked
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

private fun Map<LocalDate, List<CalendarEvent>>.reorderedBy(
    prioritizedEventsByDate: Map<LocalDate, List<CalendarEvent>>
): Map<LocalDate, List<CalendarEvent>> {
    return entries.associate { (date, events) ->
        val prioritizedIds = prioritizedEventsByDate[date].orEmpty()
            .mapIndexed { index, event -> event.id to index }
            .toMap()
        date to events.withStablePriority(prioritizedIds)
    }
}

private fun List<CalendarEvent>.withStablePriority(
    prioritizedIds: Map<Long, Int>
): List<CalendarEvent> {
    return withIndex()
        .sortedWith(
            compareBy<IndexedValue<CalendarEvent>>(
                { prioritizedIds[it.value.id] ?: Int.MAX_VALUE },
                { it.index }
            )
        )
        .map { it.value }
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
