package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.AuthApi
import com.example.hangsha_android.data.network.model.LoginRequest
import com.example.hangsha_android.data.network.model.LoginResponse
import com.example.hangsha_android.data.network.model.SocialLoginRequest
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

    suspend fun loginWithGoogle(serverAuthCode: String): Response<LoginResponse> {
        return authApi.loginWithSocial(
            SocialLoginRequest(
                provider = GOOGLE_PROVIDER,
                code = serverAuthCode,
                codeVerifier = null
            )
        )
    }

    companion object {
        private const val GOOGLE_PROVIDER = "GOOGLE"
    }
}
