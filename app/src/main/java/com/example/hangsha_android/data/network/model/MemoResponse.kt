package com.example.hangsha_android.data.network.model

import com.google.gson.annotations.SerializedName

data class MemoResponse(
    @SerializedName("id")
    val id: Long,
    @SerializedName("eventId")
    val eventId: Long,
    @SerializedName("eventTitle")
    val eventTitle: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("tags")
    val tags: List<MemoTagResponse>,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String
)

data class MemoTagResponse(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String
)
