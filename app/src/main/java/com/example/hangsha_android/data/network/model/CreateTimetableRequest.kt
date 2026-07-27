package com.example.hangsha_android.data.network.model

import com.google.gson.annotations.SerializedName

data class CreateTimetableRequest(
    @SerializedName("name")
    val name: String,
    @SerializedName("year")
    val year: Int,
    @SerializedName("semester")
    val semester: String
)
