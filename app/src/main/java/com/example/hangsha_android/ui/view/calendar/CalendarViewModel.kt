package com.example.hangsha_android.ui.view.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.model.EventCountResponse
import com.example.hangsha_android.data.repository.BookmarkRepository
import com.example.hangsha_android.data.repository.CategoryRepository
import com.example.hangsha_android.data.repository.EventRepository
import com.example.hangsha_android.data.repository.ExcludedKeywordsRepository
import com.example.hangsha_android.data.repository.model.CategoryType
import com.example.hangsha_android.data.repository.model.EventFilters
import com.example.hangsha_android.ui.view.calendar.data.CalendarPageLoader
import com.example.hangsha_android.ui.view.calendar.data.calendarBookmarkErrorMessage
import com.example.hangsha_android.ui.view.calendar.data.calendarExcludedKeywordErrorMessage
import com.example.hangsha_android.ui.view.calendar.data.calendarLoadErrorMessage
import com.example.hangsha_android.ui.view.calendar.filter.CalendarFilterOptions
import com.example.hangsha_android.ui.view.calendar.filter.CalendarFilterTab
import com.example.hangsha_android.ui.view.calendar.filter.applyFilters
import com.example.hangsha_android.ui.view.calendar.filter.resetSelections
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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

    private val pageLoader = CalendarPageLoader(eventRepository, bookmarkRepository)
    private val pageLoadJobs = mutableMapOf<CalendarPageKey, Job>()
    private var cacheFilters = _uiState.value.appliedFilters
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

    fun showPeriod(anchorDate: LocalDate) {
        val state = _uiState.value
        if (CalendarPageKey.from(state.period, state.anchorDate) ==
            CalendarPageKey.from(state.period, anchorDate)
        ) return
        loadPeriod(anchorDate = anchorDate, period = state.period)
    }

    fun showVisibleWeekDay(date: LocalDate) {
        _uiState.update { state ->
            if (state.period != CalendarPeriod.WEEK ||
                CalendarPageKey.from(CalendarPeriod.WEEK, state.anchorDate) !=
                CalendarPageKey.from(CalendarPeriod.WEEK, date)
            ) state else state.copy(anchorDate = date)
        }
    }

    fun setPeriod(period: CalendarPeriod) {
        val state = _uiState.value
        if (state.period == period) return
        loadPeriod(
            anchorDate = state.anchorDate,
            period = period
        )
    }

    fun showDayCalendar(date: LocalDate) {
        loadPeriod(
            anchorDate = date,
            period = CalendarPeriod.DAY,
            viewMode = CalendarViewMode.CALENDAR
        )
    }

    fun showDayList(date: LocalDate) {
        loadPeriod(
            anchorDate = date,
            period = CalendarPeriod.DAY,
            viewMode = CalendarViewMode.LIST
        )
    }

    fun setViewMode(viewMode: CalendarViewMode) {
        _uiState.update { state ->
            if (state.viewMode == viewMode) state else state.copy(viewMode = viewMode)
        }
    }

    fun toggleBookmark(eventId: Long) {
        val currentState = _uiState.value
        val targetEvent = currentState.pageStates.values
            .asSequence()
            .flatMap { page -> page.filterSourceEventsByDate.values.asSequence().flatten() }
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
                    ).copy(errorMessage = calendarBookmarkErrorMessage(error))
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
            hasAppliedServerFilters = state.hasAppliedServerFilters,
            forceRefresh = true
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
                _uiState.update { it.copy(errorMessage = calendarExcludedKeywordErrorMessage(error)) }
            }
        }
    }

    fun removeDraftExcludeKeyword(keyword: String) {
        viewModelScope.launch {
            runCatching {
                excludedKeywordsRepository.removeExcludedKeyword(keyword)
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = calendarExcludedKeywordErrorMessage(error)) }
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
            hasAppliedServerFilters = true,
            forceRefresh = true
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
    // Each period keeps its own page data so adjacent pages can render during a swipe.
    private fun loadPeriod(
        anchorDate: LocalDate,
        period: CalendarPeriod,
        filters: EventFilters = _uiState.value.appliedFilters,
        hasAppliedServerFilters: Boolean = _uiState.value.hasAppliedServerFilters,
        preserveFilterSheetState: Boolean = false,
        forceRefresh: Boolean = false,
        viewMode: CalendarViewMode? = null
    ) {
        val key = CalendarPageKey.from(period, anchorDate)
        val retainedKeys = (-2..2).map { offset ->
            CalendarPageKey.from(period, period.move(anchorDate, offset.toLong()))
        }.toSet()
        val filtersChanged = cacheFilters != filters

        if (filtersChanged) {
            pageLoadJobs.values.toList().forEach(Job::cancel)
            pageLoadJobs.clear()
            cacheFilters = filters
        } else {
            pageLoadJobs.keys.filter { it !in retainedKeys }.forEach { staleKey ->
                pageLoadJobs.remove(staleKey)?.cancel()
            }
        }
        if (!preserveFilterSheetState) filterCountJob?.cancel()

        _uiState.update { state ->
            val pages = if (filtersChanged) emptyMap() else {
                state.pageStates.filterKeys { it in retainedKeys }
            }
            val cachedPage = pages[key]
            state.copy(
                anchorDate = anchorDate,
                period = period,
                viewMode = viewMode ?: state.viewMode,
                pageStates = pages,
                appliedFilters = filters,
                hasAppliedServerFilters = hasAppliedServerFilters,
                errorMessage = cachedPage?.errorMessage,
                isFilterSheetVisible = if (preserveFilterSheetState) state.isFilterSheetVisible else false,
                draftFilters = if (preserveFilterSheetState) state.draftFilters else filters,
                selectedFilterTab = if (preserveFilterSheetState) state.selectedFilterTab else CalendarFilterTab.EVENT_TYPE,
                excludeKeywordInput = if (preserveFilterSheetState) state.excludeKeywordInput else ""
            )
        }

        ensurePage(key, filters, forceRefresh)
        ensurePage(CalendarPageKey.from(period, period.move(anchorDate, -1)), filters)
        ensurePage(CalendarPageKey.from(period, period.move(anchorDate, 1)), filters)
    }

    private fun ensurePage(
        key: CalendarPageKey,
        filters: EventFilters,
        forceRefresh: Boolean = false
    ) {
        val existing = _uiState.value.pageStates[key]
        if (!forceRefresh && existing != null &&
            (!existing.isLoading || pageLoadJobs.containsKey(key))
        ) return

        pageLoadJobs.remove(key)?.cancel()
        _uiState.update { state ->
            state.copy(pageStates = state.pageStates + (key to CalendarPeriodPage()))
        }

        val job = viewModelScope.launch {
            try {
                val page = pageLoader.load(key, filters)
                currentCoroutineContext().ensureActive()
                updatePage(key, filters, page)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                updatePage(
                    key,
                    filters,
                    CalendarPeriodPage(
                        isLoading = false,
                        errorMessage = calendarLoadErrorMessage(error)
                    )
                )
            }
        }
        pageLoadJobs[key] = job
        job.invokeOnCompletion {
            if (pageLoadJobs[key] === job) pageLoadJobs.remove(key)
        }
    }

    private fun updatePage(
        key: CalendarPageKey,
        filters: EventFilters,
        page: CalendarPeriodPage
    ) {
        if (cacheFilters != filters) return
        _uiState.update { state ->
            if (key !in state.pageStates) return@update state
            val updated = state.copy(pageStates = state.pageStates + (key to page))
            if (CalendarPageKey.from(state.period, state.anchorDate) != key) updated else {
                updated.copy(
                    availableFilterOptions = buildFilterOptions(),
                    errorMessage = page.errorMessage
                )
            }
        }
    }
    private fun buildFilterOptions(): CalendarFilterOptions {
        return CalendarFilterOptions(
            orgIds = categoryRepository.organizations.value.map { item -> item.key.id },
            statusIds = categoryRepository.eventStatuses.value.map { item -> item.key.id },
            eventTypeIds = categoryRepository.eventTypes.value.map { item -> item.key.id }
        )
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
            it.copy(
                pageStates = it.pageStates.mapValues { (_, page) ->
                    page.copy(
                        filterSourceEventsByDate = page.filterSourceEventsByDate
                            .withBookmarkState(eventIds),
                        eventsByDate = page.eventsByDate.withBookmarkState(eventIds)
                            .applyFilters(it.appliedFilters)
                    )
                }
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

    private fun EventFilters.normalizedAgainstCatalog(
        loadedTypes: Set<CategoryType>
    ): EventFilters {
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

private fun Response<EventCountResponse>.requireCount(): Int {
    if (!isSuccessful) {
        throw HttpException(this)
    }

    return body()?.count?.coerceAtLeast(0)
        ?: throw IllegalStateException("Event count response was empty.")
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
