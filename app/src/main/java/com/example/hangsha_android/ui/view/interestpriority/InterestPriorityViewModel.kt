package com.example.hangsha_android.ui.view.interestpriority

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hangsha_android.data.network.model.UserInterestCategory
import com.example.hangsha_android.data.repository.CategoryRepository
import com.example.hangsha_android.data.repository.UserRepository
import com.example.hangsha_android.data.repository.model.CategoryItem
import com.example.hangsha_android.data.repository.model.CategoryKey
import com.example.hangsha_android.data.repository.model.CategoryType
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
                    errorMessage = null,
                    saveErrorMessage = null,
                    isSaveSuccessful = false
                )
            }

            runCatching {
                val response = userRepository.getMyInterestCategories()
                if (!response.isSuccessful) throw HttpException(response)
                categoryRepository.ensureCategoryCatalogLoaded()

                val visibleInterests = response.body()
                    ?.items
                    .orEmpty()
                    .filter { interest -> interest.categoryType in VISIBLE_CATEGORY_TYPES }
                val selectedKeys = visibleInterests
                    .sortedBy { interest -> interest.priority }
                    .map { interest -> interest.key }
                    .distinct()
                    .take(MAX_INTEREST_PRIORITY_COUNT)
                val groups = listOf(
                    buildGroup(
                        type = CategoryType.EVENT_TYPE,
                        name = "프로그램 유형",
                        sortOrder = 0,
                        catalog = categoryRepository.eventTypes.value,
                        interests = visibleInterests
                    ),
                    buildGroup(
                        type = CategoryType.ORGANIZATION,
                        name = "주체기관",
                        sortOrder = 1,
                        catalog = categoryRepository.organizations.value,
                        interests = visibleInterests
                    )
                )
                selectedKeys to groups
            }.fold(
                onSuccess = { (selectedKeys, groups) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedCategoryIds = selectedKeys,
                            categoryGroups = groups,
                            errorMessage = null,
                            saveErrorMessage = null
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

    fun toggleCategory(categoryKey: CategoryKey) {
        _uiState.update { current ->
            val selectedKeys = current.selectedCategoryIds
            val updatedKeys = when {
                categoryKey in selectedKeys -> selectedKeys - categoryKey
                selectedKeys.size < MAX_INTEREST_PRIORITY_COUNT -> selectedKeys + categoryKey
                else -> selectedKeys
            }
            current.copy(
                selectedCategoryIds = updatedKeys,
                saveErrorMessage = null,
                isSaveSuccessful = false
            )
        }
    }

    fun save() {
        val selectedKeys = _uiState.value.selectedCategoryIds.take(MAX_INTEREST_PRIORITY_COUNT)
        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    saveErrorMessage = null,
                    isSaveSuccessful = false
                )
            }

            runCatching {
                val response = userRepository.updateMyInterestCategories(selectedKeys)
                if (!response.isSuccessful) throw HttpException(response)
            }.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            selectedCategoryIds = selectedKeys,
                            saveErrorMessage = null,
                            isSaveSuccessful = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveErrorMessage = mapSaveErrorMessage(error),
                            isSaveSuccessful = false
                        )
                    }
                }
            )
        }
    }

    fun onSaveSuccessConsumed() {
        _uiState.update { it.copy(isSaveSuccessful = false) }
    }

    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "로그인이 필요합니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "카테고리 목록을 불러오지 못했습니다. (" + error.code() + ")"
            }
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            else -> error.message ?: "카테고리 목록을 불러오지 못했습니다."
        }
    }

    private fun mapSaveErrorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                400 -> "관심사 우선순위를 확인해 주세요."
                401 -> "로그인이 필요합니다."
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                else -> "관심사 우선순위 저장에 실패했습니다. (" + error.code() + ")"
            }
            is UnknownHostException -> "인터넷 연결을 확인해 주세요."
            is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
            is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
            else -> error.message ?: "관심사 우선순위 저장에 실패했습니다."
        }
    }
}

private fun buildGroup(
    type: CategoryType,
    name: String,
    sortOrder: Int,
    catalog: List<CategoryItem>,
    interests: List<UserInterestCategory>
): InterestCategoryGroupUiModel {
    val currentInterestItems = interests
        .asSequence()
        .filter { interest -> interest.categoryType == type }
        .map { interest ->
            CategoryItem(
                key = interest.key,
                name = interest.name,
                sortOrder = interest.sortOrder
            )
        }
        .toList()
    val mergedItems = (catalog + currentInterestItems)
        .associateBy { item -> item.key }
        .values
        .sortedWith(compareBy<CategoryItem> { it.sortOrder }.thenBy { it.key.id })
    return InterestCategoryGroupUiModel(
        type = type,
        name = name,
        sortOrder = sortOrder,
        categories = mergedItems.map { item ->
            InterestCategoryUiModel(
                key = item.key,
                name = item.name,
                sortOrder = item.sortOrder
            )
        }
    )
}

private const val MAX_INTEREST_PRIORITY_COUNT = 3
private val VISIBLE_CATEGORY_TYPES = setOf(
    CategoryType.EVENT_TYPE,
    CategoryType.ORGANIZATION
)
