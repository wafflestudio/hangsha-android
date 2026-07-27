package com.example.hangsha_android.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.hangsha_android.data.network.model.TimetableListResponse
import com.example.hangsha_android.data.network.model.TimetableResponse
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

private val Context.guestTimetablesDataStore by preferencesDataStore(name = "guest_timetables")

@Singleton
class GuestTimetableLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val gson = Gson()

    val timetables: Flow<List<StoredGuestTimetable>> =
        context.guestTimetablesDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> parseTimetables(preferences[GUEST_TIMETABLES_JSON]) }

    suspend fun getTimetables(
        year: Int,
        semester: String
    ): TimetableListResponse {
        return TimetableListResponse(
            items = timetables.first()
                .filter { timetable -> timetable.year == year && timetable.semester == semester }
                .map { timetable -> timetable.toTimetableResponse() }
        )
    }

    suspend fun createTimetable(
        name: String,
        year: Int,
        semester: String
    ): TimetableResponse {
        val currentTimetables = timetables.first()
        val nextId = (currentTimetables.minOfOrNull { timetable -> timetable.id } ?: 0L) - 1L
        val timetable = StoredGuestTimetable(
            id = nextId,
            name = name.trim(),
            year = year,
            semester = semester
        )
        replaceAll(currentTimetables + timetable)
        return timetable.toTimetableResponse()
    }

    suspend fun updateTimetableName(
        timetableId: Long,
        name: String
    ): TimetableResponse {
        val currentTimetables = timetables.first()
        val target = currentTimetables.firstOrNull { timetable -> timetable.id == timetableId }
            ?: throw NoSuchElementException("Timetable was not found.")
        val updatedTimetable = target.copy(name = name.trim())
        replaceAll(currentTimetables.map { timetable ->
            if (timetable.id == timetableId) updatedTimetable else timetable
        })
        return updatedTimetable.toTimetableResponse()
    }
    suspend fun deleteTimetable(timetableId: Long) {
        replaceAll(timetables.first().filterNot { timetable -> timetable.id == timetableId })
    }
    private suspend fun replaceAll(items: List<StoredGuestTimetable>) {
        context.guestTimetablesDataStore.edit { preferences ->
            preferences[GUEST_TIMETABLES_JSON] = gson.toJson(
                items
                    .filter { timetable -> timetable.name.isNotBlank() }
                    .distinctBy { timetable -> timetable.id }
                    .sortedWith(compareBy({ timetable -> timetable.year }, { timetable -> timetable.semester }, { timetable -> timetable.id }))
            )
        }
    }

    private fun parseTimetables(json: String?): List<StoredGuestTimetable> {
        if (json.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching {
            gson.fromJson<List<StoredGuestTimetable>>(json, storedTimetableListType)
        }.getOrDefault(emptyList())
    }

    companion object {
        private val GUEST_TIMETABLES_JSON = stringPreferencesKey("guest_timetables_json")
        private val storedTimetableListType = object : TypeToken<List<StoredGuestTimetable>>() {}.type
    }
}

data class StoredGuestTimetable(
    val id: Long,
    val name: String,
    val year: Int,
    val semester: String
)

private fun StoredGuestTimetable.toTimetableResponse(): TimetableResponse {
    return TimetableResponse(
        id = id,
        name = name,
        year = year,
        semester = semester
    )
}
