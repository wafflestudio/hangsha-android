package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.AuthApi
import com.example.hangsha_android.data.network.model.LoginRequest
import com.example.hangsha_android.data.network.model.LoginResponse
import com.example.hangsha_android.data.network.model.RefreshTokenRequest
import com.example.hangsha_android.data.network.model.RegisterRequest
import com.example.hangsha_android.data.network.model.SocialLoginRequest
import com.example.hangsha_android.data.network.model.SocialLoginResponse
import javax.inject.Inject
import retrofit2.Response

class AuthRepository @Inject constructor(
    private val authApi: AuthApi
) {
    suspend fun login(email: String, password: String): Response<LoginResponse> {
        return authApi.login(
            LoginRequest(
                email = email,
                password = password
            )
        )
    }

    suspend fun loginWithGoogle(serverAuthCode: String): Response<SocialLoginResponse> {
        return authApi.loginWithSocial(
            SocialLoginRequest(
                provider = GOOGLE_PROVIDER,
                code = serverAuthCode,
                codeVerifier = null
            )
        )
    }

    suspend fun createMobileSession(accessToken: String): Response<LoginResponse> {
        return authApi.createMobileSession(
            authorization = "Bearer $accessToken"
        )
    }

    suspend fun register(
        email: String,
        password: String,
        username: String
    ): Response<LoginResponse> {
        return authApi.register(
            RegisterRequest(
                email = email,
                password = password,
                username = username
            )
        )
    }

    suspend fun logout(refreshToken: String): Response<Unit> {
        return authApi.logout(
            RefreshTokenRequest(refreshToken = refreshToken)
        )
    }

    suspend fun refresh(refreshToken: String): Response<LoginResponse> {
        return authApi.refresh(
            RefreshTokenRequest(refreshToken = refreshToken)
        )
    }

    companion object {
        private const val GOOGLE_PROVIDER = "GOOGLE"
    }
}
