package com.example.hangsha_android.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal fun Context.needsLocalDataReset(): Boolean {
    return getSharedPreferences(MIGRATION_PREFERENCES_NAME, Context.MODE_PRIVATE)
        .getInt(KEY_VERSION, 0) < CURRENT_VERSION
}

@Singleton
class LocalDataMigration @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bookmarksLocalDataSource: BookmarksLocalDataSource,
    private val excludedKeywordsLocalDataSource: ExcludedKeywordsLocalDataSource,
    private val legacyGuestDataCleaner: LegacyGuestDataCleaner
) {
    private val migrationMutex = Mutex()
    private val migrationPreferences by lazy {
        context.getSharedPreferences(MIGRATION_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    fun needsReset(): Boolean {
        return context.needsLocalDataReset()
    }

    suspend fun runIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        migrationMutex.withLock {
            if (!needsReset()) {
                return@withLock false
            }

            bookmarksLocalDataSource.clearAllData()
            excludedKeywordsLocalDataSource.clearAllData()
            legacyGuestDataCleaner.clearAllData()
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
            true
        }
    }

}

private const val MIGRATION_PREFERENCES_NAME = "local_data_migrations"
private const val KEY_VERSION = "category_api_migration_version"
private const val CURRENT_VERSION = 1
