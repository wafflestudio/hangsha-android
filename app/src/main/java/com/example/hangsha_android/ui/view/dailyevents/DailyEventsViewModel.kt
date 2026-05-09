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

    private fun loadDate(date: LocalDate) {
        loadJob?.cancel()
        _uiState.update {
            it.copy(
                selectedDate = date,
                isLoading = true,
                errorMessage = null
            )
        }

        loadJob = viewModelScope.launch {
            runCatching {
                val response = eventRepository.getDayEvents(date)
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }

                response.body()?.items.orEmpty()
            }.fold(
                onSuccess = { items ->
                    _uiState.update {
                        it.copy(
                            items = items.mapIndexed { index, event ->
                                event.toDailyEventItem(index)
                            },
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            items = emptyList(),
                            isLoading = false,
                            errorMessage = mapErrorMessage(error)
                        )
                    }
                }
            )
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
            else -> error.message ?: "Failed to load events."
        }
    }
}

private val ItemDateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.KOREA)

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
    } ?: "모집중"

    val displayDate = baseDate?.format(ItemDateFormatter) ?: "-"

    return DailyEventItem(
        id = id,
        title = title,
        organization = organization,
        displayDate = displayDate,
        dDayLabel = dDayLabel,
        accentColor = DailyEventAccentPalette[index % DailyEventAccentPalette.size],
        isBookmarked = isBookmarked
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
