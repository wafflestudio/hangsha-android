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

    val excludedKeywordSets: Flow<StoredExcludedKeywordSets> =
        context.excludedKeywordsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> preferences.toStoredExcludedKeywordSets() }

    fun excludedKeywordItems(isLoggedIn: Boolean): Flow<List<StoredExcludedKeywordItem>> {
        return excludedKeywordSets.map { sets -> sets.forAuthState(isLoggedIn) }
    }

    suspend fun getExcludedKeywordItems(isLoggedIn: Boolean): List<StoredExcludedKeywordItem> {
        return excludedKeywordItems(isLoggedIn).first()
    }

    suspend fun replaceItems(
        items: List<StoredExcludedKeywordItem>,
        isLoggedIn: Boolean
    ): List<StoredExcludedKeywordItem> {
        val normalizedItems = items.normalizeItems()
        val key = excludedKeywordsKey(isLoggedIn)
        context.excludedKeywordsDataStore.edit { preferences ->
            preferences[key] = gson.toJson(normalizedItems)
        }
        return normalizedItems
    }

    suspend fun addKeyword(
        keyword: String,
        isLoggedIn: Boolean
    ): List<StoredExcludedKeywordItem> {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) {
            return getExcludedKeywordItems(isLoggedIn)
        }

        val currentItems = getExcludedKeywordItems(isLoggedIn)
        if (currentItems.any { it.keyword == normalizedKeyword }) {
            return currentItems
        }

        return replaceItems(
            items = currentItems + StoredExcludedKeywordItem(
                id = null,
                keyword = normalizedKeyword,
                createdAt = null
            ),
            isLoggedIn = isLoggedIn
        )
    }

    suspend fun removeKeyword(
        keyword: String,
        isLoggedIn: Boolean
    ): List<StoredExcludedKeywordItem> {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isBlank()) {
            return getExcludedKeywordItems(isLoggedIn)
        }

        val currentItems = getExcludedKeywordItems(isLoggedIn)
        return replaceItems(
            items = currentItems.filterNot { it.keyword == normalizedKeyword },
            isLoggedIn = isLoggedIn
        )
    }

    private fun Preferences.toStoredExcludedKeywordSets(): StoredExcludedKeywordSets {
        return StoredExcludedKeywordSets(
            guestItems = parseItems(this[GUEST_EXCLUDED_KEYWORDS_JSON]),
            authenticatedItems = parseItems(this[AUTH_EXCLUDED_KEYWORDS_JSON])
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

    private fun excludedKeywordsKey(isLoggedIn: Boolean): Preferences.Key<String> {
        return if (isLoggedIn) {
            AUTH_EXCLUDED_KEYWORDS_JSON
        } else {
            GUEST_EXCLUDED_KEYWORDS_JSON
        }
    }

    companion object {
        private val GUEST_EXCLUDED_KEYWORDS_JSON =
            stringPreferencesKey("guest_excluded_keywords_json")
        private val AUTH_EXCLUDED_KEYWORDS_JSON =
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

data class StoredExcludedKeywordSets(
    val guestItems: List<StoredExcludedKeywordItem>,
    val authenticatedItems: List<StoredExcludedKeywordItem>
) {
    fun forAuthState(isLoggedIn: Boolean): List<StoredExcludedKeywordItem> {
        return if (isLoggedIn) authenticatedItems else guestItems
    }
}

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