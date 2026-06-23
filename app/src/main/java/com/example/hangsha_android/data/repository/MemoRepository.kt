package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.MemoApi
import com.example.hangsha_android.data.network.model.CreateMemoRequest
import com.example.hangsha_android.data.network.model.MemoResponse
import javax.inject.Inject
import retrofit2.Response

class MemoRepository @Inject constructor(
    private val memoApi: MemoApi
) {
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
}
