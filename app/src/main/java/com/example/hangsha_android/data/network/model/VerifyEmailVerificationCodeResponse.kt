package com.example.hangsha_android.data.network.model

data class VerifyEmailVerificationCodeResponse(
    val signupToken: String,
    val expiresAt: String
)
