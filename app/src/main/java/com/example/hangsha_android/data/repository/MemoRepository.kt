package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.MemoApi
import com.example.hangsha_android.data.network.model.CreateMemoRequest
import com.example.hangsha_android.data.network.model.MemoListResponse
import com.example.hangsha_android.data.network.model.MemoResponse
import com.example.hangsha_android.data.network.model.UpdateMemoRequest
import javax.inject.Inject
import retrofit2.Response

class MemoRepository @Inject constructor(
    private val memoApi: MemoApi
) {
    suspend fun getMemos(): Response<MemoListResponse> {
        return memoApi.getMemos()
    }

    suspend fun createMemo(
        eventId: Long,
        content: String,
        tagNames: List<String>
    ): Response<MemoResponse> {
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
        content: String
    ): Response<MemoResponse> {
        return memoApi.updateMemo(
            memoId = memoId,
            request = UpdateMemoRequest(content = content)
        )
    }
}
