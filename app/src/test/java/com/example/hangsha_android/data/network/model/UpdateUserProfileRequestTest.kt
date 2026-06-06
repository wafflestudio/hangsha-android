package com.example.hangsha_android.data.network.model

import com.google.gson.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateUserProfileRequestTest {

    @Test
    fun updateUsernameIncludesOnlyUsername() {
        val jsonObject = UpdateUserProfileRequest
            .updateUsername("new_name")
            .toJsonObject()

        assertEquals("new_name", jsonObject["username"].asString)
        assertFalse(jsonObject.has("profileImageUrl"))
    }

    @Test
    fun updateProfileImageUrlWithNullIncludesJsonNull() {
        val jsonObject = UpdateUserProfileRequest
            .updateProfileImageUrl(null)
            .toJsonObject()

        assertFalse(jsonObject.has("username"))
        assertTrue(jsonObject.has("profileImageUrl"))
        assertSame(JsonNull.INSTANCE, jsonObject["profileImageUrl"])
    }

    @Test
    fun updateProfileIncludesBothFields() {
        val jsonObject = UpdateUserProfileRequest
            .updateProfile(
                username = "new_name",
                profileImageUrl = "https://example.com/profile.png"
            )
            .toJsonObject()

        assertEquals("new_name", jsonObject["username"].asString)
        assertEquals("https://example.com/profile.png", jsonObject["profileImageUrl"].asString)
    }
}
