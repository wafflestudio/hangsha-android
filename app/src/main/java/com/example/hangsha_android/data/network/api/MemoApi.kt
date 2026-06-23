package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.CreateMemoRequest
import com.example.hangsha_android.data.network.model.MemoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MemoApi {
    @POST("api/v1/memos")
    suspend fun createMemo(
        @Body request: CreateMemoRequest
    ): Response<MemoResponse>
}
