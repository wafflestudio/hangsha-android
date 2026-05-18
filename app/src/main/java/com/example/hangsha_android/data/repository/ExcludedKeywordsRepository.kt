package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.ExcludedKeywordsApi
import com.example.hangsha_android.data.network.model.ExcludedKeywordItemResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException

@Singleton
class ExcludedKeywordsRepository @Inject constructor(
    private val excludedKeywordsApi: ExcludedKeywordsApi
) {
    private val _excludedKeywordItems =
        MutableStateFlow<List<ExcludedKeywordItemResponse>>(emptyList())
    val excludedKeywordItems: StateFlow<List<ExcludedKeywordItemResponse>> =
        _excludedKeywordItems.asStateFlow()

    private val _excludedKeywords = MutableStateFlow<List<String>>(emptyList())
    val excludedKeywords: StateFlow<List<String>> = _excludedKeywords.asStateFlow()

    fun currentExcludedKeywords(): List<String> = _excludedKeywords.value

    suspend fun refreshExcludedKeywords(): List<String> {
        val response = excludedKeywordsApi.getExcludedKeywords()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val items = response.body()?.items.orEmpty().normalizeItems()
        val keywords = items.map { it.keyword }
        _excludedKeywordItems.value = items
        _excludedKeywords.value = keywords
        return keywords
    }

    suspend fun addExcludedKeyword(keyword: String): List<String> {
        throw UnsupportedOperationException("POST excluded-keywords spec is not integrated yet.")
    }

    suspend fun removeExcludedKeyword(keyword: String): List<String> {
        throw UnsupportedOperationException("DELETE excluded-keywords spec is not integrated yet.")
    }
}

private fun List<ExcludedKeywordItemResponse>.normalizeItems(): List<ExcludedKeywordItemResponse> {
    return mapNotNull { item ->
        val normalizedKeyword = item.keyword.trim()
        if (normalizedKeyword.isBlank()) {
            null
        } else {
            item.copy(keyword = normalizedKeyword)
        }
    }.distinctBy { it.id }
}
