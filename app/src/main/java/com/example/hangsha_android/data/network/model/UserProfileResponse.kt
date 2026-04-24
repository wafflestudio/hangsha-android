package com.example.hangsha_android.data.network.model

data class UserProfileResponse(
    val id: Long,
    val username: String,
    val email: String,
    val profileImageUrl: String?,
    val interestCategories: List<UserInterestCategory>
)

data class UserInterestCategory(
    val category: InterestCategory,
    val priority: Int
)

data class InterestCategory(
    val id: Long,
    val groupId: Long,
    val name: String,
    val sortOrder: Int
)
