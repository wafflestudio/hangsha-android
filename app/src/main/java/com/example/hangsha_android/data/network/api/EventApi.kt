package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.DayEventsResponse
import com.example.hangsha_android.data.network.model.EventDetailResponse
import com.example.hangsha_android.data.network.model.MonthlyEventsResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface EventApi {
    @GET("api/v1/events/month")
    suspend fun getEvents(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("bookmarkedOnly") bookmarkedOnly: Boolean? = null,
        @Query("interestedOnly") interestedOnly: Boolean? = null,
        @Query("orgId") orgId: List<Long>? = null,
        @Query("statusId") statusId: List<Long>? = null,
        @Query("eventTypeId") eventTypeId: List<Long>? = null,
        @Query("excludedKeywords") excludedKeywords: List<String>? = null
    ): Response<MonthlyEventsResponse>

    @GET("api/v1/events/day")
    suspend fun getDayEvents(
        @Query("date") date: String,
        @Query("bookmarkedOnly") bookmarkedOnly: Boolean? = null,
        @Query("interestedOnly") interestedOnly: Boolean? = null,
        @Query("orgId") orgId: List<Long>? = null,
        @Query("statusId") statusId: List<Long>? = null,
        @Query("eventTypeId") eventTypeId: List<Long>? = null,
        @Query("excludedKeywords") excludedKeywords: List<String>? = null
    ): Response<DayEventsResponse>

    @GET("api/v1/events/{eventId}")
    suspend fun getEventDetail(
        @Path("eventId") eventId: Long
    ): Response<EventDetailResponse>

    @POST("api/v1/events/{eventId}/bookmark")
    suspend fun createBookmark(
        @Path("eventId") eventId: Long
    ): Response<Unit>

    @DELETE("api/v1/events/{eventId}/bookmark")
    suspend fun deleteBookmark(
        @Path("eventId") eventId: Long
    ): Response<Unit>
}
