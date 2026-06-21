package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.CategoryApi
import com.example.hangsha_android.data.network.model.OrganizationCategoryResponse
import com.example.hangsha_android.data.network.api.UserApi
import com.example.hangsha_android.data.network.model.ProfileImageUploadResponse
import com.example.hangsha_android.data.network.model.UpdateUserProfileRequest
import com.example.hangsha_android.data.network.model.UserProfileResponse
import java.io.File
import javax.inject.Singleton
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.HttpException
import retrofit2.Response

@Singleton
class UserRepository @Inject constructor(
    private val userApi: UserApi,
    private val categoryApi: CategoryApi
) {
    private val _organizationNames = MutableStateFlow<Map<Long, String>>(emptyMap())
    val organizationNames: StateFlow<Map<Long, String>> = _organizationNames.asStateFlow()

    suspend fun getMyProfile(): Response<UserProfileResponse> {
        return userApi.getMyProfile()
    }

    suspend fun deleteMyAccount(): Response<Unit> {
        return userApi.deleteMyAccount()
    }

    suspend fun uploadMyProfileImage(
        imageFile: File,
        mimeType: String?
    ): Response<ProfileImageUploadResponse> {
        val requestBody = imageFile.asRequestBody(mimeType?.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = imageFile.name,
            body = requestBody
        )
        return userApi.uploadMyProfileImage(part)
    }

    suspend fun updateMyUsername(username: String): Response<UserProfileResponse> {
        validateUsername(username)
        return updateMyProfile(UpdateUserProfileRequest.updateUsername(username.trim()))
    }

    suspend fun updateMyProfileImageUrl(profileImageUrl: String?): Response<UserProfileResponse> {
        validateProfileImageUrl(profileImageUrl)
        return updateMyProfile(UpdateUserProfileRequest.updateProfileImageUrl(profileImageUrl))
    }

    suspend fun updateMyProfile(
        username: String,
        profileImageUrl: String?
    ): Response<UserProfileResponse> {
        validateUsername(username)
        validateProfileImageUrl(profileImageUrl)
        return updateMyProfile(
            UpdateUserProfileRequest.updateProfile(
                username = username.trim(),
                profileImageUrl = profileImageUrl
            )
        )
    }

    private suspend fun updateMyProfile(
        request: UpdateUserProfileRequest
    ): Response<UserProfileResponse> {
        val requestBody = request.toJsonObject()
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE.toMediaTypeOrNull())
        return userApi.updateMyProfile(requestBody)
    }

    suspend fun ensureOrganizationNamesLoaded(forceRefresh: Boolean = false) {
        if (!forceRefresh && _organizationNames.value.isNotEmpty()) {
            return
        }

        val response = categoryApi.getOrganizationCategories()
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

private fun validateUsername(username: String) {
    val trimmedUsername = username.trim()
    require(trimmedUsername.isNotBlank()) {
        "Username cannot be blank."
    }

    val maxLength = if (trimmedUsername.any { char -> char.isKorean() }) {
        KOREAN_USERNAME_MAX_LENGTH
    } else {
        ENGLISH_USERNAME_MAX_LENGTH
    }
    require(trimmedUsername.length <= maxLength) {
        "Username must be $maxLength characters or less."
    }
}

private fun Char.isKorean(): Boolean {
    return this in '\uAC00'..'\uD7A3' ||
        this in '\u1100'..'\u11FF' ||
        this in '\u3130'..'\u318F'
}

private fun validateProfileImageUrl(profileImageUrl: String?) {
    if (profileImageUrl == null) {
        return
    }

    require(profileImageUrl.startsWith("http://") || profileImageUrl.startsWith("https://")) {
        "Profile image URL must start with http:// or https://."
    }
}

private fun OrganizationCategoryResponse?.toOrganizationNameMap(): Map<Long, String> {
    return this?.items
        .orEmpty()
        .sortedBy { item -> item.sortOrder }
        .associate { item -> item.id to item.name }
}

private const val ENGLISH_USERNAME_MAX_LENGTH = 20
private const val KOREAN_USERNAME_MAX_LENGTH = 10
private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
