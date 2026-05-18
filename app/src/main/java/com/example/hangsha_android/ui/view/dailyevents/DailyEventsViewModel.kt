package com.example.hangsha_android.ui.view.dailyevents

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.network.model.EventSummaryResponse
import com.example.hangsha_android.data.repository.EventRepository
import com.example.hangsha_android.data.repository.ExcludedKeywordsRepository
import com.example.hangsha_android.data.repository.UserRepository
import com.example.hangsha_android.ui.navigation.HangshaDestinations
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
import kotlinx.coroutines.flow.collect
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
    private val userRepository: UserRepository,
    private val excludedKeywordsRepository: ExcludedKeywordsRepository,
    authTokenStorage: AuthTokenStorage,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private var hasInitialized = false

    private val _uiState = MutableStateFlow(
        DailyEventsUiState(
            selectedDate = savedStateHandle.get<String>(HangshaDestinations.DailyEvents.dateArg)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: LocalDate.now(),
            isLoggedIn = !authTokenStorage.getAccessToken().isNullOrBlank()
        )
    )
    val uiState: StateFlow<DailyEventsUiState> = _uiState.asStateFlow()

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
        viewModelScope.launch {
            excludedKeywordsRepository.excludedKeywords.collect { keywords ->
                onExcludedKeywordsChanged(keywords)
            }
        }
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

    // 캘린더에서 넘어온 적용 필터 동기화
    fun initialize(
        filters: DailyEventsFilterState?,
        hasAppliedServerFilters: Boolean?
    ) {
        if (hasInitialized) {
            return
        }
        hasInitialized = true

        val initialFilters = (filters ?: _uiState.value.appliedFilters).copy(
            excludedKeywords = excludedKeywordsRepository.currentExcludedKeywords()
        )
        val initialHasAppliedServerFilters =
            initialFilters.hasActiveFilters

        _uiState.update {
            it.copy(
                appliedFilters = initialFilters,
                draftFilters = initialFilters,
                hasAppliedServerFilters = initialHasAppliedServerFilters
            )
        }

        loadDate(
            date = _uiState.value.selectedDate,
            filters = initialFilters,
            hasAppliedServerFilters = initialHasAppliedServerFilters
        )
    }

    fun openFilterSheet() {
        val currentKeywords = excludedKeywordsRepository.currentExcludedKeywords()
        _uiState.update {
            it.copy(
                isFilterSheetVisible = true,
                draftFilters = it.appliedFilters.copy(excludedKeywords = currentKeywords),
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
        val keywordsToDelete = _uiState.value.draftFilters.excludedKeywords
        _uiState.update {
            it.copy(
                draftFilters = DailyEventsFilterState(),
                excludeKeywordInput = ""
            )
        }
        if (keywordsToDelete.isNotEmpty()) {
            viewModelScope.launch {
                runCatching {
                    keywordsToDelete.forEach { keyword ->
                        excludedKeywordsRepository.removeExcludedKeyword(keyword)
                    }
                }.onFailure { error ->
                    _uiState.update { it.copy(errorMessage = mapExcludedKeywordErrorMessage(error)) }
                }
            }
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

    // 현재 날짜의 전체 source 데이터를 먼저 가져오고
    // 그다음 화면에 보여줄 리스트만 별도로 결정
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
                    .toDailyEventItems(date)
                val filterOptions = buildFilterOptions(sourceItems)
                val visibleItems = if (hasAppliedServerFilters) {
                    eventRepository.getDayEvents(date, filters)
                        .requireBody("Filtered daily events response was empty.")
                        .items
                        .orEmpty()
                        .toDailyEventItems(date)
                } else {
                    val prioritizedItems = eventRepository.getDayEvents(
                        date = date,
                        filters = DailyEventsFilterState()
                    ).requireBody("Prioritized daily events response was empty.")
                        .items
                        .orEmpty()
                        .toDailyEventItems(date)
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
                            items = result.visibleItems.applyFilters(
                                filters = filters,
                                applyExcludedKeywords = !_uiState.value.isLoggedIn
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

    // 전체 source 데이터에서 현재 날짜에 노출 가능한 필터 항목을 추출 (행사 개수 세기 용도)
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

    private fun mapExcludedKeywordErrorMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "No internet connection. Please check your network."
            is SocketTimeoutException -> "The request timed out. Please try again."
            is HttpException -> when (error.code()) {
                400 -> "Invalid excluded keyword request."
                401 -> "Login is required."
                403 -> "You do not have permission to update excluded keywords."
                404 -> "Excluded keyword information could not be found."
                in 500..599 -> "Server error occurred. Please try again later."
                else -> "Failed to update excluded keywords with code ${error.code()}."
            }
            is IOException -> "Network error occurred. Please try again."
            is IllegalStateException -> error.message ?: "Failed to update excluded keywords."
            else -> error.message ?: "Failed to update excluded keywords."
        }
    }

    private fun onExcludedKeywordsChanged(keywords: List<String>) {
        val previousState = _uiState.value
        val previousDraftKeywords = previousState.draftFilters.excludedKeywords

        if (previousDraftKeywords == keywords) {
            return
        }

        val updatedDraftFilters = previousState.draftFilters.copy(excludedKeywords = keywords)

        _uiState.update {
            it.copy(
                draftFilters = updatedDraftFilters,
                errorMessage = null
            )
        }
    }
}

private data class DailyEventsLoadResult(
    val filterSourceItems: List<DailyEventItem>,
    val visibleItems: List<DailyEventItem>,
    val filterOptions: DailyEventsFilterOptions
)

private val ItemDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.KOREA)
private val ItemDateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.KOREA)

private fun List<EventSummaryResponse>.toDailyEventItems(
    selectedDate: LocalDate
): List<DailyEventItem> {
    return map { event ->
        Log.d("DailyEventsViewModel", "event: $event, selectedDate: $selectedDate")
        event.toDailyEventItem(selectedDate)
    }
}

private fun EventSummaryResponse.toDailyEventItem(selectedDate: LocalDate): DailyEventItem {
    val eventEndDate = parseEventDate(eventEnd)
    val dDayLabel = eventEndDate?.let { targetDate ->
        val diff = targetDate.toEpochDay() - selectedDate.toEpochDay()
        when {
            diff == 0L -> "D-day"
            diff > 0L -> "D-$diff"
            else -> "D$diff"
        }
    } ?: "-"
    val eventEndDisplay = formatEventEnd(eventEnd)
        ?: eventEndDate?.format(ItemDateFormatter)
        ?: "-"

    return DailyEventItem(
        id = id,
        title = title,
        organization = organization,
        eventEndDisplay = eventEndDisplay,
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
        runCatching { LocalDateTime.parse(value).toLocalDate() }.getOrElse {
            runCatching { LocalDate.parse(value) }.getOrNull()
        }
    }
}

private fun formatEventEnd(value: String?): String? {
    if (value.isNullOrBlank()) {
        return null
    }

    return runCatching {
        OffsetDateTime.parse(value).toLocalDateTime().format(ItemDateTimeFormatter)
    }.getOrElse {
        runCatching { LocalDateTime.parse(value).format(ItemDateTimeFormatter) }.getOrElse {
            runCatching { LocalDate.parse(value).format(ItemDateFormatter) }.getOrNull()
        }
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
