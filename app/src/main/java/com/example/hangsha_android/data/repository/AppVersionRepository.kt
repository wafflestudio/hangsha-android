package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.AppVersionApi
import com.example.hangsha_android.data.network.model.AppVersionResponse
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class AppVersionRepository @Inject constructor(
    private val appVersionApi: AppVersionApi
) {
    suspend fun getAppVersion(
        currentVersionCode: Int,
        availableVersionCode: Int
    ): AppVersionResponse {
        val response = appVersionApi.getAppVersion(
            platform = ANDROID_PLATFORM,
            currentVersionCode = currentVersionCode,
            availableVersionCode = availableVersionCode
        )
        if (!response.isSuccessful) throw HttpException(response)
        return requireNotNull(response.body()) { "App version response body is empty." }
    }

    private companion object {
        const val ANDROID_PLATFORM = "android"
    }
}
