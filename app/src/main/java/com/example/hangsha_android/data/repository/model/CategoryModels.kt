package com.example.hangsha_android.data.repository.model

import com.google.gson.annotations.SerializedName

enum class CategoryType {
    @SerializedName("EVENT_STATUS")
    EVENT_STATUS,
    @SerializedName("EVENT_TYPE")
    EVENT_TYPE,
    @SerializedName("ORGANIZATION")
    ORGANIZATION
}

data class CategoryKey(
    val type: CategoryType,
    val id: Long
)

data class CategoryItem(
    val key: CategoryKey,
    val name: String,
    val sortOrder: Int
)
