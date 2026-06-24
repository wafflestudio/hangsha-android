package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.CreateMemoRequest
import com.example.hangsha_android.data.network.model.MemoListResponse
import com.example.hangsha_android.data.network.model.MemoResponse
import com.example.hangsha_android.data.network.model.UpdateMemoRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST

interface MemoApi {
    @GET("api/v1/memos")
    suspend fun getMemos(): Response<MemoListResponse>

    @POST("api/v1/memos")
    suspend fun createMemo(
        @Body request: CreateMemoRequest
    ): Response<MemoResponse>

    @PATCH("api/v1/memos/{memoId}")
    suspend fun updateMemo(
        @Path("memoId") memoId: Long,
        @Body request: UpdateMemoRequest
    ): Response<MemoResponse>

    @DELETE("api/v1/memos/{memoId}")
    suspend fun deleteMemo(
        @Path("memoId") memoId: Long
    ): Response<Unit>
}
