package com.example.hangsha_android.data.local

import android.content.Context
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
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    val excludedKeywordItems: Flow<List<StoredExcludedKeywordItem>> =
        context.excludedKeywordsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val json = preferences[EXCLUDED_KEYWORDS_JSON].orEmpty()
                if (json.isBlank()) {
                    emptyList()
                } else {
                    runCatching {
                        gson.fromJson<List<StoredExcludedKeywordItem>>(json, storedItemListType)
                    }.getOrDefault(emptyList())
                }
            }

    suspend fun replaceItems(items: List<StoredExcludedKeywordItem>): List<StoredExcludedKeywordItem> {
        val normalizedItems = items.normalizeItems()
        context.excludedKeywordsDataStore.edit { preferences ->
            preferences[EXCLUDED_KEYWORDS_JSON] = gson.toJson(normalizedItems)
        }
        return normalizedItems
    }

    suspend fun addKeyword(keyword: String): List<StoredExcludedKeywordItem> {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) {
            return excludedKeywordItems.first()
        }

        val currentItems = excludedKeywordItems.first()
        if (currentItems.any { it.keyword == normalizedKeyword }) {
            return currentItems
        }

        return replaceItems(
            currentItems + StoredExcludedKeywordItem(
                id = null,
                keyword = normalizedKeyword,
                createdAt = null
            )
        )
    }

    suspend fun removeKeyword(keyword: String): List<StoredExcludedKeywordItem> {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) {
            return excludedKeywordItems.first()
        }

        val currentItems = excludedKeywordItems.first()
        return replaceItems(currentItems.filterNot { it.keyword == normalizedKeyword })
    }

    companion object {
        private val EXCLUDED_KEYWORDS_JSON = stringPreferencesKey("excluded_keywords_json")
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
