package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.local.AuthTokenStorage
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
    private val authTokenStorage: AuthTokenStorage
) {
    suspend fun getMemos(): Response<MemoListResponse> {
        if (!isLoggedIn()) {
            return unauthorizedResponse()
        }

        return memoApi.getMemos()
    }

    suspend fun getMemoByEvent(
        eventId: Long
    ): Response<MemoResponse> {
        if (!isLoggedIn()) {
            return unauthorizedResponse()
        }

        return memoApi.getMemoByEvent(eventId = eventId)
    }

    suspend fun createMemo(
        eventId: Long,
        content: String,
        tagNames: List<String>
    ): Response<MemoResponse> {
        if (!isLoggedIn()) {
            return unauthorizedResponse()
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
            return unauthorizedResponse()
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
            return unauthorizedResponse()
        }

        return memoApi.deleteMemo(memoId = memoId)
    }

    private fun isLoggedIn(): Boolean {
        return authTokenStorage.hasAuthenticatedUser()
    }

    private fun <T> unauthorizedResponse(): Response<T> {
        return Response.error(401, "".toResponseBody(null))
    }
}
