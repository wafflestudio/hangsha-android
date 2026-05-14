package com.example.hangsha_android.ui.view.eventdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.model.EventDetailResponse
import com.example.hangsha_android.data.repository.EventRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val eventId = savedStateHandle.get<Long>(HangshaDestinations.EventDetail.eventIdArg) ?: -1L

    private val _uiState = MutableStateFlow(EventDetailUiState(eventId = eventId))
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadEventDetail()
    }

    fun retry() {
        loadEventDetail()
    }

    private fun loadEventDetail() {
        if (eventId <= 0L) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Invalid event id.",
                    item = null
                )
            }
            return
        }

        loadJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        loadJob = viewModelScope.launch {
            runCatching {
                eventRepository.getEventDetail(eventId)
                    .requireBody("Event detail response was empty.")
                    .toEventDetailItem()
            }.fold(
                onSuccess = { item ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            item = item
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapErrorMessage(error),
                            item = null
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
                403 -> "You do not have permission to view this event."
                404 -> "Event information could not be found."
                in 500..599 -> "Server error occurred. Please try again later."
                else -> "Failed to load event with code ${error.code()}."
            }
            is IOException -> "Network error occurred. Please try again."
            is IllegalStateException -> error.message ?: "Failed to load event."
            else -> error.message ?: "Failed to load event."
        }
    }
}

private val DetailDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.KOREA)
private val DetailDateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.KOREA)

private fun EventDetailResponse.toEventDetailItem(): EventDetailItem {
    return EventDetailItem(
        id = id,
        title = title,
        imageUrl = imageUrl,
        organization = organization,
        location = location,
        operationMode = operationMode,
        applyPeriodText = formatPeriod(applyStart, applyEnd),
        eventPeriodText = formatPeriod(eventStart, eventEnd),
        statusText = "statusId: $statusId",
        capacityText = capacity?.toString() ?: "-",
        applyCountText = applyCount?.toString() ?: "-",
        tags = tags.orEmpty()
            .split(",", " ", "#")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct(),
        applyLink = applyLink,
        detail = detail,
        isBookmarked = isBookmarked,
        isInterested = isInterested
    )
}

private fun formatPeriod(start: String?, end: String?): String {
    val startText = formatDateTime(start)
    val endText = formatDateTime(end)

    return when {
        startText != null && endText != null -> "$startText - $endText"
        startText != null -> startText
        endText != null -> endText
        else -> "-"
    }
}

private fun formatDateTime(value: String?): String? {
    if (value.isNullOrBlank()) {
        return null
    }

    return runCatching {
        OffsetDateTime.parse(value).toLocalDateTime().format(DetailDateTimeFormatter)
    }.getOrElse {
        runCatching { LocalDateTime.parse(value).format(DetailDateTimeFormatter) }.getOrElse {
            runCatching { LocalDate.parse(value).format(DetailDateFormatter) }.getOrNull()
        }
    }
}

private fun Response<EventDetailResponse>.requireBody(
    emptyMessage: String
): EventDetailResponse {
    if (!isSuccessful) {
        throw HttpException(this)
    }

    return body() ?: throw IllegalStateException(emptyMessage)
}
