package com.example.hangsha_android.data.network.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UserProfileResponseTest {

    private val gson = Gson()

    @Test
    fun parsesMyPageProfileResponse() {
        val json = """
            {
              "id": 10,
              "username": "푱푱한 토끼",
              "email": "123@test.com",
              "profileImageUrl": "https://objectstorage.ap-chuncheon-1.oraclecloud.com/n/ax1dvc8vmenm/b/hangsha-asset/o/default/43513b43-2f84-4f0f-8de8-7d61120fe3aa.png",
              "interestCategories": [
                {
                  "category": {
                    "id": 16,
                    "groupId": 2,
                    "name": "글로벌사회공헌단",
                    "sortOrder": 6
                  },
                  "priority": 1
                },
                {
                  "category": {
                    "id": 18,
                    "groupId": 2,
                    "name": "인권센터",
                    "sortOrder": 8
                  },
                  "priority": 2
                },
                {
                  "category": {
                    "id": 17,
                    "groupId": 2,
                    "name": "아동가족학과",
                    "sortOrder": 7
                  },
                  "priority": 3
                }
              ]
            }
        """.trimIndent()

        val response = gson.fromJson(json, UserProfileResponse::class.java)

        assertEquals(10L, response.id)
        assertEquals("푱푱한 토끼", response.username)
        assertEquals("123@test.com", response.email)
        assertNotNull(response.profileImageUrl)
        assertEquals(3, response.interestCategories.orEmpty().size)
        assertEquals("글로벌사회공헌단", response.interestCategories.orEmpty()[0].category.name)
        assertEquals(1, response.interestCategories.orEmpty()[0].priority)
    }
}
