package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.OrganizationCategoryResponse
import com.example.hangsha_android.data.network.model.UserProfileResponse
import retrofit2.Response
import retrofit2.http.GET

interface UserApi {
    @GET("api/v1/users/me")
    suspend fun getMyProfile(): Response<UserProfileResponse>

    @GET("api/v1/categories/orgs")
    suspend fun getOrganizationCategories(): Response<OrganizationCategoryResponse>
}
