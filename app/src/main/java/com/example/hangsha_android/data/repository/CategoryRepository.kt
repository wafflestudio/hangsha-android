package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.CategoryApi
import com.example.hangsha_android.data.network.model.CategoryItemResponse
import com.example.hangsha_android.data.network.model.CategoryItemsResponse
import com.example.hangsha_android.data.repository.model.CategoryItem
import com.example.hangsha_android.data.repository.model.CategoryKey
import com.example.hangsha_android.data.repository.model.CategoryType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    private val _eventStatuses = MutableStateFlow<List<CategoryItem>>(emptyList())
    val eventStatuses: StateFlow<List<CategoryItem>> = _eventStatuses.asStateFlow()

    private val _eventTypes = MutableStateFlow<List<CategoryItem>>(emptyList())
    val eventTypes: StateFlow<List<CategoryItem>> = _eventTypes.asStateFlow()

    private val _organizations = MutableStateFlow<List<CategoryItem>>(emptyList())
    val organizations: StateFlow<List<CategoryItem>> = _organizations.asStateFlow()

    private val _eventStatusNames = MutableStateFlow<Map<Long, String>>(emptyMap())
    val eventStatusNames: StateFlow<Map<Long, String>> = _eventStatusNames.asStateFlow()

    private val _eventTypeNames = MutableStateFlow<Map<Long, String>>(emptyMap())
    val eventTypeNames: StateFlow<Map<Long, String>> = _eventTypeNames.asStateFlow()

    private val _organizationNames = MutableStateFlow<Map<Long, String>>(emptyMap())
    val organizationNames: StateFlow<Map<Long, String>> = _organizationNames.asStateFlow()

    private val _catalogErrorMessage = MutableStateFlow<String?>(null)
    val catalogErrorMessage: StateFlow<String?> = _catalogErrorMessage.asStateFlow()

    suspend fun ensureCategoryCatalogLoaded(forceRefresh: Boolean = false) {
        catalogLoadMutex.withLock {
            if (!forceRefresh && hasLoadedCatalog) return@withLock

            runCatching {
                coroutineScope {
                    val statuses = async {
                        categoryApi.getEventStatuses().requireItems(CategoryType.EVENT_STATUS)
                    }
                    val types = async {
                        categoryApi.getEventTypes().requireItems(CategoryType.EVENT_TYPE)
                    }
                    val organizations = async {
                        categoryApi.getOrganizations().requireItems(CategoryType.ORGANIZATION)
                    }
                    Triple(statuses.await(), types.await(), organizations.await())
                }
            }.fold(
                onSuccess = { (statuses, types, organizations) ->
                    _eventStatuses.value = statuses
                    _eventTypes.value = types
                    _organizations.value = organizations
                    _eventStatusNames.value = statuses.toNameMap()
                    _eventTypeNames.value = types.toNameMap()
                    _organizationNames.value = organizations.toNameMap()
                    _catalogErrorMessage.value = null
                    hasLoadedCatalog = true
                },
                onFailure = { error ->
                    _catalogErrorMessage.value = CATEGORY_CATALOG_ERROR_MESSAGE
                    throw error
                }
            )
        }
    }

    fun consumeCatalogError() {
        _catalogErrorMessage.value = null
    }
}

private fun Response<CategoryItemsResponse>.requireItems(type: CategoryType): List<CategoryItem> {
    if (!isSuccessful) throw HttpException(this)
    val body = body() ?: error("Category response body is empty.")
    return body.items
        .orEmpty()
        .sortedWith(compareBy<CategoryItemResponse> { it.sortOrder }.thenBy { it.id })
        .map { item ->
            CategoryItem(
                key = CategoryKey(type = type, id = item.id),
                name = item.name,
                sortOrder = item.sortOrder
            )
        }
}

private fun List<CategoryItem>.toNameMap(): Map<Long, String> {
    return associate { item -> item.key.id to item.name }
}

private const val CATEGORY_CATALOG_ERROR_MESSAGE =
    "서버 연결이 원활하지 않습니다. 잠시 후 다시 시도해 주세요."
