package com.example.hangsha_android.ui.view.calendar.week

import com.example.hangsha_android.ui.view.calendar.CalendarEvent
import com.example.hangsha_android.util.HANGSHA_ZONE_ID
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

internal data class WeekAllDayEvent(
    val eventId: Long,
    val title: String,
    val eventTypeId: Long?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isPeriodEvent: Boolean
)

internal data class WeekTimedEvent(
    val eventId: Long,
    val title: String,
    val eventTypeId: Long?,
    val date: LocalDate,
    val startMinute: Int,
    val endMinute: Int,
    val serverOrder: Int
)

internal data class CalendarWeekEventLayout(
    val allDay: List<WeekAllDayEvent>,
    val timed: List<WeekTimedEvent>
)

internal object CalendarWeekEventMapper {
    fun map(
        weekStart: LocalDate,
        eventsByDate: Map<LocalDate, List<CalendarEvent>>
    ): CalendarWeekEventLayout {
        val weekEnd = weekStart.plusDays(6)
        val allDay = mutableListOf<WeekAllDayEvent>()
        val timed = mutableListOf<WeekTimedEvent>()
        val sourceDates = mutableMapOf<Long, MutableList<LocalDate>>()
        eventsByDate.forEach { (date, events) ->
            events.forEach { event -> sourceDates.getOrPut(event.id) { mutableListOf() } += date }
        }
        val sourceEntries = eventsByDate.entries.sortedBy { it.key }
        val sourceEvents = sourceEntries.flatMap { it.value }.distinctBy { it.id }
        val fallbackOrder = sourceEvents.mapIndexed { index, event -> event.id to index }.toMap()
        val serverOrderByDate = sourceEntries.associate { (date, events) ->
            date to events.mapIndexed { index, event -> event.id to index }
                .distinctBy { it.first }.toMap()
        }

        fun CalendarEvent.addTimed(date: LocalDate, start: Int, end: Int) {
            val dateOrder = serverOrderByDate[date].orEmpty()
            val order = dateOrder[id]
                ?: (eventsByDate[date]?.size ?: 0) + fallbackOrder.getValue(id)
            timed += WeekTimedEvent(
                eventId = id,
                title = title,
                eventTypeId = eventTypeId,
                date = date,
                startMinute = start,
                endMinute = end,
                serverOrder = order
            )
        }

        sourceEvents.forEach { event ->
                fun addAllDay(from: LocalDate, to: LocalDate) {
                    if (to.isBefore(weekStart) || from.isAfter(weekEnd) || to.isBefore(from)) return
                    allDay += WeekAllDayEvent(
                        eventId = event.id,
                        title = event.title,
                        eventTypeId = event.eventTypeId,
                        startDate = from,
                        endDate = to,
                        isPeriodEvent = event.isPeriodEvent
                    )
                }

                val range = event.displayRange()
                if (range == null || !range.end.dateTime.isAfter(range.start.dateTime)) {
                    val dates = sourceDates[event.id].orEmpty()
                    if (dates.isNotEmpty()) addAllDay(dates.min(), dates.max())
                    return@forEach
                }

                val startDate = range.start.dateTime.toLocalDate()
                val rawEndDate = range.end.dateTime.toLocalDate()
                val endDate = if (
                    !range.end.isDateOnly && rawEndDate.isAfter(startDate) &&
                    range.end.dateTime.toLocalTime() == LocalTime.MIDNIGHT
                ) rawEndDate.minusDays(1) else rawEndDate
                if (endDate.isBefore(startDate)) return@forEach

                if (range.start.isDateOnly || range.end.isDateOnly) {
                    addAllDay(startDate, endDate)
                    return@forEach
                }

                if (event.isPeriodEvent && startDate != endDate) {
                    val startMinute = range.start.dateTime.toMinuteOfDay()
                    val endMinute = range.end.dateTime.toMinuteOfDay()
                    if (isFullDay(startMinute, endMinute) || endMinute <= startMinute) {
                        addAllDay(startDate, endDate)
                    } else {
                        datesBetween(maxOf(startDate, weekStart), minOf(endDate, weekEnd))
                            .forEach { date ->
                                event.addTimed(date, startMinute, endMinute)
                            }
                    }
                    return@forEach
                }

                val allDayDates = mutableListOf<LocalDate>()
                datesBetween(maxOf(startDate, weekStart), minOf(endDate, weekEnd)).forEach dayLoop@{ date ->
                    val dayStart = date.atStartOfDay()
                    val dayEnd = date.plusDays(1).atStartOfDay()
                    val clippedStart = maxOf(range.start.dateTime, dayStart)
                    val clippedEnd = minOf(range.end.dateTime, dayEnd)
                    if (!clippedEnd.isAfter(clippedStart)) return@dayLoop

                    val startMinute = clippedStart.toMinuteOfDay()
                    val endMinute = if (clippedEnd == dayEnd) 24 * 60
                    else clippedEnd.toMinuteOfDay()
                    if (isFullDay(startMinute, endMinute) || endMinute <= startMinute) {
                        allDayDates += date
                    } else {
                        event.addTimed(date, startMinute, endMinute)
                    }
                }
                // A multi-day timed event may contain fully occupied middle days.
                if (allDayDates.isNotEmpty()) {
                    addAllDay(allDayDates.first(), allDayDates.last())
                }
            }

        return CalendarWeekEventLayout(
            allDay = allDay,
            timed = timed
        )
    }

    private fun CalendarEvent.displayRange(): ParsedRange? {
        val primary = if (isPeriodEvent) parseRange(applyStart, applyEnd)
        else parseRange(eventStart, eventEnd)
        val fallback = if (isPeriodEvent) parseRange(eventStart, eventEnd)
        else parseRange(applyStart, applyEnd)
        return primary ?: fallback
    }

    private fun parseRange(start: String?, end: String?): ParsedRange? {
        val parsedStart = parseTime(start) ?: return null
        val parsedEnd = parseTime(end) ?: return null
        return ParsedRange(parsedStart, parsedEnd)
    }

    private fun parseTime(value: String?): ParsedTime? {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return null
        try {
            return ParsedTime(
                OffsetDateTime.parse(text).atZoneSameInstant(HANGSHA_ZONE_ID).toLocalDateTime(),
                false
            )
        } catch (_: DateTimeParseException) { }
        try {
            return ParsedTime(
                ZonedDateTime.parse(text).withZoneSameInstant(HANGSHA_ZONE_ID).toLocalDateTime(),
                false
            )
        } catch (_: DateTimeParseException) { }
        try {
            return ParsedTime(LocalDateTime.parse(text), false)
        } catch (_: DateTimeParseException) { }
        return try {
            ParsedTime(LocalDate.parse(text).atStartOfDay(), true)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun LocalDateTime.toMinuteOfDay(): Int = hour * 60 + minute
    private fun isFullDay(start: Int, end: Int): Boolean = start == 0 && end >= 23 * 60 + 59

    private fun datesBetween(from: LocalDate, to: LocalDate): List<LocalDate> {
        if (to.isBefore(from)) return emptyList()
        return (0L..ChronoUnit.DAYS.between(from, to)).map { from.plusDays(it) }
    }

    private data class ParsedTime(val dateTime: LocalDateTime, val isDateOnly: Boolean)
    private data class ParsedRange(val start: ParsedTime, val end: ParsedTime)
}

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
