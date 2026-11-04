package com.example.hangsha_android.data.network.model

import com.google.gson.annotations.SerializedName

data class AppVersionResponse(
    @SerializedName("updateType")
    val updateType: AppUpdateType,
    @SerializedName("minimumSupportedVersionCode")
    val minimumSupportedVersionCode: Int,
    @SerializedName("latestVersionName")
    val latestVersionName: String,
    @SerializedName("releaseNotes")
    val releaseNotes: String
)

enum class AppUpdateType {
    @SerializedName("NONE")
    NONE,

    @SerializedName("OPTIONAL")
    OPTIONAL,

    @SerializedName("REQUIRED")
    REQUIRED
}
