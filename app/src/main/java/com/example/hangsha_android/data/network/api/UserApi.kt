package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.ProfileImageUploadResponse
import com.example.hangsha_android.data.network.model.UpdateInterestCategoriesRequest
import com.example.hangsha_android.data.network.model.UserInterestCategoriesResponse
import com.example.hangsha_android.data.network.model.UserProfileResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PUT

interface UserApi {
    @GET("api/v1/users/me")
    suspend fun getMyProfile(): Response<UserProfileResponse>

    @GET("api/v1/users/me/interest-categories")
    suspend fun getMyInterestCategories(): Response<UserInterestCategoriesResponse>

    @DELETE("api/v1/users/me")
    suspend fun deleteMyAccount(): Response<Unit>

    @PATCH("api/v1/users/me")
    suspend fun updateMyProfile(
        @Body request: RequestBody
    ): Response<UserProfileResponse>

    @Multipart
    @POST("api/v1/users/me/profile-image")
    suspend fun uploadMyProfileImage(
        @Part file: MultipartBody.Part
    ): Response<ProfileImageUploadResponse>

    @PUT("api/v1/users/me/interest-categories")
    suspend fun updateMyInterestCategories(
        @Body request: UpdateInterestCategoriesRequest
    ): Response<Unit>
}
