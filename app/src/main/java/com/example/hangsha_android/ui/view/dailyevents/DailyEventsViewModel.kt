package com.example.hangsha_android.ui.view.dailyevents

import com.example.hangsha_android.data.local.StoredGuestBookmarkSnapshot

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.model.EventSummaryResponse
import com.example.hangsha_android.data.repository.BookmarkRepository
import com.example.hangsha_android.data.repository.CategoryRepository
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
    private val bookmarkRepository: BookmarkRepository,
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository,
    private val excludedKeywordsRepository: ExcludedKeywordsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private var hasInitialized = false

    private val _uiState = MutableStateFlow(
        DailyEventsUiState(
            selectedDate = savedStateHandle.get<String>(HangshaDestinations.DailyEvents.dateArg)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: LocalDate.now(),
            isLoggedIn = bookmarkRepository.isLoggedIn()
        )
    )
    val uiState: StateFlow<DailyEventsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

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
                    isBookmarked = shouldBookmark,
                    guestSnapshot = if (shouldBookmark) targetItem.toGuestBookmarkSnapshot() else null
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
        _uiState.update {
            it.copy(
                draftFilters = it.draftFilters.resetSelections(),
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
                val sourceResponse = eventRepository.getAllDayEvents(date)
                val sourceItems = sourceResponse
                    .requireBody("Daily events response was empty.")
                    .items
                    .orEmpty()
                    .also { bookmarkRepository.syncKnownRemoteBookmarks(it.toBookmarkMap(), sourceUserId) }
                    .toDailyEventItems()
                val filterOptions = buildFilterOptions(sourceItems)
                val visibleItems = if (hasAppliedServerFilters) {
                    val filteredResponses = eventRepository.getDayEvents(date, filters)
                        .requireBody("Filtered daily events response was empty.")
                        .items
                        .orEmpty()
                    bookmarkRepository.syncKnownRemoteBookmarks(filteredResponses.toBookmarkMap(), sourceUserId)
                    filteredResponses.toDailyEventItems()
                } else {
                    val prioritizedResponses = eventRepository.getDayEvents(
                        date = date,
                        filters = DailyEventsFilterState()
                    ).requireBody("Prioritized daily events response was empty.")
                        .items
                        .orEmpty()
                    bookmarkRepository.syncKnownRemoteBookmarks(prioritizedResponses.toBookmarkMap(), sourceUserId)
                    val prioritizedItems = prioritizedResponses.toDailyEventItems()
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
                        val bookmarkIds = bookmarkRepository.currentBookmarkedEventIds()
                        val filterSourceItems = result.filterSourceItems.withBookmarkState(bookmarkIds)
                        val visibleItems = result.visibleItems.withBookmarkState(bookmarkIds)
                        it.copy(
                            filterSourceItems = filterSourceItems,
                            items = visibleItems.applyFilters(
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
            orgIds = userRepository.organizationNames.value.keys
                .takeIf { it.isNotEmpty() }
                ?.toList()
                ?: items.mapNotNull { it.orgId }
                    .distinct()
                    .sorted(),
            statusIds = items.map { it.statusId }
                .distinct()
                .sorted(),
            eventTypeIds = categoryRepository.eventTypeNames.value.keys
                .takeIf { it.isNotEmpty() }
                ?.toList()
                ?: items.map { it.eventTypeId }
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
    }
    private fun onBookmarkedEventIdsChanged(eventIds: Set<Long>) {
        _uiState.update {
            val filterSourceItems = it.filterSourceItems.withBookmarkState(eventIds)
            val items = it.items.withBookmarkState(eventIds)
                .applyFilters(
                    filters = it.appliedFilters,
                    applyExcludedKeywords = !it.isLoggedIn
                )

            it.copy(
                filterSourceItems = filterSourceItems,
                items = items
            )
        }
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
    return associate { event -> event.id to event.isBookmarked }
}

private fun EventSummaryResponse.toDailyEventItem(): DailyEventItem {
    val eventEndDate = parseEventDate(eventEnd)
    val applyEndDate = parseEventDate(applyEnd)
    val dDayLabel = applyEndDate?.let { targetDate ->
        val diff = targetDate.toEpochDay() - LocalDate.now().toEpochDay()
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
        imageUrl = imageUrl,
        organization = organization,
        eventEndDisplay = eventEndDisplay,
        applyPeriodDisplay = formatPeriod(applyStart, applyEnd),
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

private fun DailyEventItem.toGuestBookmarkSnapshot(): StoredGuestBookmarkSnapshot {
    return StoredGuestBookmarkSnapshot(
        eventId = id,
        title = title,
        imageUrl = imageUrl,
        organization = organization,
        dDayLabel = dDayLabel,
        applyPeriodDisplay = applyPeriodDisplay,
        eventTypeId = eventTypeId,
        updatedAt = OffsetDateTime.now().toString()
    )
}
private fun List<DailyEventItem>.withBookmarkState(
    bookmarkedEventIds: Set<Long>
): List<DailyEventItem> {
    return map { item ->
        item.copy(isBookmarked = item.id in bookmarkedEventIds)
    }
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
        OffsetDateTime.parse(value).toLocalDate().format(ItemDateFormatter)
    }.getOrElse {
        runCatching { LocalDateTime.parse(value).toLocalDate().format(ItemDateFormatter) }.getOrElse {
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
        filters = appliedFilters,
        applyExcludedKeywords = !isLoggedIn
    )

    return copy(
        filterSourceItems = updatedFilterSourceItems,
        items = updatedItems
    )
}
