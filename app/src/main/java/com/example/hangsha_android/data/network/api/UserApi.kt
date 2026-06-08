package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.OrganizationCategoryResponse
import com.example.hangsha_android.data.network.model.ProfileImageUploadResponse
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

interface UserApi {
    @GET("api/v1/users/me")
    suspend fun getMyProfile(): Response<UserProfileResponse>

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

    // 엄밀히 말하면 여기 있을 친구는 아니긴 한데... 얘 하나 때문에 api 만드는게 좀 귀찮기도 하고 복잡해질 거 같아서
    // 여기에 두었습니다. 추후 수정해주셔도 됩니다. FIXME
    @GET("api/v1/categories/orgs")
    suspend fun getOrganizationCategories(): Response<OrganizationCategoryResponse>
}
