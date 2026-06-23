package com.example.hangsha_android.data.network.model

import com.google.gson.annotations.SerializedName

data class UpdateMemoRequest(
    @SerializedName("content")
    val content: String
)
