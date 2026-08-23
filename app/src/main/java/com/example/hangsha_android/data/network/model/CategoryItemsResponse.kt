package com.example.hangsha_android.data.network.model

import com.google.gson.annotations.SerializedName

data class CategoryItemsResponse(
    @SerializedName("items")
    val items: List<CategoryItemResponse>?
)

data class CategoryItemResponse(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("sortOrder")
    val sortOrder: Int
)
