package com.example.hangsha_android.data.network.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class EventCountResponseParsingTest {
    @Test
    fun eventCountResponse_parsesCount() {
        val response = Gson().fromJson("""{"count":7}""", EventCountResponse::class.java)

        assertEquals(7, response.count)
    }
}