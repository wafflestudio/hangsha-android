package com.example.hangsha_android.ui.view.dailyevents

import com.example.hangsha_android.util.currentHangshaDate
import com.example.hangsha_android.util.toHangshaDate
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.model.EventCountResponse
import com.example.hangsha_android.data.network.model.EventSummaryResponse
import com.example.hangsha_android.data.repository.BookmarkRepository
import com.example.hangsha_android.data.repository.CategoryRepository
import com.example.hangsha_android.data.repository.EventRepository
import com.example.hangsha_android.data.repository.ExcludedKeywordsRepository
import com.example.hangsha_android.data.repository.model.CategoryType
import com.example.hangsha_android.ui.navigation.HangshaDestinations
import com.example.hangsha_android.ui.view.event.formatApplicationDeadlineLabel
import com.example.hangsha_android.ui.view.event.formatEventCountdownLabel
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
import com.example.hangsha_android.ui.view.event.eventTypeColor
import retrofit2.HttpException
import retrofit2.Response

@HiltViewModel
class DailyEventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val categoryRepository: CategoryRepository,
    private val excludedKeywordsRepository: ExcludedKeywordsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private var hasInitialized = false

    private val _uiState = MutableStateFlow(
        DailyEventsUiState(
            selectedDate = savedStateHandle.get<String>(HangshaDestinations.DailyEvents.dateArg)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: currentHangshaDate()
        )
    )
    val uiState: StateFlow<DailyEventsUiState> = _uiState.asStateFlow()

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

    fun toggleBookmark(eventId: Long) {
        val currentState = _uiState.value
        val targetItem = currentState.filterSourceItems.firstOrNull { it.id == eventId }
            ?: currentState.items.firstOrNull { it.id == eventId }
            ?: return
        val shouldBookmark = !targetItem.isBookmarked

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
        ).normalizedAgainstCatalog(categoryRepository.loadedCategoryTypes.value)
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
        requestFilterCount()
    }

    fun dismissFilterSheet() {
        filterCountJob?.cancel()
        _uiState.update {
            it.copy(
                isFilterSheetVisible = false,
                draftFilters = it.appliedFilters,
                selectedFilterTab = DailyEventsFilterTab.EVENT_TYPE,
                excludeKeywordInput = "",
                filteredItemCount = null,
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
    fun selectFilterTab(tab: DailyEventsFilterTab) {
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
                selectedFilterTab = DailyEventsFilterTab.EVENT_TYPE,
                excludeKeywordInput = "",
                isFilterSheetVisible = false,
                filteredItemCount = null,
                isFilterCountLoading = false,
                errorMessage = null
            )
        }
        loadDate(
            date = state.selectedDate,
            filters = appliedFilters,
            hasAppliedServerFilters = true
        )
    }
    private fun requestFilterCount() {
        val state = _uiState.value
        if (!state.isFilterSheetVisible) return

        val date = state.selectedDate
        val filters = state.draftFilters
        filterCountJob?.cancel()
        _uiState.update {
            it.copy(
                filteredItemCount = null,
                isFilterCountLoading = true
            )
        }

        filterCountJob = viewModelScope.launch {
            try {
                delay(FILTER_COUNT_DEBOUNCE_MS)
                val count = withTimeout(FILTER_COUNT_TIMEOUT_MS) {
                    eventRepository.getDayEventCount(
                        date = date,
                        filters = filters
                    ).requireCount()
                }
                _uiState.update { current ->
                    if (
                        current.isFilterSheetVisible &&
                        current.selectedDate == date &&
                        current.draftFilters == filters
                    ) {
                        current.copy(
                            filteredItemCount = count,
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
                        current.selectedDate == date &&
                        current.draftFilters == filters
                    ) {
                        current.copy(
                            filteredItemCount = null,
                            isFilterCountLoading = false
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }
    // 현재 날짜의 전체 source 데이터를 먼저 가져오고
    // 그다음 화면에 보여줄 리스트만 별도로 결정
    private fun loadDate(
        date: LocalDate,
        filters: DailyEventsFilterState = _uiState.value.appliedFilters,
        hasAppliedServerFilters: Boolean = _uiState.value.hasAppliedServerFilters,
        preserveFilterSheetState: Boolean = false
    ) {
        loadJob?.cancel()
        _uiState.update {
            it.copy(
                selectedDate = date,
                appliedFilters = filters,
                hasAppliedServerFilters = hasAppliedServerFilters,
                isLoading = true,
                errorMessage = null,
                isFilterSheetVisible = if (preserveFilterSheetState) it.isFilterSheetVisible else false,
                draftFilters = if (preserveFilterSheetState) it.draftFilters else filters,
                selectedFilterTab = if (preserveFilterSheetState) it.selectedFilterTab else DailyEventsFilterTab.EVENT_TYPE,
                excludeKeywordInput = if (preserveFilterSheetState) it.excludeKeywordInput else ""
            )
        }

        loadJob = viewModelScope.launch {
            val sourceUserId = bookmarkRepository.currentUserId()
            runCatching {
                val response = eventRepository.getDayEvents(date, filters)
                val visibleItems = response.items
                    .orEmpty()
                    .also { bookmarkRepository.syncKnownRemoteBookmarks(it.toBookmarkMap(), sourceUserId) }
                    .toDailyEventItems()
                val filterOptions = buildFilterOptions()

                DailyEventsLoadResult(
                    filterSourceItems = visibleItems,
                    visibleItems = visibleItems,
                    filterOptions = filterOptions
                )
            }.fold(
                onSuccess = { result ->
                    _uiState.update {
                        val bookmarkIds = bookmarkRepository.currentBookmarkedEventIds()
                        val filterSourceItems = result.filterSourceItems.withBookmarkState(bookmarkIds)
                        val visibleItems = result.visibleItems.withBookmarkState(bookmarkIds)
                        it.copy(
                            filterSourceItems = filterSourceItems,
                            items = visibleItems.applyFilters(
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

    // 새 카테고리 목록 API의 ID만 행사 조회 필터로 사용한다.
    private fun buildFilterOptions(): DailyEventsFilterOptions {
        return DailyEventsFilterOptions(
            orgIds = categoryRepository.organizations.value.map { item -> item.key.id },
            statusIds = categoryRepository.eventStatuses.value.map { item -> item.key.id },
            eventTypeIds = categoryRepository.eventTypes.value.map { item -> item.key.id }
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

    private fun mapBookmarkErrorMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "\uC778\uD130\uB137 \uC5F0\uACB0\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            is SocketTimeoutException -> "\uC694\uCCAD \uC2DC\uAC04\uC774 \uCD08\uACFC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is HttpException -> when (error.code()) {
                400 -> "\uBD81\uB9C8\uD06C \uC694\uCCAD\uC774 \uC62C\uBC14\uB974\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4."
                401 -> "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."
                403 -> "\uC774 \uBD81\uB9C8\uD06C\uB97C \uBCC0\uACBD\uD560 \uAD8C\uD55C\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."
                404 -> "\uD589\uC0AC \uC815\uBCF4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."
                in 500..599 -> "\uC11C\uBC84 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
                else -> "\uBD81\uB9C8\uD06C\uB97C \uBCC0\uACBD\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4. (${error.code()})"
            }
            is IOException -> "\uB124\uD2B8\uC6CC\uD06C \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            else -> "\uBD81\uB9C8\uD06C\uB97C \uBCC0\uACBD\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
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
        if (hasInitialized) {
            loadDate(
                date = previousState.selectedDate,
                filters = updatedAppliedFilters,
                hasAppliedServerFilters = updatedAppliedFilters.hasActiveFilters,
                preserveFilterSheetState = true
            )
        }
        requestFilterCount()
    }
    private fun onBookmarkedEventIdsChanged(eventIds: Set<Long>) {
        _uiState.update {
            val filterSourceItems = it.filterSourceItems.withBookmarkState(eventIds)
            val items = it.items.withBookmarkState(eventIds)
                .applyFilters(
                    filters = it.appliedFilters
                )

            it.copy(
                filterSourceItems = filterSourceItems,
                items = items
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
        if (hasInitialized) {
            loadDate(
                date = state.selectedDate,
                filters = applied,
                hasAppliedServerFilters = applied.hasActiveFilters,
                preserveFilterSheetState = true
            )
        }
    }

    private fun DailyEventsFilterState.normalizedAgainstCatalog(
        loadedTypes: Set<CategoryType>
    ): DailyEventsFilterState {
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

private data class DailyEventsLoadResult(
    val filterSourceItems: List<DailyEventItem>,
    val visibleItems: List<DailyEventItem>,
    val filterOptions: DailyEventsFilterOptions
)

private val ItemDateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.KOREA)
private val ItemFullDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA)
private val ItemMonthDayFormatter = DateTimeFormatter.ofPattern("MM.dd", Locale.KOREA)

private fun List<EventSummaryResponse>.toDailyEventItems(): List<DailyEventItem> {
    return map { event ->
        event.toDailyEventItem()
    }
}

private fun List<EventSummaryResponse>.toBookmarkMap(): Map<Long, Boolean> {
    return mapNotNull { event ->
        event.isBookmarked?.let { isBookmarked -> event.id to isBookmarked }
    }.toMap()
}

private fun EventSummaryResponse.toDailyEventItem(): DailyEventItem {
    val eventStartDate = parseEventDate(eventStart)
    val eventEndDate = parseEventDate(eventEnd)
    val applyEndDate = parseEventDate(applyEnd)
    val dDayLabel = formatApplicationDeadlineLabel(applyEndDate)
    val eventEndDisplay = formatEventEnd(eventEnd)
        ?: eventEndDate?.format(ItemDateFormatter)
        ?: "-"

    return DailyEventItem(
        id = id,
        title = title,
        imageUrl = imageUrl,
        organization = organization,
        eventEndDisplay = eventEndDisplay,
        applyPeriodDisplay = formatPeriod(applyStart, applyEnd),
        dDayLabel = dDayLabel,
        eventDDayLabel = formatEventCountdownLabel(eventStartDate, eventEndDate),
        accentColor = eventTypeColor(eventTypeId),
        isBookmarked = isBookmarked == true,
        isInterested = isInterested == true,
        orgId = orgId,
        statusId = statusId,
        eventTypeId = eventTypeId,
        location = location,
        tags = tags
    )
}

private fun List<DailyEventItem>.withBookmarkState(
    bookmarkedEventIds: Set<Long>
): List<DailyEventItem> {
    return map { item ->
        item.copy(isBookmarked = item.id in bookmarkedEventIds)
    }
}

private fun parseEventDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) {
        return null
    }

    return runCatching { OffsetDateTime.parse(value).toHangshaDate() }.getOrElse {
        runCatching { LocalDateTime.parse(value).toLocalDate() }.getOrElse {
            runCatching { LocalDate.parse(value) }.getOrNull()
        }
    }
}

private fun formatPeriod(
    startValue: String?,
    endValue: String?
): String {
    val start = parseEventDate(startValue)
    val end = parseEventDate(endValue)
    return when {
        start != null && end != null && start.year == end.year ->
            "${start.format(ItemFullDateFormatter)}~${end.format(ItemMonthDayFormatter)}"
        start != null && end != null ->
            "${start.format(ItemFullDateFormatter)}~${end.format(ItemFullDateFormatter)}"
        start != null -> start.format(ItemFullDateFormatter)
        end != null -> end.format(ItemFullDateFormatter)
        else -> "-"
    }
}
private fun formatEventEnd(value: String?): String? {
    if (value.isNullOrBlank()) {
        return null
    }

    return runCatching {
        OffsetDateTime.parse(value).toHangshaDate().format(ItemDateFormatter)
    }.getOrElse {
        runCatching { LocalDateTime.parse(value).toLocalDate().format(ItemDateFormatter) }.getOrElse {
            runCatching { LocalDate.parse(value).format(ItemDateFormatter) }.getOrNull()
        }
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

private fun List<DailyEventItem>.toggleBookmark(eventId: Long): List<DailyEventItem> {
    return map { item ->
        if (item.id == eventId) {
            item.copy(isBookmarked = !item.isBookmarked)
        } else {
            item
        }
    }
}

private fun List<DailyEventItem>.setBookmark(
    eventId: Long,
    isBookmarked: Boolean
): List<DailyEventItem> {
    return map { item ->
        if (item.id == eventId) {
            item.copy(isBookmarked = isBookmarked)
        } else {
            item
        }
    }
}

private fun DailyEventsUiState.withUpdatedBookmark(
    eventId: Long,
    isBookmarked: Boolean
): DailyEventsUiState {
    val updatedFilterSourceItems = filterSourceItems.setBookmark(
        eventId = eventId,
        isBookmarked = isBookmarked
    )
    val updatedItems = items.setBookmark(
        eventId = eventId,
        isBookmarked = isBookmarked
    ).applyFilters(
        filters = appliedFilters
    )

    return copy(
        filterSourceItems = updatedFilterSourceItems,
        items = updatedItems
    )
}

private const val FILTER_COUNT_DEBOUNCE_MS = 300L
private const val FILTER_COUNT_TIMEOUT_MS = 3_000L
