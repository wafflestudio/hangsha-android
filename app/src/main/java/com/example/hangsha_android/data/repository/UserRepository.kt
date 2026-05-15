package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.model.OrganizationCategoryResponse
import com.example.hangsha_android.data.network.api.UserApi
import com.example.hangsha_android.data.network.model.UserProfileResponse
import javax.inject.Singleton
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.HttpException
import retrofit2.Response

@Singleton
class UserRepository @Inject constructor(
    private val userApi: UserApi
) {
    private val _organizationNames = MutableStateFlow<Map<Long, String>>(emptyMap())
    val organizationNames: StateFlow<Map<Long, String>> = _organizationNames.asStateFlow()

    suspend fun getMyProfile(): Response<UserProfileResponse> {
        return userApi.getMyProfile()
    }

    suspend fun ensureOrganizationNamesLoaded(forceRefresh: Boolean = false) {
        if (!forceRefresh && _organizationNames.value.isNotEmpty()) {
            return
        }

        val response = userApi.getOrganizationCategories()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val mappedNames = response.body()
            .toOrganizationNameMap()

        _organizationNames.update { current ->
            if (!forceRefresh && current.isNotEmpty()) {
                current
            } else {
                mappedNames
            }
        }
    }
}

private fun OrganizationCategoryResponse?.toOrganizationNameMap(): Map<Long, String> {
    return this?.items
        .orEmpty()
        .sortedBy { item -> item.sortOrder }
        .associate { item -> item.id to item.name }
}
