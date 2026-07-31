package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.local.ExcludedKeywordsLocalDataSource
import com.example.hangsha_android.data.local.StoredExcludedKeywordItem
import com.example.hangsha_android.data.network.api.ExcludedKeywordsApi
import com.example.hangsha_android.data.network.model.CreateExcludedKeywordRequest
import com.example.hangsha_android.data.network.model.ExcludedKeywordItemResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ExcludedKeywordsRepository @Inject constructor(
    private val excludedKeywordsApi: ExcludedKeywordsApi,
    private val localDataSource: ExcludedKeywordsLocalDataSource,
    private val authTokenStorage: AuthTokenStorage
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutationMutex = Mutex()

    private val _excludedKeywordItems =
        MutableStateFlow<List<ExcludedKeywordItemResponse>>(emptyList())
    val excludedKeywordItems: StateFlow<List<ExcludedKeywordItemResponse>> =
        _excludedKeywordItems.asStateFlow()

    private val _excludedKeywords = MutableStateFlow<List<String>>(emptyList())
    val excludedKeywords: StateFlow<List<String>> = _excludedKeywords.asStateFlow()

    init {
        repositoryScope.launch {
            authTokenStorage.currentUserId
                .flatMapLatest { userId -> localDataSource.excludedKeywordItems(userId) }
                .collectLatest { items ->
                    _excludedKeywordItems.value = items.toNetworkResponseItems()
                    _excludedKeywords.value = items.map { it.keyword }
                }
        }
    }

    fun currentExcludedKeywords(): List<String> = _excludedKeywords.value

    suspend fun refreshExcludedKeywords(): List<String> {
        return mutationMutex.withLock {
            val userId = authTokenStorage.getCurrentUserId()
                ?: return@withLock localDataSource
                    .getExcludedKeywordItems(userId = null)
                    .map { it.keyword }
            refreshExcludedKeywordsFromRemote(userId)
        }
    }

    suspend fun addExcludedKeyword(keyword: String): List<String> {
        return mutationMutex.withLock {
            val userId = authTokenStorage.getCurrentUserId()
            val previousItems = localDataSource.getExcludedKeywordItems(userId)
            val updatedItems = localDataSource.addKeyword(
                keyword = keyword,
                userId = userId
            )
            val normalizedKeyword = keyword.trim()

            if (normalizedKeyword.isBlank() || userId == null) {
                return@withLock updatedItems.map { it.keyword }
            }

            val postResponse = excludedKeywordsApi.addExcludedKeyword(
                CreateExcludedKeywordRequest(keyword = normalizedKeyword)
            )
            if (!postResponse.isSuccessful) {
                restoreItemsIfCurrentUser(userId, previousItems)
                throw HttpException(postResponse)
            }

            runCatching {
                refreshExcludedKeywordsFromRemote(userId)
            }.getOrElse { error ->
                restoreItemsIfCurrentUser(userId, previousItems)
                throw error
            }
        }
    }

    suspend fun removeExcludedKeyword(keyword: String): List<String> {
        return mutationMutex.withLock {
            val normalizedKeyword = keyword.trim()
            val userId = authTokenStorage.getCurrentUserId()
            if (normalizedKeyword.isBlank()) {
                return@withLock localDataSource
                    .getExcludedKeywordItems(userId)
                    .map { it.keyword }
            }

            val previousItems = localDataSource.getExcludedKeywordItems(userId)
            val targetItem = previousItems.firstOrNull { it.keyword == normalizedKeyword }
                ?: return@withLock previousItems.map { it.keyword }
            val updatedItems = localDataSource.removeKeyword(
                keyword = normalizedKeyword,
                userId = userId
            )

            val excludedKeywordId = targetItem.id
            if (userId == null || excludedKeywordId == null) {
                return@withLock updatedItems.map { it.keyword }
            }

            val deleteResponse = excludedKeywordsApi.deleteExcludedKeyword(excludedKeywordId)
            if (!deleteResponse.isSuccessful) {
                restoreItemsIfCurrentUser(userId, previousItems)
                throw HttpException(deleteResponse)
            }

            runCatching {
                refreshExcludedKeywordsFromRemote(userId)
            }.getOrElse { error ->
                restoreItemsIfCurrentUser(userId, previousItems)
                throw error
            }
        }
    }

    private suspend fun restoreItemsIfCurrentUser(
        userId: Long,
        items: List<StoredExcludedKeywordItem>
    ) {
        if (authTokenStorage.getCurrentUserId() == userId) {
            localDataSource.replaceItems(items = items, userId = userId)
        }
    }

    private suspend fun refreshExcludedKeywordsFromRemote(userId: Long): List<String> {
        val response = excludedKeywordsApi.getExcludedKeywords()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val items = response.body()?.items.orEmpty().normalizeItems()
        val keywords = items.map { it.keyword }
        if (authTokenStorage.getCurrentUserId() == userId) {
            localDataSource.replaceItems(
                items = items.toStoredItems(),
                userId = userId
            )
        }
        return keywords
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