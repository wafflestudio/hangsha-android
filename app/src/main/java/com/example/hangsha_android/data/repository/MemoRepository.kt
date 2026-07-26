package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.local.GuestMemosLocalDataSource
import com.example.hangsha_android.data.network.api.MemoApi
import com.example.hangsha_android.data.network.model.CreateMemoRequest
import com.example.hangsha_android.data.network.model.MemoListResponse
import com.example.hangsha_android.data.network.model.MemoResponse
import com.example.hangsha_android.data.network.model.UpdateMemoRequest
import javax.inject.Inject
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class MemoRepository @Inject constructor(
    private val memoApi: MemoApi,
    private val localDataSource: GuestMemosLocalDataSource,
    private val authTokenStorage: AuthTokenStorage
) {
    suspend fun getMemos(): Response<MemoListResponse> {
        if (!isLoggedIn()) {
            return Response.success(MemoListResponse(items = localDataSource.getMemos()))
        }

        return memoApi.getMemos()
    }

    suspend fun getMemoByEvent(
        eventId: Long
    ): Response<MemoResponse> {
        if (!isLoggedIn()) {
            return localDataSource.getMemoByEvent(eventId)?.let { memo ->
                Response.success(memo)
            } ?: notFoundResponse()
        }

        return memoApi.getMemoByEvent(eventId = eventId)
    }

    suspend fun createMemo(
        eventId: Long,
        content: String,
        tagNames: List<String>,
        eventTitle: String = ""
    ): Response<MemoResponse> {
        if (!isLoggedIn()) {
            return Response.success(
                localDataSource.createMemo(
                    eventId = eventId,
                    eventTitle = eventTitle,
                    content = content,
                    tagNames = tagNames
                )
            )
        }

        return memoApi.createMemo(
            request = CreateMemoRequest(
                eventId = eventId,
                content = content,
                tagNames = tagNames
            )
        )
    }

    suspend fun updateMemo(
        memoId: Long,
        content: String? = null,
        tagNames: List<String>? = null
    ): Response<MemoResponse> {
        if (!isLoggedIn()) {
            return localDataSource.updateMemo(
                memoId = memoId,
                content = content,
                tagNames = tagNames
            )?.let { memo ->
                Response.success(memo)
            } ?: notFoundResponse()
        }

        return memoApi.updateMemo(
            memoId = memoId,
            request = UpdateMemoRequest(
                content = content,
                tagNames = tagNames
            )
        )
    }

    suspend fun deleteMemo(
        memoId: Long
    ): Response<Unit> {
        if (!isLoggedIn()) {
            localDataSource.deleteMemo(memoId)
            return Response.success(Unit)
        }

        return memoApi.deleteMemo(memoId = memoId)
    }

    private fun isLoggedIn(): Boolean {
        return authTokenStorage.hasAccessToken()
    }

    private fun <T> notFoundResponse(): Response<T> {
        return Response.error(404, "".toResponseBody(null))
    }
}