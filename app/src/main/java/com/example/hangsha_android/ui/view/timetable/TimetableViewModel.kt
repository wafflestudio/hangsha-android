package com.example.hangsha_android.ui.view.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.model.CreateCustomTimetableEnrollTimeSlotRequest
import com.example.hangsha_android.data.network.model.EventSummaryResponse
import com.example.hangsha_android.data.network.model.TimetableEnrollResponse
import com.example.hangsha_android.data.network.model.TimetableResponse
import com.example.hangsha_android.data.network.model.UpdateCustomTimetableEnrollRequest
import com.example.hangsha_android.data.repository.EventRepository
import com.example.hangsha_android.data.repository.TimetableRepository
import com.example.hangsha_android.data.repository.model.EventDateRange
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository,
    private val eventRepository: EventRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TimetableApiUiState())
    val uiState: StateFlow<TimetableApiUiState> = _uiState.asStateFlow()

    private var loadingTimetableKey: Pair<Int, String>? = null
    private var isEnrollLoadInFlight = false
    private var loadingEventsWeek: LocalDate? = null

    fun loadWeeklyEvents(weekStart: LocalDate) {
        val monday = weekStart.minusDays((weekStart.dayOfWeek.value - 1).toLong())
        if (loadingEventsWeek == monday) return

        loadingEventsWeek = monday
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingWeeklyEvents = true,
                    loadWeeklyEventsError = null
                )
            }

            runCatching {
                val response = eventRepository.getEvents(
                    range = EventDateRange(
                        from = monday,
                        to = monday.plusDays(6)
                    )
                )
                if (!response.isSuccessful) throw HttpException(response)
                response.body()?.byDate
                    ?.values
                    ?.flatMap { day -> day.events }
                    ?.distinctBy { event -> event.id }
                    ?: throw IllegalStateException("Weekly events response was empty.")
            }.fold(
                onSuccess = { events ->
                    if (loadingEventsWeek == monday) {
                        _uiState.update {
                            it.copy(
                                isLoadingWeeklyEvents = false,
                                loadWeeklyEventsError = null,
                                loadedEventsWeek = monday,
                                weeklyEventSummaries = events
                            )
                        }
                    }
                },
                onFailure = { error ->
                    if (loadingEventsWeek == monday) {
                        _uiState.update {
                            it.copy(
                                isLoadingWeeklyEvents = false,
                                loadWeeklyEventsError = mapWeeklyEventsError(error)
                            )
                        }
                    }
                }
            )
            if (loadingEventsWeek == monday) loadingEventsWeek = null
        }
    }

    fun loadTimetables(
        year: Int,
        semester: String
    ) {
        val loadKey = year to semester
        if (loadingTimetableKey == loadKey) {
            return
        }

        loadingTimetableKey = loadKey
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingTimetables = true,
                    loadTimetablesError = null
                )
            }

            runCatching {
                val response = timetableRepository.getTimetables(
                    year = year,
                    semester = semester
                )
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
                response.body()?.items ?: throw IllegalStateException("Timetable list response was empty.")
            }.fold(
                onSuccess = { timetables ->
                    if (loadingTimetableKey == loadKey) {
                        _uiState.update {
                            it.copy(
                                isLoadingTimetables = false,
                                loadTimetablesError = null,
                                timetables = timetables
                            )
                        }
                    }
                },
                onFailure = { error ->
                    if (loadingTimetableKey == loadKey) {
                        _uiState.update {
                            it.copy(
                                isLoadingTimetables = false,
                                loadTimetablesError = mapLoadErrorMessage(error)
                            )
                        }
                    }
                }
            )
            if (loadingTimetableKey == loadKey) {
                loadingTimetableKey = null
            }
        }
    }

    fun createTimetable(
        name: String,
        year: Int,
        semester: String
    ) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank() || _uiState.value.isCreatingTimetable) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCreatingTimetable = true,
                    createTimetableError = null,
                    createdTimetable = null
                )
            }

            runCatching {
                val response = timetableRepository.createTimetable(
                    name = normalizedName,
                    year = year,
                    semester = semester
                )
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
                response.body() ?: throw IllegalStateException("Timetable response was empty.")
            }.fold(
                onSuccess = { body ->
                    _uiState.update {
                        it.copy(
                            isCreatingTimetable = false,
                            createTimetableError = null,
                            createdTimetable = body,
                            timetables = (it.timetables.filterNot { timetable -> timetable.id == body.id } + body)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCreatingTimetable = false,
                            createTimetableError = mapCreateErrorMessage(error),
                            createdTimetable = null
                        )
                    }
                }
            )
        }
    }

    fun updateTimetableName(
        timetableId: Long,
        name: String
    ) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank() || _uiState.value.updatingTimetableId != null) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    updatingTimetableId = timetableId,
                    updateTimetableError = null,
                    updatedTimetable = null
                )
            }

            runCatching {
                val response = timetableRepository.updateTimetableName(
                    timetableId = timetableId,
                    name = normalizedName
                )
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
                response.body() ?: throw IllegalStateException("Timetable response was empty.")
            }.fold(
                onSuccess = { body ->
                    _uiState.update {
                        it.copy(
                            updatingTimetableId = null,
                            updateTimetableError = null,
                            updatedTimetable = body,
                            timetables = it.timetables.map { timetable ->
                                if (timetable.id == body.id) body else timetable
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            updatingTimetableId = null,
                            updateTimetableError = mapUpdateErrorMessage(error),
                            updatedTimetable = null
                        )
                    }
                }
            )
        }
    }
    fun deleteTimetable(timetableId: Long) {
        if (_uiState.value.deletingTimetableId != null) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    deletingTimetableId = timetableId,
                    deleteTimetableError = null
                )
            }

            runCatching {
                val response = timetableRepository.deleteTimetable(timetableId)
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
            }.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            deletingTimetableId = null,
                            deleteTimetableError = null,
                            deletedTimetableId = timetableId,
                            timetables = it.timetables.filterNot { timetable -> timetable.id == timetableId },
                            loadedEnrollsTimetableId = if (it.loadedEnrollsTimetableId == timetableId) null else it.loadedEnrollsTimetableId,
                            enrolls = if (it.loadedEnrollsTimetableId == timetableId) emptyList() else it.enrolls
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            deletingTimetableId = null,
                            deleteTimetableError = mapDeleteErrorMessage(error)
                        )
                    }
                }
            )
        }
    }

    fun loadEnrolls(timetableId: Long) {
        if (isEnrollLoadInFlight && _uiState.value.loadingEnrollsTimetableId == timetableId) {
            return
        }

        isEnrollLoadInFlight = true
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loadingEnrollsTimetableId = timetableId,
                    loadEnrollsError = null
                )
            }

            runCatching {
                val response = timetableRepository.getEnrolls(timetableId = timetableId)
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
                response.body()?.items ?: throw IllegalStateException("Enroll list response was empty.")
            }.fold(
                onSuccess = { enrolls ->
                    _uiState.update {
                        it.copy(
                            loadingEnrollsTimetableId = null,
                            loadedEnrollsTimetableId = timetableId,
                            loadEnrollsError = null,
                            enrolls = enrolls
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            loadingEnrollsTimetableId = null,
                            loadEnrollsError = mapEnrollErrorMessage("load", error)
                        )
                    }
                }
            )
            isEnrollLoadInFlight = false
        }
    }

    fun loadEnroll(
        timetableId: Long,
        enrollId: Long
    ) {
        if (_uiState.value.loadingEnrollId != null) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loadingEnrollId = enrollId,
                    loadEnrollError = null,
                    loadedEnroll = null
                )
            }

            runCatching {
                val response = timetableRepository.getEnroll(
                    timetableId = timetableId,
                    enrollId = enrollId
                )
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
                response.body() ?: throw IllegalStateException("Enroll response was empty.")
            }.fold(
                onSuccess = { enroll ->
                    _uiState.update {
                        it.copy(
                            loadingEnrollId = null,
                            loadEnrollError = null,
                            loadedEnroll = enroll
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            loadingEnrollId = null,
                            loadEnrollError = mapEnrollErrorMessage("load", error),
                            loadedEnroll = null
                        )
                    }
                }
            )
        }
    }

    fun createCustomEnroll(
        timetableId: Long,
        year: Int,
        semester: String,
        courseTitle: String,
        timeSlots: List<CreateCustomTimetableEnrollTimeSlotRequest>,
        courseNumber: String? = null,
        lectureNumber: String? = null,
        credit: Int? = null,
        instructor: String? = null
    ) {
        val normalizedTitle = courseTitle.trim()
        if (normalizedTitle.isBlank() || timeSlots.isEmpty() || _uiState.value.isCreatingCustomEnroll) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCreatingCustomEnroll = true,
                    createCustomEnrollError = null,
                    createdCustomEnroll = null
                )
            }

            runCatching {
                val response = timetableRepository.createCustomEnroll(
                    timetableId = timetableId,
                    year = year,
                    semester = semester,
                    courseTitle = normalizedTitle,
                    timeSlots = timeSlots,
                    courseNumber = courseNumber,
                    lectureNumber = lectureNumber,
                    credit = credit,
                    instructor = instructor
                )
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
                response.body() ?: throw IllegalStateException("Enroll response was empty.")
            }.fold(
                onSuccess = { enroll ->
                    _uiState.update {
                        val updatedEnrolls = if (it.loadedEnrollsTimetableId == timetableId) {
                            it.enrolls.filterNot { current -> current.enrollId == enroll.enrollId } + enroll
                        } else {
                            listOf(enroll)
                        }
                        it.copy(
                            isCreatingCustomEnroll = false,
                            createCustomEnrollError = null,
                            createdCustomEnroll = enroll,
                            loadedEnrollsTimetableId = timetableId,
                            enrolls = updatedEnrolls
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCreatingCustomEnroll = false,
                            createCustomEnrollError = mapEnrollErrorMessage("create", error),
                            createdCustomEnroll = null
                        )
                    }
                }
            )
        }
    }


    fun updateCustomEnroll(
        timetableId: Long,
        enrollId: Long,
        request: UpdateCustomTimetableEnrollRequest
    ) {
        if (_uiState.value.updatingEnrollId != null) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    updatingEnrollId = enrollId,
                    updateEnrollError = null,
                    updatedEnroll = null
                )
            }

            runCatching {
                val response = timetableRepository.updateCustomEnroll(
                    timetableId = timetableId,
                    enrollId = enrollId,
                    request = request
                )
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
                response.body() ?: throw IllegalStateException("Enroll response was empty.")
            }.fold(
                onSuccess = { enroll ->
                    _uiState.update {
                        it.copy(
                            updatingEnrollId = null,
                            updateEnrollError = null,
                            updatedEnroll = enroll,
                            loadedEnroll = if (it.loadedEnroll?.enrollId == enroll.enrollId) enroll else it.loadedEnroll,
                            enrolls = if (it.loadedEnrollsTimetableId == timetableId) {
                                it.enrolls.map { current ->
                                    if (current.enrollId == enroll.enrollId) enroll else current
                                }
                            } else {
                                it.enrolls
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            updatingEnrollId = null,
                            updateEnrollError = mapEnrollErrorMessage("update", error),
                            updatedEnroll = null
                        )
                    }
                }
            )
        }
    }

    fun onUpdatedEnrollConsumed() {
        _uiState.update {
            it.copy(updatedEnroll = null)
        }
    }

    fun clearUpdateEnrollError() {
        _uiState.update {
            it.copy(updateEnrollError = null)
        }
    }

    fun deleteEnroll(
        timetableId: Long,
        enrollId: Long
    ) {
        if (_uiState.value.deletingEnrollId != null) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    deletingEnrollId = enrollId,
                    deleteEnrollError = null
                )
            }

            runCatching {
                val response = timetableRepository.deleteEnroll(
                    timetableId = timetableId,
                    enrollId = enrollId
                )
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
            }.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            deletingEnrollId = null,
                            deletedEnrollId = enrollId,
                            deleteEnrollError = null,
                            enrolls = if (it.loadedEnrollsTimetableId == timetableId) {
                                it.enrolls.filterNot { enroll -> enroll.enrollId == enrollId }
                            } else {
                                it.enrolls
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            deletingEnrollId = null,
                            deleteEnrollError = mapEnrollErrorMessage("delete", error)
                        )
                    }
                }
            )
        }
    }

    fun onCreatedCustomEnrollConsumed() {
        _uiState.update {
            it.copy(createdCustomEnroll = null)
        }
    }

    fun onDeletedEnrollConsumed() {
        _uiState.update {
            it.copy(deletedEnrollId = null)
        }
    }

    fun clearCreateCustomEnrollError() {
        _uiState.update {
            it.copy(createCustomEnrollError = null)
        }
    }

    fun clearDeleteEnrollError() {
        _uiState.update {
            it.copy(deleteEnrollError = null)
        }
    }

    fun clearLoadEnrollError() {
        _uiState.update {
            it.copy(loadEnrollError = null)
        }
    }

    fun clearLoadEnrollsError() {
        _uiState.update {
            it.copy(loadEnrollsError = null)
        }
    }

    fun clearLoadedEnrolls() {
        _uiState.update {
            it.copy(
                loadingEnrollsTimetableId = null,
                loadedEnrollsTimetableId = null,
                loadEnrollsError = null,
                enrolls = emptyList(),
                loadingEnrollId = null,
                loadedEnroll = null,
                loadEnrollError = null
            )
        }
    }


    fun onUpdatedTimetableConsumed() {
        _uiState.update {
            it.copy(updatedTimetable = null)
        }
    }

    fun clearUpdateError() {
        _uiState.update {
            it.copy(updateTimetableError = null)
        }
    }
    fun onDeletedTimetableConsumed() {
        _uiState.update {
            it.copy(deletedTimetableId = null)
        }
    }

    fun clearDeleteError() {
        _uiState.update {
            it.copy(deleteTimetableError = null)
        }
    }
    fun onCreatedTimetableConsumed() {
        _uiState.update {
            it.copy(createdTimetable = null)
        }
    }

    fun clearCreateError() {
        _uiState.update {
            it.copy(createTimetableError = null)
        }
    }

    private fun mapLoadErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                400 -> "조회할 학기 정보를 확인해 주세요."
                401 -> "로그인이 필요합니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "시간표 목록을 불러오지 못했습니다. (${error.code()})"
            }
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            is IllegalStateException -> error.message ?: "시간표 목록을 불러오지 못했습니다."
            else -> error.message ?: "시간표 목록을 불러오지 못했습니다."
        }
    }

    private fun mapUpdateErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                400 -> "시간표 이름을 확인해 주세요."
                401 -> "로그인이 필요합니다."
                403 -> "시간표 수정 권한이 없습니다."
                404 -> "이미 삭제되었거나 찾을 수 없는 시간표입니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "시간표 수정에 실패했습니다. (${error.code()})"
            }
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            is IllegalStateException -> error.message ?: "시간표 수정에 실패했습니다."
            else -> error.message ?: "시간표 수정에 실패했습니다."
        }
    }
    private fun mapDeleteErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "로그인이 필요합니다."
                403 -> "시간표 삭제 권한이 없습니다."
                404 -> "이미 삭제되었거나 찾을 수 없는 시간표입니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "시간표 삭제에 실패했습니다. (${error.code()})"
            }
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            else -> error.message ?: "시간표 삭제에 실패했습니다."
        }
    }
    private fun mapEnrollErrorMessage(action: String, error: Throwable): String {
        val actionLabel = enrollActionLabel(action)
        return when (error) {
            is HttpException -> when (error.code()) {
                400 -> "\uC218\uC5C5 \uC815\uBCF4\uB97C \uD655\uC778\uD574 \uC8FC\uC138\uC694."
                401 -> "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."
                403 -> "\uC774 \uC2DC\uAC04\uD45C\uC5D0 \uB300\uD55C \uAD8C\uD55C\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."
                404 -> "\uC2DC\uAC04\uD45C\uB098 \uC218\uC5C5\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4."
                in 500..599 -> "\uC11C\uBC84 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
                else -> "$actionLabel\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4. (${error.code()})"
            }
            is UnknownHostException -> "\uC778\uD130\uB137 \uC5F0\uACB0\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            is SocketTimeoutException -> "\uC694\uCCAD \uC2DC\uAC04\uC774 \uCD08\uACFC\uB418\uC5C8\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is IOException -> "\uB124\uD2B8\uC6CC\uD06C \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC2B5\uB2C8\uB2E4. \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
            is IllegalStateException -> error.message ?: "$actionLabel\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."
            else -> error.message ?: "$actionLabel\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4."
        }
    }

    private fun enrollActionLabel(action: String): String {
        return when (action) {
            "load" -> "\uC218\uC5C5 \uBAA9\uB85D \uBD88\uB7EC\uC624\uAE30"
            "create" -> "\uC218\uC5C5 \uCD94\uAC00"
            "update" -> "\uC218\uC5C5 \uC218\uC815"
            "delete" -> "\uC218\uC5C5 \uC0AD\uC81C"
            else -> "\uC218\uC5C5 \uC694\uCCAD"
        }
    }

    private fun mapWeeklyEventsError(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "\uB85C\uADF8\uC778\uC774 \uD544\uC694\uD569\uB2C8\uB2E4."
                in 500..599 -> "\uC11C\uBC84 \uC624\uB958\uB85C \uD589\uC0AC\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
                else -> "\uD589\uC0AC\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4. (${error.code()})"
            }
            is UnknownHostException -> "\uC778\uD130\uB137 \uC5F0\uACB0\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694."
            is SocketTimeoutException -> "\uD589\uC0AC \uC694\uCCAD \uC2DC\uAC04\uC774 \uCD08\uACFC\uB418\uC5C8\uC2B5\uB2C8\uB2E4."
            is IOException -> "\uB124\uD2B8\uC6CC\uD06C \uC624\uB958\uB85C \uD589\uC0AC\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
            else -> error.message ?: "\uD589\uC0AC\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4."
        }
    }

    private fun mapCreateErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                400 -> "시간표 정보를 확인해 주세요."
                401 -> "로그인이 필요합니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "시간표 생성에 실패했습니다. (${error.code()})"
            }
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            is IllegalStateException -> error.message ?: "시간표 생성에 실패했습니다."
            else -> error.message ?: "시간표 생성에 실패했습니다."
        }
    }
}

