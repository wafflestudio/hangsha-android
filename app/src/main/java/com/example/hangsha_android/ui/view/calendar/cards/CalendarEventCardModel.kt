package com.example.hangsha_android.ui.view.calendar.cards

import com.example.hangsha_android.data.repository.model.EventDateRange
import com.example.hangsha_android.ui.view.calendar.CalendarEvent
import com.example.hangsha_android.ui.view.calendar.parseCalendarEventDate
import com.example.hangsha_android.ui.view.event.formatApplicationDeadlineLabel
import com.example.hangsha_android.ui.view.event.formatEventCountdownLabel
import com.example.hangsha_android.ui.view.org.organizationLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class CalendarEventCardItem(
    val id: Long,
    val title: String,
    val imageUrl: String?,
    val organization: String,
    val eventTypeId: Long?,
    val eventTypeLabel: String,
    val eventDateDisplay: String,
    val applicationCountdownLabel: String,
    val eventCountdownLabel: String,
    val isBookmarked: Boolean
)

internal fun buildCalendarEventCardItems(
    contentRange: EventDateRange,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    organizationNames: Map<Long, String>,
    eventTypeNames: Map<Long, String>
): List<CalendarEventCardItem> {
    return eventsByDate.entries
        .asSequence()
        .sortedBy { (date, _) -> date }
        .flatMap { (_, events) -> events.asSequence() }
        .distinctBy { event -> event.id }
        .filter { event ->
            val range = event.displayRange()
            !range.end.isBefore(contentRange.from) && !range.start.isAfter(contentRange.to)
        }
        .map { event -> event.toCardItem(organizationNames, eventTypeNames) }
        .toList()
}

private fun CalendarEvent.toCardItem(
    organizationNames: Map<Long, String>,
    eventTypeNames: Map<Long, String>
): CalendarEventCardItem {
    val eventStartDate = parseCalendarEventDate(eventStart)
    val eventEndDate = parseCalendarEventDate(eventEnd)

    return CalendarEventCardItem(
        id = id,
        title = title,
        imageUrl = imageUrl,
        organization = organizationLabel(
            orgId = orgId,
            organizationNames = organizationNames,
            fallbackName = organization
        ),
        eventTypeId = eventTypeId,
        eventTypeLabel = eventTypeId
            ?.let(eventTypeNames::get)
            ?.takeIf { label -> label.isNotBlank() }
            ?: "기타",
        eventDateDisplay = formatCalendarCardDateRange(
            start = eventStartDate,
            end = eventEndDate,
            fallback = date
        ),
        applicationCountdownLabel = formatApplicationDeadlineLabel(
            parseCalendarEventDate(applyEnd)
        ),
        eventCountdownLabel = formatEventCountdownLabel(eventStartDate, eventEndDate),
        isBookmarked = isBookmarked
    )
}

private data class CalendarCardDateRange(
    val start: LocalDate,
    val end: LocalDate
)

private fun CalendarEvent.displayRange(): CalendarCardDateRange {
    val eventRange = normalizeCalendarCardRange(eventStart, eventEnd)
    val applicationRange = normalizeCalendarCardRange(applyStart, applyEnd)
    return if (isPeriodEvent) {
        applicationRange ?: eventRange
    } else {
        eventRange ?: applicationRange
    } ?: CalendarCardDateRange(date, date)
}

private fun normalizeCalendarCardRange(
    startValue: String?,
    endValue: String?
): CalendarCardDateRange? {
    val start = parseCalendarEventDate(startValue)
    val end = parseCalendarEventDate(endValue)
    val fallback = start ?: end ?: return null
    val normalizedStart = start ?: fallback
    val normalizedEnd = end?.takeUnless { it.isBefore(normalizedStart) } ?: normalizedStart
    return CalendarCardDateRange(normalizedStart, normalizedEnd)
}

private val CalendarCardFullDateFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA)
private val CalendarCardMonthDayFormatter =
    DateTimeFormatter.ofPattern("MM.dd", Locale.KOREA)

private fun formatCalendarCardDateRange(
    start: LocalDate?,
    end: LocalDate?,
    fallback: LocalDate
): String {
    val normalizedStart = start ?: end ?: fallback
    val normalizedEnd = end?.takeUnless { it.isBefore(normalizedStart) } ?: normalizedStart
    return when {
        normalizedStart == normalizedEnd -> normalizedStart.format(CalendarCardFullDateFormatter)
        normalizedStart.year == normalizedEnd.year ->
            "${normalizedStart.format(CalendarCardFullDateFormatter)} ~ ${normalizedEnd.format(CalendarCardMonthDayFormatter)}"
        else ->
            "${normalizedStart.format(CalendarCardFullDateFormatter)} ~ ${normalizedEnd.format(CalendarCardFullDateFormatter)}"
    }
}
