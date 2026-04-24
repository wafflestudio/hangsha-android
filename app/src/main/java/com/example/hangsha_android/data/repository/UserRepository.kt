package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.UserApi
import com.example.hangsha_android.data.network.model.UserProfileResponse
import javax.inject.Inject
import retrofit2.Response

class UserRepository @Inject constructor(
    private val userApi: UserApi
) {
    suspend fun getMyProfile(): Response<UserProfileResponse> {
        return userApi.getMyProfile()
    }
}
