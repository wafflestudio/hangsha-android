package com.example.hangsha_android.ui.view.timetable

import org.junit.Assert.assertEquals
import org.junit.Test

class TimetableLayoutCalculatorTest {
    @Test
    fun positionBlocks_sixOverlappingEvents_areSplitAcrossThreeTwoColumnPages() {
        val positions = positionEvents(
            (1..6).map { index ->
                TimetableBlock(
                    id = "event-$index",
                    weekday = 1,
                    startMinute = 9 * 60,
                    endMinute = 10 * 60
                )
            }
        )

        assertEquals(listOf(0, 0, 1, 1, 2, 2), positions.map { it.pageIndex })
        assertEquals(listOf(0, 1, 0, 1, 0, 1), positions.map { it.laneIndex })
        assertEquals(List(6) { 2 }, positions.map { it.laneCount })
        assertEquals(List(6) { 3 }, positions.map { it.pageCount })
    }

    @Test
    fun positionBlocks_lastPageWithOneEvent_usesTheFullDayWidth() {
        val positions = positionEvents(
            (1..5).map { index ->
                TimetableBlock(
                    id = "event-$index",
                    weekday = 0,
                    startMinute = 12 * 60,
                    endMinute = 13 * 60
                )
            }
        )

        val lastPageEvent = positions.single { it.pageIndex == 2 }
        assertEquals(0, lastPageEvent.laneIndex)
        assertEquals(1, lastPageEvent.laneCount)
        assertEquals(3, lastPageEvent.pageCount)
    }

    @Test
    fun positionBlocks_touchingAndDifferentDayEvents_doNotShareOverlapPages() {
        val positions = positionEvents(
            listOf(
                TimetableBlock("monday-first", 0, 9 * 60, 10 * 60),
                TimetableBlock("monday-second", 0, 10 * 60, 11 * 60),
                TimetableBlock("tuesday-first", 1, 9 * 60, 10 * 60),
                TimetableBlock("tuesday-second", 1, 9 * 60, 10 * 60),
                TimetableBlock("tuesday-third", 1, 9 * 60, 10 * 60)
            )
        ).associateBy { it.id }

        assertEquals(1, positions.getValue("monday-first").pageCount)
        assertEquals(1, positions.getValue("monday-second").pageCount)
        assertEquals(2, positions.getValue("tuesday-third").pageCount)
        assertEquals(1, positions.getValue("tuesday-third").pageIndex)
        assertEquals(0, positions.getValue("tuesday-third").laneIndex)
    }

    private fun positionEvents(blocks: List<TimetableBlock>): List<PositionedTimetableBlock> {
        return TimetableLayoutCalculator.positionBlocks(
            blocks = blocks,
            gridStartMinute = 7 * 60,
            gridEndMinute = 24 * 60,
            dayCount = 5,
            splitOverlaps = true,
            maxLanesPerPage = 2
        )
    }
}
