package com.example.hangsha_android.data.network.model

data class ExcludedKeywordsResponse(
    val items: List<ExcludedKeywordItemResponse> = emptyList()
)

data class ExcludedKeywordItemResponse(
    val id: Long,
    val keyword: String,
    val createdAt: String
)
