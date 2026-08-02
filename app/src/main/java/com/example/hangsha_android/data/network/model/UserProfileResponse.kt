package com.example.hangsha_android.data.network.model

import com.google.gson.annotations.SerializedName

data class UserProfileResponse(
    @SerializedName("id")
    val id: Long,
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("profileImageUrl")
    val profileImageUrl: String?,
    @SerializedName("interestCategories")
    val interestCategories: List<UserInterestCategory>?
)

data class UserInterestCategory(
    @SerializedName("category")
    val category: InterestCategory,
    @SerializedName("priority")
    val priority: Int
)

data class UserInterestCategoriesResponse(
    @SerializedName("items")
    val items: List<UserInterestCategory>?
)

data class InterestCategory(
    @SerializedName("id")
    val id: Long,
    @SerializedName("groupId")
    val groupId: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("sortOrder")
    val sortOrder: Int
)
