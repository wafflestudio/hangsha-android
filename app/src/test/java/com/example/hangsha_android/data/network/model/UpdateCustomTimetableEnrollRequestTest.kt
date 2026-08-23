package com.example.hangsha_android.data.network.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCustomTimetableEnrollRequestTest {

    @Test
    fun serializesTimeSlotsAsNumericMinutes() {
        val request = UpdateCustomTimetableEnrollRequest.partial(
            timeSlots = TimetableEnrollPatchValue.Set(
                listOf(
                    UpdateCustomTimetableEnrollTimeSlotRequest(
                        dayOfWeek = "MONDAY",
                        startAt = 540,
                        endAt = 630
                    )
                )
            )
        )

        val slot = request.toJsonObject()["timeSlots"]
            .asJsonArray[0]
            .asJsonObject

        assertTrue(slot["startAt"].asJsonPrimitive.isNumber)
        assertTrue(slot["endAt"].asJsonPrimitive.isNumber)
        assertEquals(540, slot["startAt"].asInt)
        assertEquals(630, slot["endAt"].asInt)
    }
}
