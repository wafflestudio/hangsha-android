package com.example.hangsha_android.data.network.api

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface EventApi {
    @GET("api/v1/events/month")
    suspend fun getEvents(
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<JsonObject>
}
