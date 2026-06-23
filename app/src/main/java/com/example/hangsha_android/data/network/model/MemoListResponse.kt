package com.example.hangsha_android.data.network.model

import com.google.gson.annotations.SerializedName

data class MemoListResponse(
    @SerializedName("items")
    val items: List<MemoResponse>
)
