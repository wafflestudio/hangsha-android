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
import com.example.hangsha_android.ui.view.event.eventTypeColor
import retrofit2.HttpException
import retrofit2.Response

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
        loadDate(
            date = _uiState.value.selectedDate,
            filters = _uiState.value.appliedFilters,
            hasAppliedServerFilters = _uiState.value.hasAppliedServerFilters
        )
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
                hasAppliedServerFilters = true,
                selectedFilterTab = DailyEventsFilterTab.EVENT_TYPE,
                excludeKeywordInput = "",
                isFilterSheetVisible = false,
                errorMessage = null
            )
        }
        loadDate(
            date = state.selectedDate,
            filters = appliedFilters,
            hasAppliedServerFilters = true
        )
    }

    // 현재 날짜의 전체 source 데이터를 먼저 가져오고, 그다음 화면에 보여줄 리스트만 별도로 결정한다.
    private fun loadDate(
        date: LocalDate,
        filters: DailyEventsFilterState = _uiState.value.appliedFilters,
        hasAppliedServerFilters: Boolean = _uiState.value.hasAppliedServerFilters
    ) {
        loadJob?.cancel()
        _uiState.update {
            it.copy(
                selectedDate = date,
                appliedFilters = filters,
                hasAppliedServerFilters = hasAppliedServerFilters,
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
                val sourceResponse = eventRepository.getAllDayEvents(date)
                val sourceItems = sourceResponse
                    .requireBody("Daily events response was empty.")
                    .items
                    .orEmpty()
                    .toDailyEventItems()
                val filterOptions = buildFilterOptions(sourceItems)
                val visibleItems = if (hasAppliedServerFilters) {
                    eventRepository.getDayEvents(date, filters)
                        .requireBody("Filtered daily events response was empty.")
                        .items
                        .orEmpty()
                        .toDailyEventItems()
                } else {
                    val prioritizedItems = eventRepository.getDayEvents(
                        date = date,
                        filters = DailyEventsFilterState()
                    ).requireBody("Prioritized daily events response was empty.")
                        .items
                        .orEmpty()
                        .toDailyEventItems()
                    sourceItems.reorderedBy(prioritizedItems)
                }

                DailyEventsLoadResult(
                    filterSourceItems = sourceItems,
                    visibleItems = visibleItems,
                    filterOptions = filterOptions
                )
            }.fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            filterSourceItems = result.filterSourceItems,
                            items = result.visibleItems,
                            availableFilterOptions = result.filterOptions,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            filterSourceItems = emptyList(),
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

    // 전체 source 데이터에서 현재 날짜에 노출 가능한 필터 항목을 한 번만 추출한다.
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

private data class DailyEventsLoadResult(
    val filterSourceItems: List<DailyEventItem>,
    val visibleItems: List<DailyEventItem>,
    val filterOptions: DailyEventsFilterOptions
)

private val ItemDateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.KOREA)

private fun List<EventSummaryResponse>.toDailyEventItems(): List<DailyEventItem> {
    return map { event ->
        event.toDailyEventItem()
    }
}

private fun EventSummaryResponse.toDailyEventItem(): DailyEventItem {
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
        accentColor = eventTypeColor(eventTypeId),
        isBookmarked = isBookmarked,
        isInterested = isInterested,
        orgId = orgId,
        statusId = statusId,
        eventTypeId = eventTypeId,
        location = location,
        tags = tags
    )
}

// 서버가 내려준 기본 상태 우선순서를 전체 source 데이터에 덧씌워, 필터 전 리스트도 원하는 순서로 보여준다.
private fun List<DailyEventItem>.reorderedBy(
    prioritizedItems: List<DailyEventItem>
): List<DailyEventItem> {
    val prioritizedIds = prioritizedItems
        .mapIndexed { index, item -> item.id to index }
        .toMap()

    return withIndex()
        .sortedWith(
            compareBy<IndexedValue<DailyEventItem>>(
                { prioritizedIds[it.value.id] ?: Int.MAX_VALUE },
                { it.index }
            )
        )
        .map { it.value }
}

private fun parseEventDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) {
        return null
    }

    return runCatching { OffsetDateTime.parse(value).toLocalDate() }.getOrElse {
        runCatching { LocalDate.parse(value) }.getOrNull()
    }
}

private fun Response<com.example.hangsha_android.data.network.model.DayEventsResponse>.requireBody(
    emptyMessage: String
): com.example.hangsha_android.data.network.model.DayEventsResponse {
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
