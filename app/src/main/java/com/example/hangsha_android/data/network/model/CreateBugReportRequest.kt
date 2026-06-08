package com.example.hangsha_android.data.network.model

import com.google.gson.annotations.SerializedName

data class CreateBugReportRequest(
    @SerializedName("title")
    val title: String,
    @SerializedName("content")
    val content: String
)
