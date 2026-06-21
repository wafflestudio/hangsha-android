package com.example.hangsha_android.data.network.model

import com.google.gson.annotations.SerializedName

data class CategoryGroupsWithCategoriesResponse(
    @SerializedName("items")
    val items: List<CategoryGroupWithCategoriesItemResponse>?
)

data class CategoryGroupWithCategoriesItemResponse(
    @SerializedName("group")
    val group: CategoryGroupResponse,
    @SerializedName("categories")
    val categories: List<CategoryItemResponse>?
)

data class CategoryGroupResponse(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("sortOrder")
    val sortOrder: Int
)

data class CategoryItemResponse(
    @SerializedName("id")
    val id: Long,
    @SerializedName("groupId")
    val groupId: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("sortOrder")
    val sortOrder: Int
)
