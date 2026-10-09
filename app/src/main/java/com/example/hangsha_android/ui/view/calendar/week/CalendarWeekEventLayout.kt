package com.example.hangsha_android.ui.view.calendar.week

import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal data class PositionedWeekAllDayEvent(
    val event: WeekAllDayEvent,
    val startDay: Int,
    val endDay: Int,
    val lane: Int,
    val continuesBeforeWeek: Boolean,
    val continuesAfterWeek: Boolean
)

internal fun positionWeekAllDayEvents(
    weekStart: LocalDate,
    events: List<WeekAllDayEvent>
): List<PositionedWeekAllDayEvent> {
    val weekEnd = weekStart.plusDays(6)
    val laneEnds = mutableListOf<Int>()
    return events.mapNotNull { event ->
        if (event.endDate.isBefore(weekStart) || event.startDate.isAfter(weekEnd)) return@mapNotNull null
        val startDay = ChronoUnit.DAYS.between(weekStart, maxOf(event.startDate, weekStart)).toInt()
        val endDay = ChronoUnit.DAYS.between(weekStart, minOf(event.endDate, weekEnd)).toInt()
        val lane = laneEnds.indexOfFirst { it < startDay }.takeIf { it >= 0 } ?: laneEnds.size
        if (lane == laneEnds.size) laneEnds += endDay else laneEnds[lane] = endDay
        PositionedWeekAllDayEvent(
            event = event,
            startDay = startDay,
            endDay = endDay,
            lane = lane,
            continuesBeforeWeek = event.startDate.isBefore(weekStart),
            continuesAfterWeek = event.endDate.isAfter(weekEnd)
        )
    }
}

internal data class PositionedWeekTimedEvent(
    val event: WeekTimedEvent,
    val dayIndex: Int,
    val lane: Int,
    val laneCount: Int
)

internal data class WeekTimedOverflow(
    val dayIndex: Int,
    val hiddenCount: Int
)

internal data class PositionedWeekTimedLayout(
    val visible: List<PositionedWeekTimedEvent>,
    val overflow: List<WeekTimedOverflow>
)

internal fun positionWeekTimedEvents(
    weekStart: LocalDate,
    events: List<WeekTimedEvent>
): PositionedWeekTimedLayout {
    val visible = mutableListOf<PositionedWeekTimedEvent>()
    val overflow = mutableListOf<WeekTimedOverflow>()
    events.groupBy { it.date }.forEach { (date, dayEvents) ->
        val dayIndex = ChronoUnit.DAYS.between(weekStart, date).toInt()
        if (dayIndex !in 0..6) return@forEach
        val shown = mutableListOf<WeekTimedEvent>()
        var hiddenCount = 0
        dayEvents.sortedWith(compareBy({ it.serverOrder }, { it.eventId })).forEach { event ->
            val overlapping = shown.filter {
                it.startMinute < event.endMinute && event.startMinute < it.endMinute
            }
            if ((overlapping + event).maxSimultaneousEvents() <= 2) {
                shown += event
            } else {
                hiddenCount++
            }
        }
        if (hiddenCount > 0) overflow += WeekTimedOverflow(dayIndex, hiddenCount)

        val group = mutableListOf<WeekTimedEvent>()
        var groupEnd = -1
        fun flush() {
            if (group.isEmpty()) return
            val laneCount = group.maxSimultaneousEvents()
            val laneEnds = IntArray(laneCount) { -1 }
            val positioned = group.map { event ->
                val lane = laneEnds.indexOfFirst { it <= event.startMinute }
                laneEnds[lane] = event.endMinute
                PositionedWeekTimedEvent(event, dayIndex, lane, laneCount)
            }
            val firstServerLane = positioned.minBy { it.event.serverOrder }.lane
            visible += if (laneCount == 2 && firstServerLane == 1) {
                positioned.map { it.copy(lane = 1 - it.lane) }
            } else positioned
            group.clear()
        }

        shown.sortedWith(compareBy({ it.startMinute }, { it.endMinute }, { it.serverOrder }))
            .forEach { event ->
                if (group.isNotEmpty() && event.startMinute >= groupEnd) {
                    flush()
                    groupEnd = -1
                }
                group += event
                groupEnd = maxOf(groupEnd, event.endMinute)
            }
        flush()
    }
    return PositionedWeekTimedLayout(visible, overflow)
}

private fun List<WeekTimedEvent>.maxSimultaneousEvents(): Int {
    val boundaries = flatMap { event ->
        listOf(event.startMinute to 1, event.endMinute to -1)
    }.sortedWith(compareBy({ it.first }, { it.second }))
    var active = 0
    var maximum = 0
    boundaries.forEach { (_, change) ->
        active += change
        maximum = maxOf(maximum, active)
    }
    return maximum
}
