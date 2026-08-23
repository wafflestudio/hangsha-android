package com.example.hangsha_android.ui.view.timetable

import com.example.hangsha_android.data.network.model.EventSummaryResponse
import com.example.hangsha_android.ui.view.event.eventTypeColor
import com.example.hangsha_android.util.HANGSHA_ZONE_ID
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

internal object TimetableEventMapper {
    private const val VisibleDayCount = 5
    private const val FullDayThresholdMinutes = 23 * 60 + 59L

    fun map(
        events: List<EventSummaryResponse>,
        weekStart: LocalDate
    ): TimetableWeekEvents {
        val monday = weekStart.minusDays((weekStart.dayOfWeek.value - 1).toLong())
        val friday = monday.plusDays(VisibleDayCount - 1L)
        val timed = mutableListOf<TimetableEventItem>()
        val period = mutableListOf<TimetableTimelineEventItem>()
        val allDay = mutableListOf<TimetableTimelineEventItem>()

        events.distinctBy { it.id }.forEach { event ->
            val range = event.displayRange() ?: return@forEach
            if (range.end.dateTime.isBefore(range.start.dateTime)) return@forEach

            when {
                event.isPeriodEvent -> timelineItem(
                    event = event,
                    range = range,
                    weekStart = monday,
                    weekEnd = friday,
                    kind = TimetableTimelineEventKind.PERIOD
                )?.let(period::add)

                range.isAllDay() -> timelineItem(
                    event = event,
                    range = range,
                    weekStart = monday,
                    weekEnd = friday,
                    kind = TimetableTimelineEventKind.ALL_DAY
                )?.let(allDay::add)

                else -> {
                    val eventDate = range.start.dateTime.toLocalDate()
                    val weekday = ChronoUnit.DAYS.between(monday, eventDate).toInt()
                    if (weekday !in 0 until VisibleDayCount || range.end.dateTime.toLocalDate() != eventDate) {
                        return@forEach
                    }
                    val startMinute = range.start.dateTime.toMinuteOfDay()
                    val endMinute = range.end.dateTime.toMinuteOfDay()
                    if (endMinute <= startMinute) return@forEach

                    timed += TimetableEventItem(
                        id = "${event.id}-$eventDate",
                        eventId = event.id,
                        title = event.title,
                        weekday = weekday,
                        startMinute = startMinute,
                        endMinute = endMinute,
                        categoryColor = eventTypeColor(event.eventTypeId)
                    )
                }
            }
        }

        return TimetableWeekEvents(
            timed = timed.sortedWith(compareBy({ it.weekday }, { it.startMinute }, { it.endMinute }, { it.eventId })),
            period = period.sortedWith(compareBy({ it.startDate }, { it.endDate }, { it.eventId })),
            allDay = allDay.sortedWith(compareBy({ it.startDate }, { it.endDate }, { it.eventId }))
        )
    }

    private fun EventSummaryResponse.displayRange(): ParsedRange? {
        val startValue = if (isPeriodEvent) applyStart ?: eventStart else eventStart ?: applyStart
        val endValue = if (isPeriodEvent) applyEnd ?: eventEnd else eventEnd ?: applyEnd
        val start = parseEventDateTime(startValue) ?: return null
        val end = parseEventDateTime(endValue) ?: return null
        return ParsedRange(start = start, end = end)
    }

    private fun timelineItem(
        event: EventSummaryResponse,
        range: ParsedRange,
        weekStart: LocalDate,
        weekEnd: LocalDate,
        kind: TimetableTimelineEventKind
    ): TimetableTimelineEventItem? {
        val startDate = range.start.dateTime.toLocalDate()
        val endDate = range.timelineEndDate(kind)
        if (endDate.isBefore(weekStart) || startDate.isAfter(weekEnd)) return null

        return TimetableTimelineEventItem(
            id = "${event.id}-${kind.name}",
            eventId = event.id,
            title = event.title,
            startDate = startDate,
            endDate = endDate,
            categoryColor = eventTypeColor(event.eventTypeId),
            kind = kind
        )
    }

    private fun ParsedRange.isAllDay(): Boolean {
        if (start.isDateOnly || end.isDateOnly) return true
        if (start.dateTime.toLocalDate() != end.dateTime.toLocalDate()) return true
        return Duration.between(start.dateTime, end.dateTime).toMinutes() >= FullDayThresholdMinutes
    }

    private fun ParsedRange.timelineEndDate(kind: TimetableTimelineEventKind): LocalDate {
        val rawEndDate = end.dateTime.toLocalDate()
        if (
            kind == TimetableTimelineEventKind.ALL_DAY &&
            !end.isDateOnly &&
            end.dateTime.toLocalTime() == LocalTime.MIDNIGHT &&
            rawEndDate.isAfter(start.dateTime.toLocalDate())
        ) {
            return rawEndDate.minusDays(1)
        }
        return rawEndDate
    }

    private fun parseEventDateTime(value: String?): ParsedDateTime? {
        val normalized = value?.trim().orEmpty()
        if (normalized.isEmpty()) return null

        try {
            return ParsedDateTime(
                dateTime = OffsetDateTime.parse(normalized)
                    .atZoneSameInstant(HANGSHA_ZONE_ID)
                    .toLocalDateTime(),
                isDateOnly = false
            )
        } catch (_: DateTimeParseException) {
            // Try the remaining ISO-8601 formats below.
        }
        try {
            return ParsedDateTime(
                dateTime = ZonedDateTime.parse(normalized)
                    .withZoneSameInstant(HANGSHA_ZONE_ID)
                    .toLocalDateTime(),
                isDateOnly = false
            )
        } catch (_: DateTimeParseException) {
            // Try a local date-time next.
        }
        try {
            return ParsedDateTime(
                dateTime = LocalDateTime.parse(normalized),
                isDateOnly = false
            )
        } catch (_: DateTimeParseException) {
            // Finally accept a date-only server value.
        }
        return try {
            ParsedDateTime(
                dateTime = LocalDate.parse(normalized).atStartOfDay(),
                isDateOnly = true
            )
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun LocalDateTime.toMinuteOfDay(): Int = hour * 60 + minute

    private data class ParsedDateTime(
        val dateTime: LocalDateTime,
        val isDateOnly: Boolean
    )

    private data class ParsedRange(
        val start: ParsedDateTime,
        val end: ParsedDateTime
    )
}
