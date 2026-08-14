package com.example.hangsha_android.data.network.model

import com.google.gson.annotations.SerializedName

data class SocialLoginRequest(
    @SerializedName("provider")
    val provider: String,
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("accessToken")
    val accessToken: String? = null,
    @SerializedName("codeVerifier")
    val codeVerifier: String? = null,
    @SerializedName("client_type")
    val clientType: String? = null
)