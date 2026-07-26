package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.local.StoredGuestBookmarkSnapshot

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
import kotlinx.coroutines.flow.combine
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
            combine(
                authTokenStorage.isLoggedIn,
                localDataSource.bookmarkSets
            ) { isLoggedIn, bookmarkSets ->
                bookmarkSets.forAuthState(isLoggedIn)
            }.collectLatest { eventIds ->
                _bookmarkedEventIds.value = eventIds
            }
        }
    }

    fun currentBookmarkedEventIds(): Set<Long> = _bookmarkedEventIds.value

    fun isLoggedIn(): Boolean {
        return authTokenStorage.hasAccessToken()
    }

    suspend fun setBookmark(
        eventId: Long,
        isBookmarked: Boolean,
        guestSnapshot: StoredGuestBookmarkSnapshot? = null
    ): Set<Long> {
        return mutationMutex.withLock {
            val isLoggedIn = isLoggedIn()
            val previousEventIds = localDataSource.getBookmarkedEventIds(isLoggedIn)
            val updatedEventIds = localDataSource.setBookmarked(
                eventId = eventId,
                isBookmarked = isBookmarked,
                isLoggedIn = isLoggedIn,
                guestSnapshot = guestSnapshot
            )

            if (!isLoggedIn) {
                return@withLock updatedEventIds
            }

            val response = if (isBookmarked) {
                eventApi.createBookmark(eventId)
            } else {
                eventApi.deleteBookmark(eventId)
            }
            if (!response.isSuccessful) {
                localDataSource.replaceBookmarkedEventIds(
                    eventIds = previousEventIds,
                    isLoggedIn = true
                )
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
        if (!isLoggedIn()) {
            return localDataSource.getBookmarkedEventIds(isLoggedIn = false)
        }
        if (remoteBookmarks.isEmpty()) {
            return localDataSource.getBookmarkedEventIds(isLoggedIn = true)
        }

        return mutationMutex.withLock {
            val currentEventIds = localDataSource.getBookmarkedEventIds(isLoggedIn = true)
            val knownEventIds = remoteBookmarks.keys
            val remoteBookmarkedEventIds = remoteBookmarks
                .filterValues { it }
                .keys

            localDataSource.replaceBookmarkedEventIds(
                eventIds = (currentEventIds - knownEventIds) + remoteBookmarkedEventIds,
                isLoggedIn = true
            )
        }
    }
}