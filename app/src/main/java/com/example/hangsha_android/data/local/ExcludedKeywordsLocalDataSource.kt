package com.example.hangsha_android.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.excludedKeywordsDataStore by preferencesDataStore(name = "excluded_keywords")

@Singleton
class ExcludedKeywordsLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val gson = Gson()

    fun excludedKeywordItems(userId: Long?): Flow<List<StoredExcludedKeywordItem>> {
        return context.excludedKeywordsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> parseItems(preferences[excludedKeywordsKey(userId)]) }
    }

    suspend fun getExcludedKeywordItems(userId: Long?): List<StoredExcludedKeywordItem> {
        return excludedKeywordItems(userId).first()
    }

    suspend fun replaceItems(
        items: List<StoredExcludedKeywordItem>,
        userId: Long?
    ): List<StoredExcludedKeywordItem> {
        val normalizedItems = items.normalizeItems()
        val key = excludedKeywordsKey(userId)
        context.excludedKeywordsDataStore.edit { preferences ->
            preferences[key] = gson.toJson(normalizedItems)
        }
        return normalizedItems
    }

    suspend fun addKeyword(
        keyword: String,
        userId: Long?
    ): List<StoredExcludedKeywordItem> {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) {
            return getExcludedKeywordItems(userId)
        }

        val currentItems = getExcludedKeywordItems(userId)
        if (currentItems.any { it.keyword == normalizedKeyword }) {
            return currentItems
        }

        return replaceItems(
            items = currentItems + StoredExcludedKeywordItem(
                id = null,
                keyword = normalizedKeyword,
                createdAt = null
            ),
            userId = userId
        )
    }

    suspend fun removeKeyword(
        keyword: String,
        userId: Long?
    ): List<StoredExcludedKeywordItem> {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) {
            return getExcludedKeywordItems(userId)
        }

        val currentItems = getExcludedKeywordItems(userId)
        return replaceItems(
            items = currentItems.filterNot { it.keyword == normalizedKeyword },
            userId = userId
        )
    }

    private fun parseItems(json: String?): List<StoredExcludedKeywordItem> {
        if (json.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching {
            gson.fromJson<List<StoredExcludedKeywordItem>>(json, storedItemListType)
        }.getOrDefault(emptyList()).normalizeItems()
    }

    suspend fun clearUserData(userId: Long) {
        context.excludedKeywordsDataStore.edit { preferences ->
            preferences.remove(excludedKeywordsKey(userId))
        }
    }

    suspend fun clearLegacyAuthenticatedData() {
        context.excludedKeywordsDataStore.edit { preferences ->
            preferences.remove(LEGACY_AUTH_EXCLUDED_KEYWORDS_JSON)
        }
    }

    suspend fun clearAllData() {
        context.excludedKeywordsDataStore.edit { preferences -> preferences.clear() }
    }

    private fun excludedKeywordsKey(userId: Long?): Preferences.Key<String> {
        return userId?.let { id ->
            require(id > 0L) { "User ID must be positive." }
            stringPreferencesKey("user_${id}_excluded_keywords_json")
        } ?: GUEST_EXCLUDED_KEYWORDS_JSON
    }

    companion object {
        private val GUEST_EXCLUDED_KEYWORDS_JSON =
            stringPreferencesKey("guest_excluded_keywords_json")
        private val LEGACY_AUTH_EXCLUDED_KEYWORDS_JSON =
            stringPreferencesKey("authenticated_excluded_keywords_json")
        private val storedItemListType =
            object : TypeToken<List<StoredExcludedKeywordItem>>() {}.type
    }
}

data class StoredExcludedKeywordItem(
    val id: Long?,
    val keyword: String,
    val createdAt: String?
)

private fun List<StoredExcludedKeywordItem>.normalizeItems(): List<StoredExcludedKeywordItem> {
    return mapNotNull { item ->
        val normalizedKeyword = item.keyword.trim()
        if (normalizedKeyword.isBlank()) {
            null
        } else {
            item.copy(keyword = normalizedKeyword)
        }
    }.distinctBy { it.id ?: "local:${it.keyword.lowercase()}" }
}
