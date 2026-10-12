package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.AppVersionResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AppVersionApi {
    @GET("api/v1/app-version")
    suspend fun getAppVersion(
        @Query("platform") platform: String,
        @Query("currentVersionCode") currentVersionCode: Int,
        @Query("availableVersionCode") availableVersionCode: Int
    ): Response<AppVersionResponse>
}
