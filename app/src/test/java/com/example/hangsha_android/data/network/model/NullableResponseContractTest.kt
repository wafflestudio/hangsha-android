package com.example.hangsha_android.data.network.model

import com.google.gson.Gson
import org.junit.Assert.assertNull
import org.junit.Test

class NullableResponseContractTest {

    private val gson = Gson()

    @Test
    fun eventSummaryAcceptsNullableCategoryAndPersonalizationFields() {
        val response = gson.fromJson(
            """
                {
                  "id": 1,
                  "title": "행사",
                  "statusId": null,
                  "eventTypeId": null,
                  "isPeriodEvent": false,
                  "isInterested": null,
                  "isBookmarked": null
                }
            """.trimIndent(),
            EventSummaryResponse::class.java
        )

        assertNull(response.statusId)
        assertNull(response.eventTypeId)
        assertNull(response.isInterested)
        assertNull(response.isBookmarked)
    }

    @Test
    fun userProfileAcceptsNullableIdentityFields() {
        val response = gson.fromJson(
            """
                {
                  "id": 1,
                  "username": null,
                  "email": null,
                  "profileImageUrl": null,
                  "interestCategories": []
                }
            """.trimIndent(),
            UserProfileResponse::class.java
        )

        assertNull(response.username)
        assertNull(response.email)
    }
}
