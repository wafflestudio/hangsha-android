package com.example.hangsha_android.data.network.model

import com.google.gson.JsonNull
import com.google.gson.JsonObject

class UpdateUserProfileRequest private constructor(
    private val username: PatchValue<String>,
    private val profileImageUrl: PatchValue<String>
) {
    fun toJsonObject(): JsonObject {
        require(username !is PatchValue.Unchanged || profileImageUrl !is PatchValue.Unchanged) {
            "At least one profile field must be included."
        }

        return JsonObject().apply {
            when (username) {
                is PatchValue.Set -> addProperty("username", username.value)
                PatchValue.Clear -> error("Username cannot be cleared.")
                PatchValue.Unchanged -> Unit
            }

            when (profileImageUrl) {
                is PatchValue.Set -> addProperty("profileImageUrl", profileImageUrl.value)
                PatchValue.Clear -> add("profileImageUrl", JsonNull.INSTANCE)
                PatchValue.Unchanged -> Unit
            }
        }
    }

    companion object {
        fun updateUsername(username: String): UpdateUserProfileRequest {
            return UpdateUserProfileRequest(
                username = PatchValue.Set(username),
                profileImageUrl = PatchValue.Unchanged
            )
        }

        fun updateProfileImageUrl(profileImageUrl: String?): UpdateUserProfileRequest {
            return UpdateUserProfileRequest(
                username = PatchValue.Unchanged,
                profileImageUrl = if (profileImageUrl == null) {
                    PatchValue.Clear
                } else {
                    PatchValue.Set(profileImageUrl)
                }
            )
        }

        fun updateProfile(
            username: String,
            profileImageUrl: String?
        ): UpdateUserProfileRequest {
            return UpdateUserProfileRequest(
                username = PatchValue.Set(username),
                profileImageUrl = if (profileImageUrl == null) {
                    PatchValue.Clear
                } else {
                    PatchValue.Set(profileImageUrl)
                }
            )
        }
    }
}

private sealed interface PatchValue<out T> {
    data class Set<T>(val value: T) : PatchValue<T>

    data object Clear : PatchValue<Nothing>

    data object Unchanged : PatchValue<Nothing>
}
