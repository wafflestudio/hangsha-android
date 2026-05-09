package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.DayEventsResponse
import com.example.hangsha_android.data.network.model.MonthlyEventsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface EventApi {
    @GET("api/v1/events/month")
    suspend fun getEvents(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("bookmarkedOnly") bookmarkedOnly: Boolean? = null,
        @Query("interestedOnly") interestedOnly: Boolean? = null,
        @Query("orgIds") orgIds: List<Long>? = null,
        @Query("statusIds") statusIds: List<Long>? = null,
        @Query("eventTypeIds") eventTypeIds: List<Long>? = null,
        @Query("excludedKeywords") excludedKeywords: List<String>? = null
    ): Response<MonthlyEventsResponse>

    @GET("api/v1/events/day")
    suspend fun getDayEvents(
        @Query("date") date: String,
        @Query("statusId[]") statusIds: List<Long>
    ): Response<DayEventsResponse>
}
