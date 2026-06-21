package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.CategoryApi
import com.example.hangsha_android.data.network.model.CategoryGroupsWithCategoriesResponse
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryApi: CategoryApi
) {
    suspend fun getCategoryGroupsWithCategories(): Response<CategoryGroupsWithCategoriesResponse> {
        return categoryApi.getCategoryGroupsWithCategories()
    }
}
