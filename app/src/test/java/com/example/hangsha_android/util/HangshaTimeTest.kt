package com.example.hangsha_android.util

import java.time.LocalDate
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class HangshaTimeTest {

    @Test
    fun convertsOffsetTimestampToSeoulCalendarDate() {
        val timestamp = OffsetDateTime.parse("2026-08-21T16:00:00Z")

        assertEquals(LocalDate.of(2026, 8, 22), timestamp.toHangshaDate())
    }
}
