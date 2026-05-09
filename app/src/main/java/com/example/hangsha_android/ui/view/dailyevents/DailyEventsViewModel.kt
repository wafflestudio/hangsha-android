package com.example.hangsha_android.ui.view.dailyevents

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.model.EventSummaryResponse
import com.example.hangsha_android.data.repository.EventRepository
import com.example.hangsha_android.ui.navigation.HangshaDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
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
class DailyEventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DailyEventsUiState(
            selectedDate = savedStateHandle.get<String>(HangshaDestinations.DailyEvents.dateArg)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: LocalDate.now()
        )
    )
    val uiState: StateFlow<DailyEventsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadDate(_uiState.value.selectedDate)
    }

    fun showPreviousDay() {
        loadDate(_uiState.value.selectedDate.minusDays(1))
    }

    fun showNextDay() {
        loadDate(_uiState.value.selectedDate.plusDays(1))
    }

    fun retry() {
        loadDate(_uiState.value.selectedDate)
    }

    fun openFilterSheet() {
        _uiState.update {
            it.copy(
                isFilterSheetVisible = true,
                draftFilters = it.appliedFilters,
                selectedFilterTab = DailyEventsFilterTab.EVENT_TYPE,
                excludeKeywordInput = ""
            )
        }
    }

    fun dismissFilterSheet() {
        _uiState.update {
            it.copy(
                isFilterSheetVisible = false,
                draftFilters = it.appliedFilters,
                selectedFilterTab = DailyEventsFilterTab.EVENT_TYPE,
                excludeKeywordInput = ""
            )
        }
    }

    fun clearDraftFilters() {
        _uiState.update {
            it.copy(
                draftFilters = DailyEventsFilterState(),
                excludeKeywordInput = ""
            )
        }
    }

    fun selectFilterTab(tab: DailyEventsFilterTab) {
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
                selectedFilterTab = DailyEventsFilterTab.EVENT_TYPE,
                excludeKeywordInput = "",
                isFilterSheetVisible = false,
                errorMessage = null
            )
        }
        loadDate(
            date = state.selectedDate,
            filters = appliedFilters
        )
    }

    private fun loadDate(
        date: LocalDate,
        filters: DailyEventsFilterState = _uiState.value.appliedFilters
    ) {
        loadJob?.cancel()
        _uiState.update {
            it.copy(
                selectedDate = date,
                appliedFilters = filters,
                isLoading = true,
                errorMessage = null,
                isFilterSheetVisible = false,
                draftFilters = filters,
                selectedFilterTab = DailyEventsFilterTab.EVENT_TYPE,
                excludeKeywordInput = ""
            )
        }

        loadJob = viewModelScope.launch {
            runCatching {
                val response = eventRepository.getDayEvents(date, filters)
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }

                val body = response.body()
                    ?: throw IllegalStateException("Daily events response was empty.")

                body.items.orEmpty()
            }.fold(
                onSuccess = { items ->
                    val allItems = items.toDailyEventItems()
                    val filterOptions = buildFilterOptions(allItems)
                    val filteredItems = allItems.applyFilters(filters)
                    _uiState.update {
                        it.copy(
                            allItems = allItems,
                            items = filteredItems,
                            availableFilterOptions = filterOptions,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            allItems = emptyList(),
                            items = emptyList(),
                            availableFilterOptions = DailyEventsFilterOptions(),
                            isLoading = false,
                            errorMessage = mapErrorMessage(error)
                        )
                    }
                }
            )
        }
    }

    private fun buildFilterOptions(items: List<DailyEventItem>): DailyEventsFilterOptions {
        return DailyEventsFilterOptions(
            orgIds = items.map { it.orgId }
                .distinct()
                .sorted(),
            statusIds = items.map { it.statusId }
                .distinct()
                .sorted(),
            eventTypeIds = items.map { it.eventTypeId }
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

private val ItemDateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.KOREA)

private fun List<EventSummaryResponse>.toDailyEventItems(): List<DailyEventItem> {
    return mapIndexed { index, event ->
        event.toDailyEventItem(index)
    }
}

private fun EventSummaryResponse.toDailyEventItem(index: Int): DailyEventItem {
    val baseDate = parseEventDate(applyEnd)
        ?: parseEventDate(eventStart)
        ?: parseEventDate(eventEnd)
        ?: parseEventDate(applyStart)
    val dDayLabel = baseDate?.let { targetDate ->
        val diff = targetDate.toEpochDay() - LocalDate.now().toEpochDay()
        when {
            diff == 0L -> "D-Day"
            diff > 0L -> "D-$diff"
            else -> "D+${-diff}"
        }
    } ?: "Recruiting"
    val displayDate = baseDate?.format(ItemDateFormatter) ?: "-"

    return DailyEventItem(
        id = id,
        title = title,
        organization = organization,
        displayDate = displayDate,
        dDayLabel = dDayLabel,
        accentColor = DailyEventAccentPalette[index % DailyEventAccentPalette.size],
        isBookmarked = isBookmarked,
        isInterested = isInterested,
        orgId = orgId,
        statusId = statusId,
        eventTypeId = eventTypeId,
        location = location,
        tags = tags
    )
}

private fun parseEventDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) {
        return null
    }

    return runCatching { OffsetDateTime.parse(value).toLocalDate() }.getOrElse {
        runCatching { LocalDate.parse(value) }.getOrNull()
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> {
    return if (value in this) {
        this - value
    } else {
        this + value
    }
}
