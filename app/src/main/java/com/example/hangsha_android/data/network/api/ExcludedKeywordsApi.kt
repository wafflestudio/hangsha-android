package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.CreateExcludedKeywordRequest
import com.example.hangsha_android.data.network.model.ExcludedKeywordsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ExcludedKeywordsApi {
    @GET("api/v1/users/me/excluded-keywords")
    suspend fun getExcludedKeywords(): Response<ExcludedKeywordsResponse>

    @POST("api/v1/users/me/excluded-keywords")
    suspend fun addExcludedKeyword(
        @Body request: CreateExcludedKeywordRequest
    ): Response<Unit>
}
