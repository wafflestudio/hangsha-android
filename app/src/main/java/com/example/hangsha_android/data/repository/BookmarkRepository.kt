package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.local.BookmarksLocalDataSource
import com.example.hangsha_android.data.network.api.BookmarkApi
import com.example.hangsha_android.data.network.api.EventApi
import com.example.hangsha_android.data.network.model.BookmarkedEventsResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

@Singleton
class BookmarkRepository @Inject constructor(
    private val eventApi: EventApi,
    private val bookmarkApi: BookmarkApi,
    private val localDataSource: BookmarksLocalDataSource,
    private val authTokenStorage: AuthTokenStorage
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutationMutex = Mutex()

    private val _bookmarkedEventIds = MutableStateFlow<Set<Long>>(emptySet())
    val bookmarkedEventIds: StateFlow<Set<Long>> = _bookmarkedEventIds.asStateFlow()

    init {
        repositoryScope.launch {
            localDataSource.bookmarkedEventIds.collectLatest { eventIds ->
                _bookmarkedEventIds.value = eventIds
            }
        }
    }

    fun currentBookmarkedEventIds(): Set<Long> = _bookmarkedEventIds.value

    fun isLoggedIn(): Boolean {
        return !authTokenStorage.getAccessToken().isNullOrBlank()
    }

    suspend fun setBookmark(
        eventId: Long,
        isBookmarked: Boolean
    ): Set<Long> {
        return mutationMutex.withLock {
            val previousEventIds = localDataSource.bookmarkedEventIds.first()
            val updatedEventIds = localDataSource.setBookmarked(eventId, isBookmarked)

            if (!isLoggedIn()) {
                return@withLock updatedEventIds
            }

            val response = if (isBookmarked) {
                eventApi.createBookmark(eventId)
            } else {
                eventApi.deleteBookmark(eventId)
            }
            if (!response.isSuccessful) {
                localDataSource.replaceBookmarkedEventIds(previousEventIds)
                throw HttpException(response)
            }

            updatedEventIds
        }
    }

    suspend fun getMyBookmarks(
        page: Int,
        size: Int
    ): retrofit2.Response<BookmarkedEventsResponse> {
        return bookmarkApi.getMyBookmarks(page = page, size = size)
    }

    suspend fun syncKnownRemoteBookmarks(
        remoteBookmarks: Map<Long, Boolean>
    ): Set<Long> {
        // FIXME: 북마크 목록 전용 API가 생기면, 화면에 보이는 이벤트만 갱신하지 말고 서버 전체 목록으로 local을 replace.
        if (!isLoggedIn() || remoteBookmarks.isEmpty()) {
            return localDataSource.bookmarkedEventIds.first()
        }

        return mutationMutex.withLock {
            val currentEventIds = localDataSource.bookmarkedEventIds.first()
            val knownEventIds = remoteBookmarks.keys
            val remoteBookmarkedEventIds = remoteBookmarks
                .filterValues { it }
                .keys

            localDataSource.replaceBookmarkedEventIds(
                (currentEventIds - knownEventIds) + remoteBookmarkedEventIds
            )
        }
    }
}
