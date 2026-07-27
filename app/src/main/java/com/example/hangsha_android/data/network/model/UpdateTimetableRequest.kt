package com.example.hangsha_android.data.network.model

import com.google.gson.annotations.SerializedName

data class UpdateTimetableRequest(
    @SerializedName("name")
    val name: String
)
