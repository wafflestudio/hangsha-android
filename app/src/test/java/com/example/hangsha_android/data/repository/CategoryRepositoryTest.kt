package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.CategoryApi
import com.example.hangsha_android.data.network.model.CategoryItemResponse
import com.example.hangsha_android.data.network.model.CategoryItemsResponse
import com.example.hangsha_android.data.repository.model.CategoryType
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response

class CategoryRepositoryTest {

    @Test
    fun preservesSuccessfulCatalogsAndCanRetryFailedCatalog() = runBlocking {
        val api = FakeCategoryApi()
        val repository = CategoryRepository(api)

        repository.ensureCategoryCatalogLoaded()

        assertEquals(listOf(1L, 2L), repository.eventStatuses.value.map { it.key.id })
        assertEquals(listOf(7L), repository.organizations.value.map { it.key.id })
        assertEquals(emptyList<Long>(), repository.eventTypes.value.map { it.key.id })
        assertEquals(
            setOf(CategoryType.EVENT_STATUS, CategoryType.ORGANIZATION),
            repository.loadedCategoryTypes.value
        )
        assertNotNull(repository.catalogErrorMessage.value)

        api.failEventTypes = false
        repository.ensureCategoryCatalogLoaded(forceRefresh = true)

        assertEquals(listOf(3L), repository.eventTypes.value.map { it.key.id })
        assertEquals(CategoryType.entries.toSet(), repository.loadedCategoryTypes.value)
        assertNull(repository.catalogErrorMessage.value)
    }

    private class FakeCategoryApi : CategoryApi {
        var failEventTypes = true

        override suspend fun getEventStatuses(): Response<CategoryItemsResponse> {
            return response(
                CategoryItemResponse(id = 2L, name = "모집중", sortOrder = 2),
                CategoryItemResponse(id = 1L, name = "모집대기", sortOrder = 1)
            )
        }

        override suspend fun getEventTypes(): Response<CategoryItemsResponse> {
            if (failEventTypes) throw IOException("temporary failure")
            return response(
                CategoryItemResponse(id = 3L, name = "현장학습/인턴", sortOrder = 3)
            )
        }

        override suspend fun getOrganizations(): Response<CategoryItemsResponse> {
            return response(
                CategoryItemResponse(id = 7L, name = "학생처", sortOrder = 7)
            )
        }

        private fun response(
            vararg items: CategoryItemResponse
        ): Response<CategoryItemsResponse> {
            return Response.success(CategoryItemsResponse(items.toList()))
        }
    }
}
