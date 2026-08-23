package com.example.hangsha_android.ui.view.timetable

import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal data class TimetableTimelineBlock(
    val id: String,
    val startDate: LocalDate,
    val endDate: LocalDate
)

internal data class PositionedTimelineBlock(
    val id: String,
    val startDay: Int,
    val endDay: Int,
    val laneIndex: Int,
    val laneCount: Int,
    val continuesBeforeWeek: Boolean,
    val continuesAfterWeek: Boolean
)

internal object TimetableTimelineLayoutCalculator {
    fun positionBlocks(
        blocks: List<TimetableTimelineBlock>,
        weekStart: LocalDate,
        dayCount: Int = 5
    ): List<PositionedTimelineBlock> {
        if (dayCount <= 0) return emptyList()
        val weekEnd = weekStart.plusDays(dayCount - 1L)
        val clipped = blocks.mapNotNull { block ->
            if (block.endDate.isBefore(block.startDate)) return@mapNotNull null
            if (block.endDate.isBefore(weekStart) || block.startDate.isAfter(weekEnd)) return@mapNotNull null

            val clippedStart = maxOf(block.startDate, weekStart)
            val clippedEnd = minOf(block.endDate, weekEnd)
            PositionedTimelineBlock(
                id = block.id,
                startDay = ChronoUnit.DAYS.between(weekStart, clippedStart).toInt(),
                endDay = ChronoUnit.DAYS.between(weekStart, clippedEnd).toInt(),
                laneIndex = 0,
                laneCount = 1,
                continuesBeforeWeek = block.startDate.isBefore(weekStart),
                continuesAfterWeek = block.endDate.isAfter(weekEnd)
            )
        }.sortedWith(compareBy({ it.startDay }, { it.endDay }, { it.id }))

        val laneEnds = mutableListOf<Int>()
        val assigned = clipped.map { block ->
            val reusableLane = laneEnds.indexOfFirst { laneEnd -> laneEnd < block.startDay }
            val laneIndex = if (reusableLane >= 0) reusableLane else laneEnds.size
            if (reusableLane >= 0) {
                laneEnds[reusableLane] = block.endDay
            } else {
                laneEnds += block.endDay
            }
            block.copy(laneIndex = laneIndex)
        }
        val laneCount = maxOf(1, laneEnds.size)
        return assigned.map { it.copy(laneCount = laneCount) }
    }
}
