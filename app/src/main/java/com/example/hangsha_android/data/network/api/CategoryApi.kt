package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.CategoryItemsResponse
import retrofit2.Response
import retrofit2.http.GET

interface CategoryApi {
    @GET("api/v1/event-statuses")
    suspend fun getEventStatuses(): Response<CategoryItemsResponse>

    @GET("api/v1/event-types")
    suspend fun getEventTypes(): Response<CategoryItemsResponse>

    @GET("api/v1/organizations")
    suspend fun getOrganizations(): Response<CategoryItemsResponse>
}
