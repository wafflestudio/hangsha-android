package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.LoginRequest
import com.example.hangsha_android.data.network.model.LoginResponse
import com.example.hangsha_android.data.network.model.RefreshTokenRequest
import com.example.hangsha_android.data.network.model.RegisterRequest
import com.example.hangsha_android.data.network.model.SocialLoginRequest
import com.example.hangsha_android.data.network.model.SocialLoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/mobile/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/v1/auth/login/social")
    suspend fun loginWithSocial(
        @Body request: SocialLoginRequest
    ): Response<SocialLoginResponse>

    @POST("api/v1/mobile/auth/session")
    suspend fun createMobileSession(
        @Header("Authorization") authorization: String
    ): Response<LoginResponse>

    @POST("api/v1/mobile/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<LoginResponse>

    @POST("api/v1/mobile/auth/refresh")
    suspend fun refresh(
        @Body request: RefreshTokenRequest
    ): Response<LoginResponse>

    @POST("api/v1/mobile/auth/logout")
    suspend fun logout(
        @Body request: RefreshTokenRequest
    ): Response<Unit>
}
