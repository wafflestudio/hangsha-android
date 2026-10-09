package com.example.hangsha_android.ui.view.calendar.month

import com.example.hangsha_android.ui.view.calendar.CalendarEvent
import com.example.hangsha_android.ui.view.calendar.parseCalendarEventDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal data class CalendarMonthEventItem(
    val eventId: Long,
    val title: String,
    val eventTypeId: Long?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isPeriodEvent: Boolean
)

internal data class PositionedCalendarMonthEvent(
    val event: CalendarMonthEventItem,
    val startDay: Int,
    val endDay: Int,
    val laneIndex: Int,
    val continuesBeforeWeek: Boolean,
    val continuesAfterWeek: Boolean
)

internal data class CalendarMonthWeekLayout(
    val visibleEvents: List<PositionedCalendarMonthEvent>,
    val overflowByDay: List<Int>
)

internal object CalendarMonthEventMapper {
    fun map(eventsByDate: Map<LocalDate, List<CalendarEvent>>): List<CalendarMonthEventItem> {
        return eventsByDate.entries
            .sortedBy { it.key }
            .asSequence()
            .flatMap { (_, events) -> events.asSequence() }
            .distinctBy { it.id }
            .mapNotNull { event -> event.toMonthEventItem() }
            .sortedWith(monthEventComparator)
            .toList()
    }

    private fun CalendarEvent.toMonthEventItem(): CalendarMonthEventItem? {
        val eventRange = normalizeRange(eventStart, eventEnd)
        val applyRange = normalizeRange(applyStart, applyEnd)
        val range = if (isPeriodEvent) {
            applyRange ?: eventRange
        } else {
            eventRange ?: applyRange
        } ?: DateRange(date, date)

        if (range.end.isBefore(range.start)) return null

        return CalendarMonthEventItem(
            eventId = id,
            title = title,
            eventTypeId = eventTypeId,
            startDate = range.start,
            endDate = range.end,
            isPeriodEvent = isPeriodEvent
        )
    }

    private fun normalizeRange(start: String?, end: String?): DateRange? {
        val parsedStart = parseCalendarEventDate(start)
        val parsedEnd = parseCalendarEventDate(end)
        val fallback = parsedStart ?: parsedEnd ?: return null
        return DateRange(
            start = parsedStart ?: fallback,
            end = parsedEnd ?: fallback
        )
    }

    private data class DateRange(
        val start: LocalDate,
        val end: LocalDate
    )
}

internal object CalendarMonthEventLayoutCalculator {
    fun positionWeek(
        events: List<CalendarMonthEventItem>,
        weekStart: LocalDate,
        dayCount: Int = 7,
        maxVisibleLanes: Int = 4
    ): CalendarMonthWeekLayout {
        if (dayCount <= 0 || maxVisibleLanes < 0) {
            return CalendarMonthWeekLayout(
                visibleEvents = emptyList(),
                overflowByDay = emptyList()
            )
        }

        val weekEnd = weekStart.plusDays(dayCount - 1L)
        val segments = events
            .asSequence()
            .filterNot { it.endDate.isBefore(it.startDate) }
            .filterNot { it.endDate.isBefore(weekStart) || it.startDate.isAfter(weekEnd) }
            .sortedWith(monthEventComparator)
            .map { event ->
                val clippedStart = maxOf(event.startDate, weekStart)
                val clippedEnd = minOf(event.endDate, weekEnd)
                PositionedCalendarMonthEvent(
                    event = event,
                    startDay = ChronoUnit.DAYS.between(weekStart, clippedStart).toInt(),
                    endDay = ChronoUnit.DAYS.between(weekStart, clippedEnd).toInt(),
                    laneIndex = 0,
                    continuesBeforeWeek = event.startDate.isBefore(weekStart),
                    continuesAfterWeek = event.endDate.isAfter(weekEnd)
                )
            }
            .toList()

        val lanes = mutableListOf<MutableList<PositionedCalendarMonthEvent>>()
        val positioned = segments.map { segment ->
            val reusableLane = lanes.indexOfFirst { lane ->
                lane.none { existing ->
                    existing.startDay <= segment.endDay && existing.endDay >= segment.startDay
                }
            }
            val laneIndex = if (reusableLane >= 0) reusableLane else lanes.size
            if (reusableLane >= 0) {
                lanes[reusableLane] += segment
            } else {
                lanes += mutableListOf(segment)
            }
            segment.copy(laneIndex = laneIndex)
        }

        val overflowByDay = MutableList(dayCount) { 0 }
        positioned
            .filter { it.laneIndex >= maxVisibleLanes }
            .forEach { segment ->
                for (day in segment.startDay..segment.endDay) {
                    overflowByDay[day] += 1
                }
            }

        return CalendarMonthWeekLayout(
            visibleEvents = positioned.filter { it.laneIndex < maxVisibleLanes },
            overflowByDay = overflowByDay
        )
    }
}

private val monthEventComparator = compareBy<CalendarMonthEventItem>(
    { it.startDate != it.endDate },
    { it.startDate },
    { it.endDate },
    { it.eventId }
)
