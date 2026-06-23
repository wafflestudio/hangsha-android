package com.example.hangsha_android.data.network.model

import com.google.gson.annotations.SerializedName

data class CreateMemoRequest(
    @SerializedName("eventId")
    val eventId: Long,
    @SerializedName("content")
    val content: String,
    @SerializedName("tagNames")
    val tagNames: List<String>
)
