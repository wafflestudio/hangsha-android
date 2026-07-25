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

    val bookmarkSets: Flow<StoredBookmarkSets> =
        context.bookmarksDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> preferences.toStoredBookmarkSets() }

    fun bookmarkedEventIds(isLoggedIn: Boolean): Flow<Set<Long>> {
        return bookmarkSets.map { sets -> sets.forAuthState(isLoggedIn) }
    }

    suspend fun getBookmarkedEventIds(isLoggedIn: Boolean): Set<Long> {
        return bookmarkedEventIds(isLoggedIn).first()
    }

    suspend fun replaceBookmarkedEventIds(
        eventIds: Set<Long>,
        isLoggedIn: Boolean
    ): Set<Long> {
        val normalizedEventIds = eventIds.filter { it > 0L }.toSortedSet()
        val key = bookmarkKey(isLoggedIn)
        context.bookmarksDataStore.edit { preferences ->
            preferences[key] = gson.toJson(normalizedEventIds)
        }
        return normalizedEventIds
    }

    suspend fun setBookmarked(
        eventId: Long,
        isBookmarked: Boolean,
        isLoggedIn: Boolean
    ): Set<Long> {
        if (eventId <= 0L) {
            return getBookmarkedEventIds(isLoggedIn)
        }

        val currentEventIds = getBookmarkedEventIds(isLoggedIn)
        return replaceBookmarkedEventIds(
            eventIds = if (isBookmarked) {
                currentEventIds + eventId
            } else {
                currentEventIds - eventId
            },
            isLoggedIn = isLoggedIn
        )
    }

    private fun Preferences.toStoredBookmarkSets(): StoredBookmarkSets {
        return StoredBookmarkSets(
            guestEventIds = parseEventIds(this[GUEST_BOOKMARKED_EVENT_IDS_JSON]),
            authenticatedEventIds = parseEventIds(this[AUTH_BOOKMARKED_EVENT_IDS_JSON])
        )
    }

    private fun parseEventIds(json: String?): Set<Long> {
        if (json.isNullOrBlank()) {
            return emptySet()
        }

        return runCatching {
            gson.fromJson<List<Long>>(json, eventIdListType).toSet()
        }.getOrDefault(emptySet())
    }

    private fun bookmarkKey(isLoggedIn: Boolean): Preferences.Key<String> {
        return if (isLoggedIn) {
            AUTH_BOOKMARKED_EVENT_IDS_JSON
        } else {
            GUEST_BOOKMARKED_EVENT_IDS_JSON
        }
    }

    companion object {
        private val GUEST_BOOKMARKED_EVENT_IDS_JSON =
            stringPreferencesKey("guest_bookmarked_event_ids_json")
        private val AUTH_BOOKMARKED_EVENT_IDS_JSON =
            stringPreferencesKey("authenticated_bookmarked_event_ids_json")
        private val eventIdListType = object : TypeToken<List<Long>>() {}.type
    }
}

data class StoredBookmarkSets(
    val guestEventIds: Set<Long>,
    val authenticatedEventIds: Set<Long>
) {
    fun forAuthState(isLoggedIn: Boolean): Set<Long> {
        return if (isLoggedIn) authenticatedEventIds else guestEventIds
    }
}