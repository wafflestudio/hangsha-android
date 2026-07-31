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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

@OptIn(ExperimentalCoroutinesApi::class)
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
            authTokenStorage.currentUserId
                .flatMapLatest { userId -> localDataSource.bookmarkedEventIds(userId) }
                .collectLatest { eventIds ->
                    _bookmarkedEventIds.value = eventIds
                }
        }
    }

    fun currentBookmarkedEventIds(): Set<Long> = _bookmarkedEventIds.value

    fun currentUserId(): Long? = authTokenStorage.getCurrentUserId()

    fun isLoggedIn(): Boolean = authTokenStorage.hasAuthenticatedUser()

    suspend fun setBookmark(
        eventId: Long,
        isBookmarked: Boolean,
        guestSnapshot: StoredGuestBookmarkSnapshot? = null
    ): Set<Long> {
        return mutationMutex.withLock {
            val userId = authTokenStorage.getCurrentUserId()
            val previousEventIds = localDataSource.getBookmarkedEventIds(userId)
            val updatedEventIds = localDataSource.setBookmarked(
                eventId = eventId,
                isBookmarked = isBookmarked,
                userId = userId,
                guestSnapshot = guestSnapshot
            )

            if (userId == null) {
                return@withLock updatedEventIds
            }

            val response = if (isBookmarked) {
                eventApi.createBookmark(eventId)
            } else {
                eventApi.deleteBookmark(eventId)
            }
            if (!response.isSuccessful) {
                if (authTokenStorage.getCurrentUserId() == userId) {
                    localDataSource.replaceBookmarkedEventIds(
                        eventIds = previousEventIds,
                        userId = userId
                    )
                }
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
        remoteBookmarks: Map<Long, Boolean>,
        sourceUserId: Long? = authTokenStorage.getCurrentUserId()
    ): Set<Long> {
        val userId = sourceUserId
            ?: return localDataSource.getBookmarkedEventIds(userId = null)
        if (authTokenStorage.getCurrentUserId() != userId) {
            return localDataSource.getBookmarkedEventIds(authTokenStorage.getCurrentUserId())
        }
        if (remoteBookmarks.isEmpty()) {
            return localDataSource.getBookmarkedEventIds(userId)
        }

        return mutationMutex.withLock {
            if (authTokenStorage.getCurrentUserId() != userId) {
                return@withLock localDataSource.getBookmarkedEventIds(
                    authTokenStorage.getCurrentUserId()
                )
            }

            val currentEventIds = localDataSource.getBookmarkedEventIds(userId)
            val knownEventIds = remoteBookmarks.keys
            val remoteBookmarkedEventIds = remoteBookmarks
                .filterValues { it }
                .keys

            localDataSource.replaceBookmarkedEventIds(
                eventIds = (currentEventIds - knownEventIds) + remoteBookmarkedEventIds,
                userId = userId
            )
        }
    }
}