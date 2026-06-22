package com.example.hangsha_android.data.network.model

import com.google.gson.annotations.SerializedName

data class UpdateInterestCategoriesRequest(
    @SerializedName("items")
    val items: List<UpdateInterestCategoryItemRequest>
)

data class UpdateInterestCategoryItemRequest(
    @SerializedName("categoryId")
    val categoryId: Long,
    @SerializedName("priority")
    val priority: Int
)
