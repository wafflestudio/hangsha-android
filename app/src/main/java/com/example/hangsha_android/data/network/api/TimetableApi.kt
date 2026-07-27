package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.CreateTimetableRequest
import com.example.hangsha_android.data.network.model.TimetableListResponse
import com.example.hangsha_android.data.network.model.TimetableResponse
import com.example.hangsha_android.data.network.model.UpdateTimetableRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TimetableApi {
    @GET("api/v1/timetables")
    suspend fun getTimetables(
        @Query("year") year: Int,
        @Query("semester") semester: String
    ): Response<TimetableListResponse>

    @PATCH("api/v1/timetables/{timetableId}")
    suspend fun updateTimetable(
        @Path("timetableId") timetableId: Long,
        @Body request: UpdateTimetableRequest
    ): Response<TimetableResponse>

    @DELETE("api/v1/timetables/{timetableId}")
    suspend fun deleteTimetable(
        @Path("timetableId") timetableId: Long
    ): Response<Unit>

    @POST("api/v1/timetables")
    suspend fun createTimetable(
        @Body request: CreateTimetableRequest
    ): Response<TimetableResponse>
}