data class TimetableApiUiState(
    val isLoadingWeeklyEvents: Boolean = false,
    val loadWeeklyEventsError: String? = null,
    val loadedEventsWeek: LocalDate? = null,
    val weeklyEventSummaries: List<EventSummaryResponse> = emptyList(),
    val isLoadingTimetables: Boolean = false,
    val loadTimetablesError: String? = null,
    val timetables: List<TimetableResponse> = emptyList(),
    val loadingEnrollsTimetableId: Long? = null,
    val loadedEnrollsTimetableId: Long? = null,
    val loadEnrollsError: String? = null,
    val enrolls: List<TimetableEnrollResponse> = emptyList(),
    val loadingEnrollId: Long? = null,
    val loadedEnroll: TimetableEnrollResponse? = null,
    val loadEnrollError: String? = null,
    val isCreatingCustomEnroll: Boolean = false,
    val createCustomEnrollError: String? = null,
    val createdCustomEnroll: TimetableEnrollResponse? = null,
    val updatingEnrollId: Long? = null,
    val updatedEnroll: TimetableEnrollResponse? = null,
    val updateEnrollError: String? = null,
    val deletingEnrollId: Long? = null,
    val deletedEnrollId: Long? = null,
    val deleteEnrollError: String? = null,
    val isCreatingTimetable: Boolean = false,
    val createTimetableError: String? = null,
    val createdTimetable: TimetableResponse? = null,
    val updatingTimetableId: Long? = null,
    val updatedTimetable: TimetableResponse? = null,
    val updateTimetableError: String? = null,
    val deletingTimetableId: Long? = null,
    val deletedTimetableId: Long? = null,
    val deleteTimetableError: String? = null
)