package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.CategoryGroupsWithCategoriesResponse
import com.example.hangsha_android.data.network.model.OrganizationCategoryResponse
import retrofit2.Response
import retrofit2.http.GET

interface CategoryApi {
    @GET("api/v1/category-groups/with-categories")
    suspend fun getCategoryGroupsWithCategories(): Response<CategoryGroupsWithCategoriesResponse>

    @GET("api/v1/categories/orgs")
    suspend fun getOrganizationCategories(): Response<OrganizationCategoryResponse>
}
