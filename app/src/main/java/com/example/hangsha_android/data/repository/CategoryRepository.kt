package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.CategoryApi
import com.example.hangsha_android.data.network.model.CategoryItemResponse
import com.example.hangsha_android.data.network.model.CategoryItemsResponse
import com.example.hangsha_android.data.repository.model.CategoryItem
import com.example.hangsha_android.data.repository.model.CategoryKey
import com.example.hangsha_android.data.repository.model.CategoryType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
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
    private var hasLoadedEventStatuses = false
    private var hasLoadedEventTypes = false
    private var hasLoadedOrganizations = false
    private var lastCatalogLoadAttemptNanos: Long? = null

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

    private val _loadedCategoryTypes = MutableStateFlow<Set<CategoryType>>(emptySet())
    val loadedCategoryTypes: StateFlow<Set<CategoryType>> =
        _loadedCategoryTypes.asStateFlow()

    suspend fun ensureCategoryCatalogLoaded(forceRefresh: Boolean = false) {
        catalogLoadMutex.withLock {
            val shouldLoadStatuses = forceRefresh || !hasLoadedEventStatuses
            val shouldLoadTypes = forceRefresh || !hasLoadedEventTypes
            val shouldLoadOrganizations = forceRefresh || !hasLoadedOrganizations
            if (!shouldLoadStatuses && !shouldLoadTypes && !shouldLoadOrganizations) {
                return@withLock
            }

            val nowNanos = System.nanoTime()
            val lastAttemptNanos = lastCatalogLoadAttemptNanos
            if (
                !forceRefresh &&
                lastAttemptNanos != null &&
                nowNanos - lastAttemptNanos < RETRY_COOLDOWN_NANOS
            ) {
                return@withLock
            }
            lastCatalogLoadAttemptNanos = nowNanos

            runCatching {
                supervisorScope {
                    val statuses = async {
                        loadCategoryItems(shouldLoadStatuses) {
                            categoryApi.getEventStatuses()
                                .requireItems(CategoryType.EVENT_STATUS)
                        }
                    }
                    val types = async {
                        loadCategoryItems(shouldLoadTypes) {
                            categoryApi.getEventTypes()
                                .requireItems(CategoryType.EVENT_TYPE)
                        }
                    }
                    val organizations = async {
                        loadCategoryItems(shouldLoadOrganizations) {
                            categoryApi.getOrganizations()
                                .requireItems(CategoryType.ORGANIZATION)
                        }
                    }
                    Triple(statuses.await(), types.await(), organizations.await())
                }
            }.fold(
                onSuccess = { (statuses, types, organizations) ->
                    var didFail = false
                    statuses?.fold(
                        onSuccess = { items ->
                            _eventStatuses.value = items
                            _eventStatusNames.value = items.toNameMap()
                            hasLoadedEventStatuses = true
                            markCategoryTypeLoaded(CategoryType.EVENT_STATUS)
                        },
                        onFailure = { didFail = true }
                    )
                    types?.fold(
                        onSuccess = { items ->
                            _eventTypes.value = items
                            _eventTypeNames.value = items.toNameMap()
                            hasLoadedEventTypes = true
                            markCategoryTypeLoaded(CategoryType.EVENT_TYPE)
                        },
                        onFailure = { didFail = true }
                    )
                    organizations?.fold(
                        onSuccess = { items ->
                            _organizations.value = items
                            _organizationNames.value = items.toNameMap()
                            hasLoadedOrganizations = true
                            markCategoryTypeLoaded(CategoryType.ORGANIZATION)
                        },
                        onFailure = { didFail = true }
                    )
                    _catalogErrorMessage.value =
                        CATEGORY_CATALOG_ERROR_MESSAGE.takeIf { didFail }
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    _catalogErrorMessage.value = CATEGORY_CATALOG_ERROR_MESSAGE
                }
            )
        }
    }

    fun consumeCatalogError() {
        _catalogErrorMessage.value = null
    }

    private fun markCategoryTypeLoaded(type: CategoryType) {
        _loadedCategoryTypes.value = _loadedCategoryTypes.value + type
    }
}

private suspend inline fun <T> loadCategoryItems(
    shouldLoad: Boolean,
    crossinline loader: suspend () -> T
): Result<T>? {
    if (!shouldLoad) return null
    return try {
        Result.success(loader())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
}

private fun Response<CategoryItemsResponse>.requireItems(type: CategoryType): List<CategoryItem> {
    if (!isSuccessful) throw HttpException(this)
    val body = body() ?: error("Category response body is empty.")
    return (body.items ?: error("Category response items are missing."))
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

private const val RETRY_COOLDOWN_NANOS = 10_000_000_000L

private const val CATEGORY_CATALOG_ERROR_MESSAGE =
    "서버 연결이 원활하지 않습니다. 잠시 후 다시 시도해 주세요."
