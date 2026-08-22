package com.example.hangsha_android.ui.view.interestpriority

import com.example.hangsha_android.data.repository.model.CategoryKey
import com.example.hangsha_android.data.repository.model.CategoryType

data class InterestPriorityUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val categoryGroups: List<InterestCategoryGroupUiModel> = emptyList(),
    val selectedCategoryIds: List<CategoryKey> = emptyList(),
    val errorMessage: String? = null,
    val saveErrorMessage: String? = null,
    val isSaveSuccessful: Boolean = false
)

data class InterestCategoryGroupUiModel(
    val type: CategoryType,
    val name: String,
    val sortOrder: Int,
    val categories: List<InterestCategoryUiModel>
)

data class InterestCategoryUiModel(
    val key: CategoryKey,
    val name: String,
    val sortOrder: Int
)
