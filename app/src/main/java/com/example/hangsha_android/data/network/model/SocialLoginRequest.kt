package com.example.hangsha_android.data.network.model

data class SocialLoginRequest(
    val provider: String,
    val code: String,
    val codeVerifier: String? = null
)
