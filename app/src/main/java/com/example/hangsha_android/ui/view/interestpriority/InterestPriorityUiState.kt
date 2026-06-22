package com.example.hangsha_android.ui.view.interestpriority

data class InterestPriorityUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val categoryGroups: List<InterestCategoryGroupUiModel> = emptyList(),
    val selectedCategoryIds: List<Long> = emptyList(),
    val errorMessage: String? = null,
    val saveErrorMessage: String? = null,
    val isSaveSuccessful: Boolean = false
)

data class InterestCategoryGroupUiModel(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val categories: List<InterestCategoryUiModel>
)

data class InterestCategoryUiModel(
    val id: Long,
    val groupId: Long,
    val name: String,
    val sortOrder: Int
)
