package com.example.hangsha_android.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class LocalDataMigration @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bookmarksLocalDataSource: BookmarksLocalDataSource,
    private val excludedKeywordsLocalDataSource: ExcludedKeywordsLocalDataSource,
    private val guestMemosLocalDataSource: GuestMemosLocalDataSource,
    private val guestTimetableLocalDataSource: GuestTimetableLocalDataSource
) {
    private val migrationMutex = Mutex()
    private val migrationPreferences by lazy {
        context.getSharedPreferences(MIGRATION_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    @Volatile
    var didResetThisLaunch: Boolean = false
        private set

    suspend fun runIfNeeded(): Boolean = migrationMutex.withLock {
        if (migrationPreferences.getInt(KEY_VERSION, 0) >= CURRENT_VERSION) {
            return@withLock false
        }

        bookmarksLocalDataSource.clearAllData()
        excludedKeywordsLocalDataSource.clearAllData()
        guestMemosLocalDataSource.clearAllData()
        guestTimetableLocalDataSource.clearAllData()
        context.cacheDir.listFiles().orEmpty().forEach { cachedFile ->
            cachedFile.deleteRecursively()
        }

        check(
            migrationPreferences.edit()
                .putInt(KEY_VERSION, CURRENT_VERSION)
                .commit()
        ) {
            "Failed to record local data migration."
        }
        didResetThisLaunch = true
        true
    }

    private companion object {
        const val MIGRATION_PREFERENCES_NAME = "local_data_migrations"
        const val KEY_VERSION = "category_api_migration_version"
        const val CURRENT_VERSION = 1
    }
}
