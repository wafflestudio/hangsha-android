package com.example.hangsha_android.ui.view.calendar.week

import com.example.hangsha_android.ui.view.calendar.CalendarEvent
import com.example.hangsha_android.ui.view.calendar.timelineRange
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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

            val range = event.timelineRange()
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

    private fun LocalDateTime.toMinuteOfDay(): Int = hour * 60 + minute
    private fun isFullDay(start: Int, end: Int): Boolean = start == 0 && end >= 23 * 60 + 59

    private fun datesBetween(from: LocalDate, to: LocalDate): List<LocalDate> {
        if (to.isBefore(from)) return emptyList()
        return (0L..ChronoUnit.DAYS.between(from, to)).map { from.plusDays(it) }
    }

}
