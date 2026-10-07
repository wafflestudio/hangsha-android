package com.example.hangsha_android.ui.view.calendar.day

import com.example.hangsha_android.ui.view.calendar.CalendarEvent
import com.example.hangsha_android.util.HANGSHA_ZONE_ID
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

internal data class CalendarDayEventItem(
    val eventId: Long,
    val title: String,
    val eventTypeId: Long?,
    val startMinute: Int = 0,
    val endMinute: Int = 0
)

internal data class CalendarDayEventLayout(
    val allDay: List<CalendarDayEventItem>,
    val timed: List<CalendarDayEventItem>
)

internal object CalendarDayEventMapper {
    fun map(date: LocalDate, events: List<CalendarEvent>): CalendarDayEventLayout {
        val allDay = mutableListOf<CalendarDayEventItem>()
        val timed = mutableListOf<CalendarDayEventItem>()

        events.distinctBy { it.id }.forEach { event ->
            val item = event.toDayItem(date) ?: return@forEach
            if (item.endMinute > item.startMinute) timed += item else allDay += item
        }

        return CalendarDayEventLayout(
            allDay = allDay,
            timed = timed.sortedWith(compareBy({ it.startMinute }, { it.endMinute }, { it.eventId }))
        )
    }

    private fun CalendarEvent.toDayItem(date: LocalDate): CalendarDayEventItem? {
        val base = CalendarDayEventItem(id, title, eventTypeId)
        val primary = if (isPeriodEvent) parseRange(applyStart, applyEnd) else parseRange(eventStart, eventEnd)
        val fallback = if (isPeriodEvent) parseRange(eventStart, eventEnd) else parseRange(applyStart, applyEnd)
        val range = primary ?: fallback ?: return base
        val start = range.first
        val end = range.second
        if (start.isDateOnly || end.isDateOnly || !end.dateTime.isAfter(start.dateTime)) return base
        if (date.isBefore(start.dateTime.toLocalDate()) ||
            date.isAfter(end.dateTime.toLocalDate()) ||
            (date == end.dateTime.toLocalDate() && end.dateTime.toLocalTime() == LocalTime.MIDNIGHT)
        ) return null

        val startMinute: Int
        val endMinute: Int
        if (isPeriodEvent && start.dateTime.toLocalDate() != end.dateTime.toLocalDate()) {
            // A period uses the same daily opening hours across its date range.
            startMinute = start.dateTime.toLocalTime().toMinuteOfDay()
            endMinute = end.dateTime.toLocalTime().toMinuteOfDay()
        } else {
            val dayStart = date.atStartOfDay()
            val dayEnd = date.plusDays(1).atStartOfDay()
            val clippedStart = maxOf(start.dateTime, dayStart)
            val clippedEnd = minOf(end.dateTime, dayEnd)
            if (!clippedEnd.isAfter(clippedStart)) return null
            startMinute = clippedStart.toLocalTime().toMinuteOfDay()
            endMinute = if (clippedEnd == dayEnd) 24 * 60 else clippedEnd.toLocalTime().toMinuteOfDay()
        }

        // Server day-wide values commonly end at 23:59, not midnight of the next day.
        if (startMinute == 0 && endMinute >= 23 * 60 + 59) return base
        if (endMinute <= startMinute) return base
        return base.copy(startMinute = startMinute, endMinute = endMinute)
    }

    private fun parseRange(startValue: String?, endValue: String?): Pair<ParsedTime, ParsedTime>? {
        val start = parseTime(startValue) ?: return null
        val end = parseTime(endValue) ?: return null
        return start to end
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

    private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute

    private data class ParsedTime(val dateTime: LocalDateTime, val isDateOnly: Boolean)
}

internal data class PositionedCalendarDayEvent(
    val event: CalendarDayEventItem,
    val lane: Int,
    val laneCount: Int
)

internal fun positionCalendarDayEvents(events: List<CalendarDayEventItem>): List<PositionedCalendarDayEvent> {
    val result = mutableListOf<PositionedCalendarDayEvent>()
    val group = mutableListOf<CalendarDayEventItem>()
    var groupEnd = -1

    fun flushGroup() {
        if (group.isEmpty()) return
        val laneEnds = mutableListOf<Int>()
        val assigned = group.map { event ->
            val lane = laneEnds.indexOfFirst { end -> end <= event.startMinute }
                .takeIf { it >= 0 } ?: laneEnds.size
            if (lane == laneEnds.size) laneEnds += event.endMinute
            else laneEnds[lane] = event.endMinute
            event to lane
        }
        assigned.forEach { (event, lane) ->
            result += PositionedCalendarDayEvent(event, lane, laneEnds.size)
        }
        group.clear()
    }

    events.forEach { event ->
        if (group.isNotEmpty() && event.startMinute >= groupEnd) {
            flushGroup()
            groupEnd = -1
        }
        group += event
        groupEnd = maxOf(groupEnd, event.endMinute)
    }
    flushGroup()
    return result
}
