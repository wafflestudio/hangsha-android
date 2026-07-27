package com.example.hangsha_android.ui.view.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.model.TimetableResponse
import com.example.hangsha_android.data.repository.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TimetableApiUiState())
    val uiState: StateFlow<TimetableApiUiState> = _uiState.asStateFlow()

    private var isLoadInFlight = false

    fun loadTimetables(
        year: Int,
        semester: String
    ) {
        if (isLoadInFlight) {
            return
        }

        isLoadInFlight = true
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
                    _uiState.update {
                        it.copy(
                            isLoadingTimetables = false,
                            loadTimetablesError = null,
                            timetables = timetables
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingTimetables = false,
                            loadTimetablesError = mapLoadErrorMessage(error)
                        )
                    }
                }
            )
            isLoadInFlight = false
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
                            timetables = it.timetables.filterNot { timetable -> timetable.id == timetableId }
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
    val isLoadingTimetables: Boolean = false,
    val loadTimetablesError: String? = null,
    val timetables: List<TimetableResponse> = emptyList(),
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