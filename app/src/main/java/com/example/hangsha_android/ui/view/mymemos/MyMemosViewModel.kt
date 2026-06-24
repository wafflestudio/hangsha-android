package com.example.hangsha_android.ui.view.mymemos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.model.MemoResponse
import com.example.hangsha_android.data.repository.MemoRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class MyMemosViewModel @Inject constructor(
    private val memoRepository: MemoRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyMemosUiState())
    val uiState: StateFlow<MyMemosUiState> = _uiState.asStateFlow()

    init {
        loadMemos()
    }

    fun loadMemos() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null)
            }

            runCatching {
                val response = memoRepository.getMemos()
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }

                response.body() ?: throw IllegalStateException("Memos response was empty.")
            }.fold(
                onSuccess = { body ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            groupedMemos = body.items.toMyMemoDateGroups()
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapErrorMessage(error)
                        )
                    }
                }
            )
        }
    }

    fun deleteMemo(memoId: Long) {
        if (_uiState.value.deletingMemoId != null) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(deletingMemoId = memoId, toastMessage = null)
            }

            runCatching {
                val response = memoRepository.deleteMemo(memoId)
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }
            }.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            deletingMemoId = null,
                            groupedMemos = it.groupedMemos.removeMemo(memoId),
                            toastMessage = "메모가 삭제되었습니다."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            deletingMemoId = null,
                            toastMessage = mapDeleteErrorMessage(error)
                        )
                    }
                }
            )
        }
    }

    fun onToastMessageConsumed() {
        _uiState.update {
            it.copy(toastMessage = null)
        }
    }

    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "로그인이 필요합니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "메모 목록을 불러오지 못했습니다. (${error.code()})"
            }
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            else -> error.message ?: "메모 목록을 불러오지 못했습니다."
        }
    }

    private fun mapDeleteErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "로그인이 필요합니다."
                403 -> "메모 삭제 권한이 없습니다."
                404 -> "이미 삭제되었거나 찾을 수 없는 메모입니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "메모 삭제에 실패했습니다. (${error.code()})"
            }
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            else -> error.message ?: "메모 삭제에 실패했습니다."
        }
    }
}

private fun List<MyMemoDateGroup>.removeMemo(memoId: Long): List<MyMemoDateGroup> {
    return mapNotNull { group ->
        val updatedMemos = group.memos.filterNot { memo -> memo.id == memoId }
        if (updatedMemos.isEmpty()) {
            null
        } else {
            group.copy(memos = updatedMemos)
        }
    }
}

private fun List<MemoResponse>.toMyMemoDateGroups(): List<MyMemoDateGroup> {
    return map { memo -> memo.toMyMemoItem() }
        .groupBy { memo -> memo.dateDisplay }
        .map { (dateDisplay, memos) ->
            MyMemoDateGroup(
                dateDisplay = dateDisplay,
                memos = memos
            )
        }
}

private fun MemoResponse.toMyMemoItem(): MyMemoItem {
    val dateDisplay = formatMemoDate(createdAt)
        ?: "-"

    return MyMemoItem(
        id = id,
        eventId = eventId,
        eventTitle = eventTitle,
        content = content,
        tagNames = tags.map { tag -> tag.name },
        dateDisplay = dateDisplay
    )
}

private fun formatMemoDate(value: String?): String? {
    return parseDate(value)?.format(MemoDateFormatter)
}

private fun parseDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) {
        return null
    }

    return runCatching { OffsetDateTime.parse(value).toLocalDate() }.getOrElse {
        runCatching { LocalDateTime.parse(value).toLocalDate() }.getOrElse {
            runCatching { LocalDate.parse(value) }.getOrNull()
        }
    }
}

private val MemoDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA)
