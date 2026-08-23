package com.example.hangsha_android.data.network.model

import com.example.hangsha_android.data.repository.model.CategoryKey
import com.example.hangsha_android.data.repository.model.CategoryType
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UserProfileResponseTest {

    private val gson = Gson()

    @Test
    fun parsesFlatInterestCategoriesWithCategoryType() {
        val json = """
            {
              "id": 10,
              "username": "푱푱한 토끼",
              "email": "123@test.com",
              "profileImageUrl": "https://example.com/profile.png",
              "interestCategories": [
                {
                  "categoryType": "ORGANIZATION",
                  "categoryId": 6,
                  "name": "글로벌사회공헌단",
                  "sortOrder": 6,
                  "priority": 1
                },
                {
                  "categoryType": "EVENT_TYPE",
                  "categoryId": 6,
                  "name": "OpenLnL",
                  "sortOrder": 6,
                  "priority": 2
                }
              ]
            }
        """.trimIndent()

        val response = gson.fromJson(json, UserProfileResponse::class.java)

        assertEquals(10L, response.id)
        assertEquals("푱푱한 토끼", response.username)
        assertEquals("123@test.com", response.email)
        assertNotNull(response.profileImageUrl)
        assertEquals(2, response.interestCategories.orEmpty().size)
        val organization = response.interestCategories.orEmpty()[0]
        val eventType = response.interestCategories.orEmpty()[1]
        assertEquals(CategoryType.ORGANIZATION, organization.categoryType)
        assertEquals(CategoryKey(CategoryType.ORGANIZATION, 6L), organization.key)
        assertEquals("글로벌사회공헌단", organization.name)
        assertEquals(CategoryKey(CategoryType.EVENT_TYPE, 6L), eventType.key)
        assertEquals(1, organization.priority)
    }
}
