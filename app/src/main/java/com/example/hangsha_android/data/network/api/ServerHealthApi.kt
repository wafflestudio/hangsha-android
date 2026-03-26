package com.example.hangsha_android.data.network.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET

interface ServerHealthApi {
    @GET("api/v1/health")
    suspend fun checkServer(): Response<ResponseBody>
}
