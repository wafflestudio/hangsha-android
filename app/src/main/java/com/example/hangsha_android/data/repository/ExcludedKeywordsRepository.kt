package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.local.ExcludedKeywordsLocalDataSource
import com.example.hangsha_android.data.local.StoredExcludedKeywordItem
import com.example.hangsha_android.data.network.api.ExcludedKeywordsApi
import com.example.hangsha_android.data.network.model.CreateExcludedKeywordRequest
import com.example.hangsha_android.data.network.model.ExcludedKeywordItemResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class ExcludedKeywordsRepository @Inject constructor(
    private val excludedKeywordsApi: ExcludedKeywordsApi,
    private val localDataSource: ExcludedKeywordsLocalDataSource,
    private val authTokenStorage: AuthTokenStorage
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _excludedKeywordItems =
        MutableStateFlow<List<ExcludedKeywordItemResponse>>(emptyList())
    val excludedKeywordItems: StateFlow<List<ExcludedKeywordItemResponse>> =
        _excludedKeywordItems.asStateFlow()

    private val _excludedKeywords = MutableStateFlow<List<String>>(emptyList())
    val excludedKeywords: StateFlow<List<String>> = _excludedKeywords.asStateFlow()

    init {
        repositoryScope.launch {
            localDataSource.excludedKeywordItems.collectLatest { items ->
                _excludedKeywordItems.value = items.toNetworkResponseItems()
                _excludedKeywords.value = items.map { it.keyword }
            }
        }
    }

    fun currentExcludedKeywords(): List<String> = _excludedKeywords.value

    suspend fun refreshExcludedKeywords(): List<String> {
        val response = excludedKeywordsApi.getExcludedKeywords()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val items = response.body()?.items.orEmpty().normalizeItems()
        val keywords = items.map { it.keyword }
        localDataSource.replaceItems(items.toStoredItems())
        return keywords
    }

    suspend fun addExcludedKeyword(keyword: String): List<String> {
        val previousItems = localDataSource.excludedKeywordItems.first()
        val updatedItems = localDataSource.addKeyword(keyword)
        val normalizedKeyword = keyword.trim()

        if (normalizedKeyword.isBlank() || !isLoggedIn()) {
            return updatedItems.map { it.keyword }
        }

        val response = excludedKeywordsApi.addExcludedKeyword(
            CreateExcludedKeywordRequest(keyword = normalizedKeyword)
        )
        if (!response.isSuccessful) {
            localDataSource.replaceItems(previousItems)
            throw HttpException(response)
        }

        return updatedItems.map { it.keyword }
    }

    suspend fun removeExcludedKeyword(keyword: String): List<String> {
        return localDataSource.removeKeyword(keyword).map { it.keyword }
    }

    private fun isLoggedIn(): Boolean {
        return !authTokenStorage.getAccessToken().isNullOrBlank()
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

private fun List<ExcludedKeywordItemResponse>.toStoredItems(): List<StoredExcludedKeywordItem> {
    return map { item ->
        StoredExcludedKeywordItem(
            id = item.id,
            keyword = item.keyword,
            createdAt = item.createdAt
        )
    }
}

private fun List<StoredExcludedKeywordItem>.toNetworkResponseItems(): List<ExcludedKeywordItemResponse> {
    return mapNotNull { item ->
        val id = item.id ?: return@mapNotNull null
        val createdAt = item.createdAt ?: return@mapNotNull null
        ExcludedKeywordItemResponse(
            id = id,
            keyword = item.keyword,
            createdAt = createdAt
        )
    }
}
