package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.CreateBugReportRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface BugReportApi {
    @POST("api/v1/bug-reports")
    suspend fun createBugReport(
        @Body request: CreateBugReportRequest
    ): Response<Unit>
}
