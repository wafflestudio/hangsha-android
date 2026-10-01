package com.example.hangsha_android.ui.view.timetable

internal data class TimetableBlock(
    val id: String,
    val weekday: Int,
    val startMinute: Int,
    val endMinute: Int
)

internal data class PositionedTimetableBlock(
    val id: String,
    val weekday: Int,
    val clippedStartMinute: Int,
    val clippedEndMinute: Int,
    val topFraction: Float,
    val heightFraction: Float,
    val laneIndex: Int,
    val laneCount: Int,
    val pageIndex: Int,
    val pageCount: Int
)

internal object TimetableLayoutCalculator {
    fun positionBlocks(
        blocks: List<TimetableBlock>,
        gridStartMinute: Int,
        gridEndMinute: Int,
        dayCount: Int = 5,
        splitOverlaps: Boolean = false,
        maxLanesPerPage: Int = 2
    ): List<PositionedTimetableBlock> {
        if (gridEndMinute <= gridStartMinute) {
            return emptyList()
        }
        require(maxLanesPerPage > 0) { "maxLanesPerPage must be positive." }

        val clippedBlocks = blocks.mapNotNull { block ->
            clipBlock(
                block = block,
                gridStartMinute = gridStartMinute,
                gridEndMinute = gridEndMinute,
                dayCount = dayCount
            )
        }

        return if (splitOverlaps) {
            clippedBlocks
                .groupBy { it.weekday }
                .values
                .flatMap { blocksForDay -> assignOverlapLanes(blocksForDay, maxLanesPerPage) }
                .sortedWith(compareBy({ it.weekday }, { it.clippedStartMinute }, { it.pageIndex }, { it.laneIndex }))
        } else {
            clippedBlocks
        }
    }

    private fun clipBlock(
        block: TimetableBlock,
        gridStartMinute: Int,
        gridEndMinute: Int,
        dayCount: Int
    ): PositionedTimetableBlock? {
        if (block.weekday !in 0 until dayCount || block.endMinute <= block.startMinute) {
            return null
        }

        val clippedStart = maxOf(block.startMinute, gridStartMinute)
        val clippedEnd = minOf(block.endMinute, gridEndMinute)
        if (clippedEnd <= clippedStart) {
            return null
        }

        val gridDuration = gridEndMinute - gridStartMinute
        return PositionedTimetableBlock(
            id = block.id,
            weekday = block.weekday,
            clippedStartMinute = clippedStart,
            clippedEndMinute = clippedEnd,
            topFraction = (clippedStart - gridStartMinute).toFloat() / gridDuration,
            heightFraction = (clippedEnd - clippedStart).toFloat() / gridDuration,
            laneIndex = 0,
            laneCount = 1,
            pageIndex = 0,
            pageCount = 1
        )
    }

    private fun assignOverlapLanes(
        blocks: List<PositionedTimetableBlock>,
        maxLanesPerPage: Int
    ): List<PositionedTimetableBlock> {
        val sortedBlocks = blocks.sortedWith(compareBy({ it.clippedStartMinute }, { it.clippedEndMinute }))
        val result = mutableListOf<PositionedTimetableBlock>()
        val activeGroup = mutableListOf<PositionedTimetableBlock>()
        var activeGroupEnd = Int.MIN_VALUE

        fun flushGroup() {
            if (activeGroup.isEmpty()) {
                return
            }
            result += assignLanesWithinGroup(activeGroup, maxLanesPerPage)
            activeGroup.clear()
            activeGroupEnd = Int.MIN_VALUE
        }

        sortedBlocks.forEach { block ->
            if (activeGroup.isNotEmpty() && block.clippedStartMinute >= activeGroupEnd) {
                flushGroup()
            }
            activeGroup += block
            activeGroupEnd = maxOf(activeGroupEnd, block.clippedEndMinute)
        }
        flushGroup()

        return result
    }

    private fun assignLanesWithinGroup(
        blocks: List<PositionedTimetableBlock>,
        maxLanesPerPage: Int
    ): List<PositionedTimetableBlock> {
        val laneEnds = mutableListOf<Int>()
        val assigned = blocks.map { block ->
            val reusableLane = laneEnds.indexOfFirst { laneEnd -> laneEnd <= block.clippedStartMinute }
            val laneIndex = if (reusableLane >= 0) reusableLane else laneEnds.size
            if (reusableLane >= 0) {
                laneEnds[reusableLane] = block.clippedEndMinute
            } else {
                laneEnds += block.clippedEndMinute
            }
            block.copy(laneIndex = laneIndex)
        }
        val laneCount = maxOf(1, laneEnds.size)
        val pageCount = (laneCount + maxLanesPerPage - 1) / maxLanesPerPage
        return assigned.map { block ->
            val pageIndex = block.laneIndex / maxLanesPerPage
            val firstLaneOnPage = pageIndex * maxLanesPerPage
            val lanesOnPage = minOf(maxLanesPerPage, laneCount - firstLaneOnPage)
            block.copy(
                laneIndex = block.laneIndex % maxLanesPerPage,
                laneCount = lanesOnPage,
                pageIndex = pageIndex,
                pageCount = pageCount
            )
        }
    }
}
