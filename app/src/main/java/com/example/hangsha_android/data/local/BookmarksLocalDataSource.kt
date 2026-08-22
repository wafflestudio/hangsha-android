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

private val Context.bookmarksDataStore by preferencesDataStore(name = "bookmarks")

@Singleton
class BookmarksLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val gson = Gson()

    fun bookmarkedEventIds(userId: Long): Flow<Set<Long>> {
        return context.bookmarksDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> parseEventIds(preferences[bookmarkKey(userId)]) }
    }

    suspend fun getBookmarkedEventIds(userId: Long): Set<Long> {
        return bookmarkedEventIds(userId).first()
    }

    suspend fun replaceBookmarkedEventIds(
        eventIds: Set<Long>,
        userId: Long
    ): Set<Long> {
        val normalizedEventIds = eventIds.filter { it > 0L }.toSortedSet()
        val key = bookmarkKey(userId)
        context.bookmarksDataStore.edit { preferences ->
            preferences[key] = gson.toJson(normalizedEventIds)
        }
        return normalizedEventIds
    }

    suspend fun setBookmarked(
        eventId: Long,
        isBookmarked: Boolean,
        userId: Long
    ): Set<Long> {
        if (eventId <= 0L) {
            return getBookmarkedEventIds(userId)
        }

        val currentEventIds = getBookmarkedEventIds(userId)
        val updatedEventIds = replaceBookmarkedEventIds(
            eventIds = if (isBookmarked) {
                currentEventIds + eventId
            } else {
                currentEventIds - eventId
            },
            userId = userId
        )

        return updatedEventIds
    }

    private fun parseEventIds(json: String?): Set<Long> {
        if (json.isNullOrBlank()) {
            return emptySet()
        }

        return runCatching {
            gson.fromJson<List<Long>>(json, eventIdListType).toSet()
        }.getOrDefault(emptySet())
    }

    suspend fun clearUserData(userId: Long) {
        context.bookmarksDataStore.edit { preferences ->
            preferences.remove(bookmarkKey(userId))
        }
    }

    suspend fun clearLegacyAuthenticatedData() {
        context.bookmarksDataStore.edit { preferences ->
            preferences.remove(LEGACY_AUTH_BOOKMARKED_EVENT_IDS_JSON)
        }
    }

    suspend fun clearAllData() {
        context.bookmarksDataStore.edit { preferences -> preferences.clear() }
    }

    private fun bookmarkKey(userId: Long): Preferences.Key<String> {
        require(userId > 0L) { "User ID must be positive." }
        return stringPreferencesKey("user_${userId}_bookmarked_event_ids_json")
    }

    companion object {
        private val LEGACY_AUTH_BOOKMARKED_EVENT_IDS_JSON =
            stringPreferencesKey("authenticated_bookmarked_event_ids_json")
        private val eventIdListType = object : TypeToken<List<Long>>() {}.type
    }
}
