package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.LoginRequest
import com.example.hangsha_android.data.network.model.LoginResponse
import com.example.hangsha_android.data.network.model.RegisterRequest
import com.example.hangsha_android.data.network.model.SocialLoginRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/v1/auth/login/social")
    suspend fun loginWithSocial(
        @Body request: SocialLoginRequest
    ): Response<LoginResponse>

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<LoginResponse>
}
