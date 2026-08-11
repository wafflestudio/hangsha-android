package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.CategoryApi
import com.example.hangsha_android.data.network.model.CategoryGroupWithCategoriesItemResponse
import com.example.hangsha_android.data.network.model.CategoryGroupsWithCategoriesResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import retrofit2.Response

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryApi: CategoryApi
) {
    private val catalogLoadMutex = Mutex()
    private var hasLoadedCatalog = false

    private val _categoryGroups =
        MutableStateFlow<List<CategoryGroupWithCategoriesItemResponse>>(emptyList())
    val categoryGroups: StateFlow<List<CategoryGroupWithCategoriesItemResponse>> =
        _categoryGroups.asStateFlow()

    private val _eventTypeNames = MutableStateFlow<Map<Long, String>>(emptyMap())
    val eventTypeNames: StateFlow<Map<Long, String>> = _eventTypeNames.asStateFlow()

    suspend fun getCategoryGroupsWithCategories(): Response<CategoryGroupsWithCategoriesResponse> {
        return categoryApi.getCategoryGroupsWithCategories()
    }

    suspend fun ensureCategoryCatalogLoaded(forceRefresh: Boolean = false) {
        catalogLoadMutex.withLock {
            if (!forceRefresh && hasLoadedCatalog) return@withLock

            val response = getCategoryGroupsWithCategories()
            if (!response.isSuccessful) throw HttpException(response)

            val groups = response.body()
                ?.items
                .orEmpty()
                .sortedBy { item -> item.group.sortOrder }
            _categoryGroups.value = groups
            _eventTypeNames.value = groups
                .firstOrNull { item -> item.group.id == EVENT_TYPE_GROUP_ID }
                ?.categories
                .orEmpty()
                .sortedBy { category -> category.sortOrder }
                .associate { category -> category.id to category.name }
            hasLoadedCatalog = true
        }
    }

    private companion object {
        const val EVENT_TYPE_GROUP_ID = 3L
    }
}