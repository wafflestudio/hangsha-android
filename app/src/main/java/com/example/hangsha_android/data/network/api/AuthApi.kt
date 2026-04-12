package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.SocialLoginRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/login/social")
    suspend fun loginWithSocial(
        @Body request: SocialLoginRequest
    ): Response<ResponseBody>
}
