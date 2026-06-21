package com.example.hangsha_android.ui.view.interestpriority

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.repository.CategoryRepository
import com.example.hangsha_android.data.repository.UserRepository
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
class InterestPriorityViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InterestPriorityUiState())
    val uiState: StateFlow<InterestPriorityUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            runCatching {
                val profileResponse = userRepository.getMyProfile()
                if (!profileResponse.isSuccessful) {
                    throw HttpException(profileResponse)
                }

                val categoryResponse = categoryRepository.getCategoryGroupsWithCategories()
                if (!categoryResponse.isSuccessful) {
                    throw HttpException(categoryResponse)
                }

                val selectedIds = profileResponse.body()
                    ?.interestCategories
                    .orEmpty()
                    .sortedBy { interest -> interest.priority }
                    .map { interest -> interest.category.id }
                    .take(MAX_INTEREST_PRIORITY_COUNT)

                val groups = categoryResponse.body()
                    ?.items
                    .orEmpty()
                    .filterNot { item ->
                        item.group.id == RECRUITMENT_STATUS_GROUP_ID ||
                            item.group.name == RECRUITMENT_STATUS_GROUP_NAME
                    }
                    .sortedWith(
                        compareBy(
                            { item -> interestGroupDisplayOrder(item.group.name) },
                            { item -> item.group.sortOrder }
                        )
                    )
                    .map { item ->
                        InterestCategoryGroupUiModel(
                            id = item.group.id,
                            name = item.group.name,
                            sortOrder = item.group.sortOrder,
                            categories = item.categories
                                .orEmpty()
                                .sortedBy { category -> category.sortOrder }
                                .map { category ->
                                    InterestCategoryUiModel(
                                        id = category.id,
                                        groupId = category.groupId,
                                        name = category.name,
                                        sortOrder = category.sortOrder
                                    )
                                }
                        )
                    }

                selectedIds to groups
            }.fold(
                onSuccess = { (selectedIds, groups) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedCategoryIds = selectedIds,
                            categoryGroups = groups,
                            errorMessage = null
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

    fun toggleCategory(categoryId: Long) {
        _uiState.update { current ->
            val selectedIds = current.selectedCategoryIds
            val updatedIds = when {
                categoryId in selectedIds -> selectedIds - categoryId
                selectedIds.size < MAX_INTEREST_PRIORITY_COUNT -> selectedIds + categoryId
                else -> selectedIds
            }

            current.copy(selectedCategoryIds = updatedIds)
        }
    }

    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "로그인이 필요합니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "카테고리 목록을 불러오지 못했습니다. (${error.code()})"
            }
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            else -> error.message ?: "카테고리 목록을 불러오지 못했습니다."
        }
    }
}

private fun interestGroupDisplayOrder(groupName: String): Int {
    return when (groupName) {
        "프로그램 유형" -> 0
        "주체기관" -> 1
        else -> 2
    }
}

private const val MAX_INTEREST_PRIORITY_COUNT = 3
private const val RECRUITMENT_STATUS_GROUP_ID = 1L
private const val RECRUITMENT_STATUS_GROUP_NAME = "모집현황"
