package com.example.hangsha_android.ui.view.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.model.EventSummaryResponse
import com.example.hangsha_android.data.network.model.MonthlyEventsResponse
import com.example.hangsha_android.data.repository.EventRepository
import com.example.hangsha_android.data.repository.UserRepository
import com.example.hangsha_android.data.repository.model.EventDateRange
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            userRepository.organizationNames.collect { organizationNames ->
                _uiState.update { it.copy(organizationNames = organizationNames) }
            }
        }
        viewModelScope.launch {
            runCatching { userRepository.ensureOrganizationNamesLoaded() }
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

    fun openFilterSheet() {
        _uiState.update {
            it.copy(
                isFilterSheetVisible = true,
                draftFilters = it.appliedFilters,
                selectedFilterTab = CalendarFilterTab.EVENT_TYPE,
                excludeKeywordInput = ""
            )
        }
    }

    fun dismissFilterSheet() {
        _uiState.update {
            it.copy(
                isFilterSheetVisible = false,
                draftFilters = it.appliedFilters,
                selectedFilterTab = CalendarFilterTab.EVENT_TYPE,
                excludeKeywordInput = ""
            )
        }
    }

    fun clearDraftFilters() {
        _uiState.update {
            it.copy(
                draftFilters = CalendarFilterState(),
                excludeKeywordInput = ""
            )
        }
    }

    fun selectFilterTab(tab: CalendarFilterTab) {
        _uiState.update { it.copy(selectedFilterTab = tab) }
    }

    fun setDraftBookmarkedOnly(enabled: Boolean) {
        _uiState.update {
            it.copy(
                draftFilters = it.draftFilters.copy(bookmarkedOnly = enabled)
            )
        }
    }

    fun setDraftInterestedOnly(enabled: Boolean) {
        _uiState.update {
            it.copy(
                draftFilters = it.draftFilters.copy(interestedOnly = enabled)
            )
        }
    }

    fun toggleDraftOrgId(orgId: Long) {
        _uiState.update {
            it.copy(
                draftFilters = it.draftFilters.copy(
                    orgIds = it.draftFilters.orgIds.toggle(orgId)
                )
            )
        }
    }

    fun toggleDraftStatus(statusId: Long) {
        _uiState.update {
            it.copy(
                draftFilters = it.draftFilters.copy(
                    statusIds = it.draftFilters.statusIds.toggle(statusId)
                )
            )
        }
    }

    fun toggleDraftEventType(eventTypeId: Long) {
        _uiState.update {
            it.copy(
                draftFilters = it.draftFilters.copy(
                    eventTypeIds = it.draftFilters.eventTypeIds.toggle(eventTypeId)
                )
            )
        }
    }

    fun updateExcludeKeywordInput(value: String) {
        _uiState.update { it.copy(excludeKeywordInput = value) }
    }

    fun addDraftExcludeKeyword() {
        val keyword = _uiState.value.excludeKeywordInput.trim()
        if (keyword.isBlank()) return

        _uiState.update { state ->
            if (keyword in state.draftFilters.excludedKeywords) {
                state.copy(excludeKeywordInput = "")
            } else {
                state.copy(
                    draftFilters = state.draftFilters.copy(
                        excludedKeywords = state.draftFilters.excludedKeywords + keyword
                    ),
                    excludeKeywordInput = ""
                )
            }
        }
    }

    fun removeDraftExcludeKeyword(keyword: String) {
        _uiState.update {
            it.copy(
                draftFilters = it.draftFilters.copy(
                    excludedKeywords = it.draftFilters.excludedKeywords - keyword
                )
            )
        }
    }

    fun applyDraftFilters() {
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
                errorMessage = null
            )
        }
        loadMonth(
            month = state.currentMonth,
            filters = appliedFilters,
            hasAppliedServerFilters = true
        )
    }

    // 현재 월의 전체 source 데이터를 먼저 가져오고,
    // 그다음 화면 표시용 데이터만 분기해서 구성한다.
    private fun loadMonth(
        month: YearMonth,
        filters: CalendarFilterState = _uiState.value.appliedFilters,
        hasAppliedServerFilters: Boolean = _uiState.value.hasAppliedServerFilters
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
                isFilterSheetVisible = false,
                draftFilters = filters,
                selectedFilterTab = CalendarFilterTab.EVENT_TYPE,
                excludeKeywordInput = ""
            )
        }

        loadJob = viewModelScope.launch {
            runCatching {
                val sourceResponse = eventRepository.getAllEvents(visibleRange)
                val sourceBody = sourceResponse.requireBody("Events response was empty.")
                val sourceEventsByDate = sourceBody.toCalendarEventsByDate()
                val filterOptions = buildFilterOptions(sourceEventsByDate)
                val visibleEventsByDate = if (hasAppliedServerFilters) {
                    val filteredResponse = eventRepository.getEvents(
                        range = visibleRange,
                        filters = filters
                    )
                    filteredResponse.requireBody("Filtered events response was empty.")
                        .toCalendarEventsByDate()
                } else {
                    val prioritizedResponse = eventRepository.getEvents(
                        range = visibleRange,
                        filters = CalendarFilterState()
                    )
                    val prioritizedEventsByDate = prioritizedResponse
                        .requireBody("Prioritized events response was empty.")
                        .toCalendarEventsByDate()
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
                        it.copy(
                            filterSourceEventsByDate = result.filterSourceEventsByDate,
                            eventsByDate = result.visibleEventsByDate.applyFilters(filters),
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
            orgIds = events.map { it.orgId }
                .distinct()
                .sorted(),
            statusIds = events.map { it.statusId }
                .distinct()
                .sorted(),
            eventTypeIds = events.map { it.eventTypeId }
                .distinct()
                .sorted()
        )
    }

    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "No internet connection. Please check your network."
            is SocketTimeoutException -> "The request timed out. Please try again."
            is HttpException -> when (error.code()) {
                400 -> "Invalid event request."
                401 -> "Login is required."
                403 -> "You do not have permission to view these events."
                404 -> "Event information could not be found."
                in 500..599 -> "Server error occurred. Please try again later."
                else -> "Failed to load events with code ${error.code()}."
            }
            is IOException -> "Network error occurred. Please try again."
            is IllegalStateException -> error.message ?: "Failed to load events."
            else -> error.message ?: "Failed to load events."
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
