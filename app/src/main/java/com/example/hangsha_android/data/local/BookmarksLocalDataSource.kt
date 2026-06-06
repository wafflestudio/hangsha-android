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

private val Context.bookmarksDataStore by preferencesDataStore(name = "bookmarks")

@Singleton
class BookmarksLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val gson = Gson()

    val bookmarkedEventIds: Flow<Set<Long>> =
        context.bookmarksDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val json = preferences[BOOKMARKED_EVENT_IDS_JSON].orEmpty()
                if (json.isBlank()) {
                    emptySet()
                } else {
                    runCatching {
                        gson.fromJson<List<Long>>(json, eventIdListType).toSet()
                    }.getOrDefault(emptySet())
                }
            }

    suspend fun replaceBookmarkedEventIds(eventIds: Set<Long>): Set<Long> {
        val normalizedEventIds = eventIds.filter { it > 0L }.toSortedSet()
        context.bookmarksDataStore.edit { preferences ->
            preferences[BOOKMARKED_EVENT_IDS_JSON] = gson.toJson(normalizedEventIds)
        }
        return normalizedEventIds
    }

    suspend fun setBookmarked(
        eventId: Long,
        isBookmarked: Boolean
    ): Set<Long> {
        if (eventId <= 0L) {
            return bookmarkedEventIds.first()
        }

        val currentEventIds = bookmarkedEventIds.first()
        return replaceBookmarkedEventIds(
            if (isBookmarked) {
                currentEventIds + eventId
            } else {
                currentEventIds - eventId
            }
        )
    }

    companion object {
        private val BOOKMARKED_EVENT_IDS_JSON = stringPreferencesKey("bookmarked_event_ids_json")
        private val eventIdListType = object : TypeToken<List<Long>>() {}.type
    }
}
