package com.example.hangsha_android.data.network.model

import com.google.gson.Gson
import org.junit.Assert.assertNull
import org.junit.Test

class EventOrganizationParsingTest {
    private val gson = Gson()

    @Test
    fun parsesNullSummaryOrganizationIdAsNull() {
        val response = gson.fromJson("""{"orgId":null}""", EventSummaryResponse::class.java)

        assertNull(response.orgId)
    }

    @Test
    fun parsesMissingDetailOrganizationIdAsNull() {
        val response = gson.fromJson("{}", EventDetailResponse::class.java)

        assertNull(response.orgId)
    }
}
