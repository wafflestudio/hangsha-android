package com.example.hangsha_android.data.network.model

import com.example.hangsha_android.data.repository.model.CategoryType
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateInterestCategoriesRequestTest {

    @Test
    fun serializesCategoryTypeForCollidingIds() {
        val request = UpdateInterestCategoriesRequest(
            items = listOf(
                UpdateInterestCategoryItemRequest(CategoryType.EVENT_TYPE, 1L, 1),
                UpdateInterestCategoryItemRequest(CategoryType.ORGANIZATION, 1L, 2)
            )
        )

        val items = JsonParser.parseString(Gson().toJson(request))
            .asJsonObject["items"]
            .asJsonArray
        assertEquals("EVENT_TYPE", items[0].asJsonObject["categoryType"].asString)
        assertEquals(1L, items[0].asJsonObject["categoryId"].asLong)
        assertEquals("ORGANIZATION", items[1].asJsonObject["categoryType"].asString)
        assertEquals(1L, items[1].asJsonObject["categoryId"].asLong)
    }
}
