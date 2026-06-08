package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.BugReportApi
import com.example.hangsha_android.data.network.model.CreateBugReportRequest
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class BugReportRepository @Inject constructor(
    private val bugReportApi: BugReportApi
) {
    suspend fun createBugReport(
        title: String,
        content: String
    ): Response<Unit> {
        return bugReportApi.createBugReport(
            CreateBugReportRequest(
                title = title.trim(),
                content = content.trim()
            )
        )
    }
}
