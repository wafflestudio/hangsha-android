package com.example.hangsha_android.ui.view.timetable

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableTimelineLayoutCalculatorTest {
    private val monday = LocalDate.of(2026, 8, 17)

    @Test
    fun positionBlocks_clipsRangeToMondayThroughFriday() {
        val result = TimetableTimelineLayoutCalculator.positionBlocks(
            blocks = listOf(
                TimetableTimelineBlock(
                    id = "range",
                    startDate = monday.minusDays(2),
                    endDate = monday.plusDays(6)
                )
            ),
            weekStart = monday
        )

        val block = result.single()
        assertEquals(0, block.startDay)
        assertEquals(4, block.endDay)
        assertTrue(block.continuesBeforeWeek)
        assertTrue(block.continuesAfterWeek)
    }

    @Test
    fun positionBlocks_assignsSeparateLanesForOverlappingSpans() {
        val result = TimetableTimelineLayoutCalculator.positionBlocks(
            blocks = listOf(
                TimetableTimelineBlock("a", monday, monday.plusDays(2)),
                TimetableTimelineBlock("b", monday.plusDays(1), monday.plusDays(3)),
                TimetableTimelineBlock("c", monday.plusDays(3), monday.plusDays(4))
            ),
            weekStart = monday
        ).associateBy { it.id }

        assertEquals(0, result.getValue("a").laneIndex)
        assertEquals(1, result.getValue("b").laneIndex)
        assertEquals(0, result.getValue("c").laneIndex)
        assertEquals(2, result.getValue("a").laneCount)
        assertEquals(2, result.getValue("b").laneCount)
        assertEquals(2, result.getValue("c").laneCount)
    }

    @Test
    fun positionBlocks_preservesSingleDaySpanWithoutContinuation() {
        val result = TimetableTimelineLayoutCalculator.positionBlocks(
            blocks = listOf(
                TimetableTimelineBlock("single", monday.plusDays(2), monday.plusDays(2))
            ),
            weekStart = monday
        )

        val block = result.single()
        assertEquals(2, block.startDay)
        assertEquals(2, block.endDay)
        assertFalse(block.continuesBeforeWeek)
        assertFalse(block.continuesAfterWeek)
    }

    @Test
    fun positionBlocks_ignoresInvalidAndOutOfWeekSpans() {
        val result = TimetableTimelineLayoutCalculator.positionBlocks(
            blocks = listOf(
                TimetableTimelineBlock("weekend", monday.plusDays(5), monday.plusDays(6)),
                TimetableTimelineBlock("invalid", monday.plusDays(3), monday.plusDays(2))
            ),
            weekStart = monday
        )

        assertTrue(result.isEmpty())
    }
}
