package com.example.hangsha_android.data.network.model

import com.example.hangsha_android.data.repository.model.CategoryType
import com.google.gson.annotations.SerializedName

data class UpdateInterestCategoriesRequest(
    @SerializedName("items")
    val items: List<UpdateInterestCategoryItemRequest>
)

data class UpdateInterestCategoryItemRequest(
    @SerializedName("categoryType")
    val categoryType: CategoryType,
    @SerializedName("categoryId")
    val categoryId: Long,
    @SerializedName("priority")
    val priority: Int
)
