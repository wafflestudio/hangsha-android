package com.example.hangsha_android.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.legacyGuestMemosDataStore by preferencesDataStore(name = "guest_memos")
private val Context.legacyGuestTimetablesDataStore by preferencesDataStore(name = "guest_timetables")

@Singleton
class LegacyGuestDataCleaner @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    suspend fun clearAllData() {
        context.legacyGuestMemosDataStore.edit { preferences -> preferences.clear() }
        context.legacyGuestTimetablesDataStore.edit { preferences -> preferences.clear() }
    }
}
