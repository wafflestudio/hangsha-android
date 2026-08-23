package com.example.hangsha_android.data.network.model

data class RegisterRequest(
    val email: String,
    val password: String,
    val signupToken: String,
    val username: String
)