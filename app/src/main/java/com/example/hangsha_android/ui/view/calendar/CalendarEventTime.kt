package com.example.hangsha_android.ui.view.calendar

import com.example.hangsha_android.util.HANGSHA_ZONE_ID
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/** Preserve date-only values so timeline views can distinguish them from timed events. */
internal data class CalendarEventTime(val dateTime: LocalDateTime, val isDateOnly: Boolean)

internal data class CalendarEventTimeRange(val start: CalendarEventTime, val end: CalendarEventTime)

internal fun parseCalendarEventTime(value: String?): CalendarEventTime? {
    val text = value?.trim().orEmpty()
    if (text.isEmpty()) return null
    try {
        return CalendarEventTime(
            OffsetDateTime.parse(text).atZoneSameInstant(HANGSHA_ZONE_ID).toLocalDateTime(), false
        )
    } catch (_: DateTimeParseException) { }
    try {
        return CalendarEventTime(
            ZonedDateTime.parse(text).withZoneSameInstant(HANGSHA_ZONE_ID).toLocalDateTime(), false
        )
    } catch (_: DateTimeParseException) { }
    try {
        return CalendarEventTime(LocalDateTime.parse(text), false)
    } catch (_: DateTimeParseException) { }
    return try {
        CalendarEventTime(LocalDate.parse(text).atStartOfDay(), true)
    } catch (_: DateTimeParseException) {
        null
    }
}

internal fun parseCalendarEventDate(value: String?): LocalDate? =
    parseCalendarEventTime(value)?.dateTime?.toLocalDate()

/** Timeline ranges require both endpoints; each view decides how to render missing ranges. */
internal fun CalendarEvent.timelineRange(): CalendarEventTimeRange? {
    fun parseRange(start: String?, end: String?): CalendarEventTimeRange? {
        val parsedStart = parseCalendarEventTime(start) ?: return null
        val parsedEnd = parseCalendarEventTime(end) ?: return null
        return CalendarEventTimeRange(parsedStart, parsedEnd)
    }
    return if (isPeriodEvent) {
        parseRange(applyStart, applyEnd) ?: parseRange(eventStart, eventEnd)
    } else {
        parseRange(eventStart, eventEnd) ?: parseRange(applyStart, applyEnd)
    }
}
