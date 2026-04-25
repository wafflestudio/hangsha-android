package com.example.hangsha_android.ui.view.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.model.EventSummaryResponse
import com.example.hangsha_android.data.network.model.MonthlyEventsResponse
import com.example.hangsha_android.data.repository.EventRepository
import com.example.hangsha_android.data.repository.model.EventDateRange
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.YearMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var allEventsByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap()

    init {
        loadMonth(_uiState.value.currentMonth)
    }

    fun showPreviousMonth() {
        loadMonth(_uiState.value.currentMonth.minusMonths(1))
    }

    fun showNextMonth() {
        loadMonth(_uiState.value.currentMonth.plusMonths(1))
    }

    fun retry() {
        loadMonth(_uiState.value.currentMonth)
    }

    fun openFilterSheet() {
        _uiState.update {
            it.copy(
                isFilterSheetVisible = true,
                draftFilters = it.appliedFilters
            )
        }
    }

    fun dismissFilterSheet() {
        _uiState.update {
            it.copy(
                isFilterSheetVisible = false,
                draftFilters = it.appliedFilters
            )
        }
    }

    fun clearDraftFilters() {
        _uiState.update {
            it.copy(draftFilters = CalendarFilterState())
        }
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

    fun toggleDraftOperationMode(operationMode: String) {
        _uiState.update {
            it.copy(
                draftFilters = it.draftFilters.copy(
                    operationModes = it.draftFilters.operationModes.toggle(operationMode)
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

    fun applyDraftFilters() {
        _uiState.update { state ->
            val appliedFilters = state.draftFilters
            state.copy(
                appliedFilters = appliedFilters,
                eventsByDate = applyFilters(allEventsByDate, appliedFilters),
                isFilterSheetVisible = false,
                errorMessage = null
            )
        }
    }

    private fun loadMonth(month: YearMonth) {
        val visibleRange = month.toCalendarGridRange()
        val visibleDates = visibleRange.toDateList()
        val appliedFilters = _uiState.value.appliedFilters

        loadJob?.cancel()
        _uiState.update {
            it.copy(
                currentMonth = month,
                visibleRange = visibleRange,
                visibleDates = visibleDates,
                isLoading = true,
                errorMessage = null,
                isFilterSheetVisible = false,
                draftFilters = appliedFilters
            )
        }

        loadJob = viewModelScope.launch {
            runCatching {
                val response = eventRepository.getEvents(visibleRange)
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }

                response.body() ?: throw IllegalStateException("Events response was empty.")
            }.fold(
                onSuccess = { response ->
                    allEventsByDate = response.toCalendarEventsByDate()
                    val filterOptions = buildFilterOptions(allEventsByDate)
                    _uiState.update {
                        it.copy(
                            eventsByDate = applyFilters(allEventsByDate, appliedFilters),
                            availableFilterOptions = filterOptions,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    allEventsByDate = emptyMap()
                    _uiState.update {
                        it.copy(
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

    private fun buildFilterOptions(
        eventsByDate: Map<LocalDate, List<CalendarEvent>>
    ): CalendarFilterOptions {
        val events = eventsByDate.values.flatten()
        return CalendarFilterOptions(
            operationModes = events.map { it.operationMode }
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

    private fun applyFilters(
        eventsByDate: Map<LocalDate, List<CalendarEvent>>,
        filters: CalendarFilterState
    ): Map<LocalDate, List<CalendarEvent>> {
        if (!filters.hasActiveFilters) {
            return eventsByDate
        }

        return buildMap {
            eventsByDate.toSortedMap().forEach { (date, events) ->
                val filteredEvents = events.filter(filters::matches)
                if (filteredEvents.isNotEmpty()) {
                    put(date, filteredEvents)
                }
            }
        }
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

private fun <T> Set<T>.toggle(value: T): Set<T> {
    return if (value in this) {
        this - value
    } else {
        this + value
    }
}
