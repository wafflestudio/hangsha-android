package com.example.hangsha_android.data.network.model

import com.example.hangsha_android.data.repository.model.CategoryKey
import com.example.hangsha_android.data.repository.model.CategoryType
import com.google.gson.annotations.SerializedName

data class UserProfileResponse(
    @SerializedName("id")
    val id: Long,
    @SerializedName("username")
    val username: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("profileImageUrl")
    val profileImageUrl: String?,
    @SerializedName("interestCategories")
    val interestCategories: List<UserInterestCategory>?
)

data class UserInterestCategory(
    @SerializedName("categoryType")
    val categoryType: CategoryType,
    @SerializedName("categoryId")
    val categoryId: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("sortOrder")
    val sortOrder: Int,
    @SerializedName("priority")
    val priority: Int
) {
    val key: CategoryKey
        get() = CategoryKey(type = categoryType, id = categoryId)
}

data class UserInterestCategoriesResponse(
    @SerializedName("items")
    val items: List<UserInterestCategory>?
)
