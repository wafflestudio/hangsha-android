package com.example.hangsha_android.ui.view.event

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class EventTypeColorPaletteTest {
    @Test
    fun eventColors_matchWebPaletteForApiCategoryIds() {
        val expected = mapOf(
            4L to Color(0x99FF8C28),
            5L to Color(0x99BA9E31),
            6L to Color(0x990BCE83),
            7L to Color(0x9900C1E8),
            8L to Color(0x990088FF),
            9L to Color(0x99A25AFF),
            10L to Color(0x99FF2D53)
        )

        expected.forEach { (eventTypeId, color) ->
            assertEquals(color, eventTypeColor(eventTypeId))
        }
    }

    @Test
    fun filterColors_matchWebButtonPaletteForApiCategoryIds() {
        val expected = mapOf(
            4L to Color(0x26FF8C28),
            5L to Color(0x26FFCC00),
            6L to Color(0x260BCE83),
            7L to Color(0x2600C1E8),
            8L to Color(0x260088FF),
            9L to Color(0x26A25AFF),
            10L to Color(0x26FF2D53)
        )

        expected.forEach { (eventTypeId, color) ->
            assertEquals(color, eventTypeFilterColor(eventTypeId))
        }
    }
}
