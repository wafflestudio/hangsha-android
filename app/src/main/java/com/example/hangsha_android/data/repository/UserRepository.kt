package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.api.UserApi
import com.example.hangsha_android.data.network.model.ProfileImageUploadResponse
import com.example.hangsha_android.data.network.model.UpdateInterestCategoriesRequest
import com.example.hangsha_android.data.network.model.UpdateInterestCategoryItemRequest
import com.example.hangsha_android.data.network.model.UpdateUserProfileRequest
import com.example.hangsha_android.data.network.model.UserInterestCategoriesResponse
import com.example.hangsha_android.data.network.model.UserProfileResponse
import com.example.hangsha_android.data.repository.model.CategoryKey
import java.io.File
import javax.inject.Singleton
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

@Singleton
class UserRepository @Inject constructor(
    private val userApi: UserApi
) {
    suspend fun getMyProfile(): Response<UserProfileResponse> {
        return userApi.getMyProfile()
    }

    suspend fun getMyInterestCategories(): Response<UserInterestCategoriesResponse> {
        return userApi.getMyInterestCategories()
    }

    suspend fun deleteMyAccount(): Response<Unit> {
        return userApi.deleteMyAccount()
    }

    suspend fun updateMyInterestCategories(categoryKeys: List<CategoryKey>): Response<Unit> {
        val request = UpdateInterestCategoriesRequest(
            items = categoryKeys
                .take(MAX_INTEREST_PRIORITY_COUNT)
                .mapIndexed { index, categoryKey ->
                    UpdateInterestCategoryItemRequest(
                        categoryType = categoryKey.type,
                        categoryId = categoryKey.id,
                        priority = index + 1
                    )
                }
        )
        return userApi.updateMyInterestCategories(request)
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

}

private fun validateUsername(username: String) {
    val trimmedUsername = username.trim()
    require(trimmedUsername.isNotBlank()) {
        "\uB2C9\uB124\uC784\uC744 \uC785\uB825\uD574 \uC8FC\uC138\uC694."
    }

    val maxLength = if (trimmedUsername.any { char -> char.isKorean() }) {
        KOREAN_USERNAME_MAX_LENGTH
    } else {
        ENGLISH_USERNAME_MAX_LENGTH
    }
    require(trimmedUsername.length <= maxLength) {
        "\uB2C9\uB124\uC784\uC740 ${maxLength}\uC790 \uC774\uD558\uB85C \uC785\uB825\uD574 \uC8FC\uC138\uC694."
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
        "\uD504\uB85C\uD544 \uC774\uBBF8\uC9C0 \uC8FC\uC18C\uB294 http:// \uB610\uB294 https://\uB85C \uC2DC\uC791\uD574\uC57C \uD569\uB2C8\uB2E4."
    }
}

private const val ENGLISH_USERNAME_MAX_LENGTH = 20
private const val KOREAN_USERNAME_MAX_LENGTH = 10
private const val MAX_INTEREST_PRIORITY_COUNT = 3
private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
