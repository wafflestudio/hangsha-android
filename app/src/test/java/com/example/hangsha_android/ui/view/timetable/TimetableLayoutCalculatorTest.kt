package com.example.hangsha_android.ui.view.timetable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableLayoutCalculatorTest {
    @Test
    fun positionBlocks_clipsBlockToVisibleGrid() {
        val result = TimetableLayoutCalculator.positionBlocks(
            blocks = listOf(
                TimetableBlock(
                    id = "early",
                    weekday = 0,
                    startMinute = 8 * 60 + 30,
                    endMinute = 9 * 60 + 30
                )
            ),
            gridStartMinute = 9 * 60,
            gridEndMinute = 18 * 60
        )

        assertEquals(1, result.size)
        assertEquals(9 * 60, result.first().clippedStartMinute)
        assertEquals(9 * 60 + 30, result.first().clippedEndMinute)
        assertEquals(0f, result.first().topFraction, 0.0001f)
        assertEquals(30f / 540f, result.first().heightFraction, 0.0001f)
    }

    @Test
    fun positionBlocks_ignoresBlocksOutsideGridOrWeek() {
        val result = TimetableLayoutCalculator.positionBlocks(
            blocks = listOf(
                TimetableBlock(
                    id = "before",
                    weekday = 0,
                    startMinute = 7 * 60,
                    endMinute = 8 * 60
                ),
                TimetableBlock(
                    id = "after",
                    weekday = 1,
                    startMinute = 19 * 60,
                    endMinute = 20 * 60
                ),
                TimetableBlock(
                    id = "weekend",
                    weekday = 5,
                    startMinute = 10 * 60,
                    endMinute = 11 * 60
                )
            ),
            gridStartMinute = 9 * 60,
            gridEndMinute = 18 * 60
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun positionBlocks_splitsOverlappingBlocksIntoLanes() {
        val result = TimetableLayoutCalculator.positionBlocks(
            blocks = listOf(
                TimetableBlock(
                    id = "a",
                    weekday = 2,
                    startMinute = 10 * 60,
                    endMinute = 12 * 60
                ),
                TimetableBlock(
                    id = "b",
                    weekday = 2,
                    startMinute = 11 * 60,
                    endMinute = 13 * 60
                )
            ),
            gridStartMinute = 9 * 60,
            gridEndMinute = 18 * 60,
            splitOverlaps = true
        ).sortedBy { it.id }

        assertEquals(2, result.size)
        assertEquals(2, result[0].laneCount)
        assertEquals(2, result[1].laneCount)
        assertEquals(0, result[0].laneIndex)
        assertEquals(1, result[1].laneIndex)
    }
}
