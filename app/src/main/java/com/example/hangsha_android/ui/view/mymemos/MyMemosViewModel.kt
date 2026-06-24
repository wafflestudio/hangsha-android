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
    private var isLoadInFlight = false

    init {
        loadMemos()
    }

    fun loadMemos() {
        if (isLoadInFlight) {
            return
        }

        isLoadInFlight = true
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(isLoading = it.groupedMemos.isEmpty(), errorMessage = null)
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
            } finally {
                isLoadInFlight = false
            }
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

    fun startEditMemo(memo: MyMemoItem) {
        _uiState.update {
            it.copy(
                editingMemoId = memo.id,
                editContent = memo.content,
                editTagNames = memo.tagNames,
                isAddingTag = false,
                editTagInput = "",
                toastMessage = null
            )
        }
    }

    fun onEditContentChanged(value: String) {
        _uiState.update {
            it.copy(editContent = value)
        }
    }

    fun startAddingTag() {
        _uiState.update {
            it.copy(isAddingTag = true, editTagInput = "")
        }
    }

    fun onEditTagInputChanged(value: String) {
        _uiState.update {
            it.copy(editTagInput = value)
        }
    }

    fun addEditTag() {
        val tagName = _uiState.value.editTagInput.trim()
        if (tagName.isBlank()) {
            return
        }

        _uiState.update {
            it.copy(
                editTagInput = "",
                isAddingTag = false,
                editTagNames = (it.editTagNames + tagName).distinct()
            )
        }
    }

    fun removeEditTag(tagName: String) {
        _uiState.update {
            it.copy(editTagNames = it.editTagNames - tagName)
        }
    }

    fun saveEditedMemo() {
        val currentState = _uiState.value
        val memoId = currentState.editingMemoId ?: return
        if (currentState.savingMemoId != null) {
            return
        }

        val content = currentState.editContent.trim()
        val tagNames = (currentState.editTagNames + currentState.editTagInput.trim())
            .filter { it.isNotBlank() }
            .distinct()

        viewModelScope.launch {
            _uiState.update {
                it.copy(savingMemoId = memoId, toastMessage = null)
            }

            runCatching {
                if (content.isBlank() && tagNames.isEmpty()) {
                    val response = memoRepository.deleteMemo(memoId)
                    if (!response.isSuccessful) {
                        throw HttpException(response)
                    }
                    null
                } else {
                    val response = memoRepository.updateMemo(
                        memoId = memoId,
                        content = content,
                        tagNames = tagNames
                    )
                    if (!response.isSuccessful) {
                        throw HttpException(response)
                    }

                    response.body() ?: throw IllegalStateException("Memo response was empty.")
                }
            }.fold(
                onSuccess = { memo ->
                    _uiState.update {
                        val updatedGroups = if (memo == null) {
                            it.groupedMemos.removeMemo(memoId)
                        } else {
                            it.groupedMemos.replaceMemo(memo.toMyMemoItem())
                        }
                        it.copy(
                            savingMemoId = null,
                            editingMemoId = null,
                            editContent = "",
                            editTagNames = emptyList(),
                            isAddingTag = false,
                            editTagInput = "",
                            groupedMemos = updatedGroups,
                            toastMessage = if (memo == null) {
                                "메모가 삭제되었습니다."
                            } else {
                                "메모가 수정되었습니다."
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            savingMemoId = null,
                            toastMessage = mapSaveErrorMessage(error)
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

    private fun mapSaveErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                400 -> "메모 내용을 확인해 주세요."
                401 -> "로그인이 필요합니다."
                403 -> "메모 수정 권한이 없습니다."
                404 -> "찾을 수 없는 메모입니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "메모 수정에 실패했습니다. (${error.code()})"
            }
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            is IllegalStateException -> error.message ?: "메모 수정에 실패했습니다."
            else -> error.message ?: "메모 수정에 실패했습니다."
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

private fun List<MyMemoDateGroup>.replaceMemo(updatedMemo: MyMemoItem): List<MyMemoDateGroup> {
    val removed = removeMemo(updatedMemo.id)
    val groupIndex = removed.indexOfFirst { group -> group.dateDisplay == updatedMemo.dateDisplay }

    if (groupIndex < 0) {
        return removed + MyMemoDateGroup(
            dateDisplay = updatedMemo.dateDisplay,
            memos = listOf(updatedMemo)
        )
    }

    return removed.mapIndexed { index, group ->
        if (index == groupIndex) {
            group.copy(memos = group.memos + updatedMemo)
        } else {
            group
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
