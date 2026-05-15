package com.example.hangsha_android.data.network.model

data class OrganizationCategoryResponse(
    val items: List<OrganizationCategoryItemResponse>?
)

data class OrganizationCategoryItemResponse(
    val id: Long,
    val groupId: Long,
    val name: String,
    val sortOrder: Int
)
