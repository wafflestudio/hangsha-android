package com.example.hangsha_android.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.hangsha_android.data.network.model.MemoResponse
import com.example.hangsha_android.data.network.model.MemoTagResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.guestMemosDataStore by preferencesDataStore(name = "guest_memos")

@Singleton
class GuestMemosLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val gson = Gson()

    val memos: Flow<List<StoredGuestMemo>> =
        context.guestMemosDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> parseMemos(preferences[GUEST_MEMOS_JSON]) }

    suspend fun getMemos(): List<MemoResponse> {
        return memos.first().map { it.toMemoResponse() }
    }

    suspend fun getMemoByEvent(eventId: Long): MemoResponse? {
        return memos.first()
            .firstOrNull { it.eventId == eventId }
            ?.toMemoResponse()
    }

    suspend fun createMemo(
        eventId: Long,
        eventTitle: String,
        content: String,
        tagNames: List<String>
    ): MemoResponse {
        val now = OffsetDateTime.now().toString()
        val memo = StoredGuestMemo(
            id = eventId,
            eventId = eventId,
            eventTitle = eventTitle,
            content = content,
            tagNames = tagNames.normalizeTags(),
            createdAt = now,
            updatedAt = now
        )
        replaceStoredMemo(memo)
        return memo.toMemoResponse()
    }

    suspend fun updateMemo(
        memoId: Long,
        content: String?,
        tagNames: List<String>?
    ): MemoResponse? {
        val currentMemos = memos.first()
        val target = currentMemos.firstOrNull { it.id == memoId } ?: return null
        val updatedMemo = target.copy(
            content = content ?: target.content,
            tagNames = tagNames?.normalizeTags() ?: target.tagNames,
            updatedAt = OffsetDateTime.now().toString()
        )
        replaceAll(currentMemos.map { if (it.id == memoId) updatedMemo else it })
        return updatedMemo.toMemoResponse()
    }

    suspend fun deleteMemo(memoId: Long) {
        replaceAll(memos.first().filterNot { it.id == memoId })
    }

    suspend fun clearAllData() {
        context.guestMemosDataStore.edit { preferences -> preferences.clear() }
    }

    private suspend fun replaceStoredMemo(memo: StoredGuestMemo) {
        val updatedMemos = memos.first()
            .filterNot { it.id == memo.id || it.eventId == memo.eventId } + memo
        replaceAll(updatedMemos)
    }

    private suspend fun replaceAll(items: List<StoredGuestMemo>) {
        context.guestMemosDataStore.edit { preferences ->
            preferences[GUEST_MEMOS_JSON] = gson.toJson(
                items.filter { it.eventId > 0L }
                    .distinctBy { it.eventId }
                    .sortedByDescending { it.updatedAt }
            )
        }
    }

    private fun parseMemos(json: String?): List<StoredGuestMemo> {
        if (json.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching {
            gson.fromJson<List<StoredGuestMemo>>(json, storedMemoListType)
        }.getOrDefault(emptyList())
    }

    companion object {
        private val GUEST_MEMOS_JSON = stringPreferencesKey("guest_memos_json")
        private val storedMemoListType = object : TypeToken<List<StoredGuestMemo>>() {}.type
    }
}

data class StoredGuestMemo(
    val id: Long,
    val eventId: Long,
    val eventTitle: String,
    val content: String,
    val tagNames: List<String>,
    val createdAt: String,
    val updatedAt: String
)

private fun StoredGuestMemo.toMemoResponse(): MemoResponse {
    return MemoResponse(
        id = id,
        eventId = eventId,
        eventTitle = eventTitle,
        content = content,
        tags = tagNames.mapIndexed { index, name ->
            MemoTagResponse(id = id * 1000 + index, name = name)
        },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun List<String>.normalizeTags(): List<String> {
    return map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}
