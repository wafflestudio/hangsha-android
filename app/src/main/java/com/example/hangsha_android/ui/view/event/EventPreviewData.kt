package com.example.hangsha_android.ui.view.event

import com.example.hangsha_android.data.network.model.EventSummaryResponse
import com.example.hangsha_android.ui.view.calendar.CalendarEvent
import com.example.hangsha_android.util.HANGSHA_ZONE_ID
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal data class EventPreviewData(
    val title: String,
    val schedule: String?,
    val organizationOrType: String?
)

internal fun CalendarEvent.toEventPreview(
    organizationNames: Map<Long, String>,
    eventTypeNames: Map<Long, String>
): EventPreviewData = EventPreviewData(
    title = title,
    schedule = formatPreviewSchedule(eventStart, eventEnd, applyStart, applyEnd, isPeriodEvent),
    organizationOrType = orgId?.let(organizationNames::get)?.takeIf { it.isNotBlank() }
        ?: organization?.takeIf { it.isNotBlank() && it != "-" }
        ?: eventTypeId?.let { eventTypeNames[it] ?: eventTypeLabel(it) }
)

internal fun EventSummaryResponse.toEventPreview(): EventPreviewData = EventPreviewData(
    title = title,
    schedule = formatPreviewSchedule(eventStart, eventEnd, applyStart, applyEnd, isPeriodEvent),
    organizationOrType = organization?.takeIf { it.isNotBlank() && it != "-" }
        ?: eventTypeId?.let(::eventTypeLabel)
)

private data class PreviewTime(val value: LocalDateTime, val dateOnly: Boolean)

private fun parsePreviewTime(raw: String?): PreviewTime? {
    val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    runCatching { OffsetDateTime.parse(text).atZoneSameInstant(HANGSHA_ZONE_ID).toLocalDateTime() }
        .getOrNull()?.let { return PreviewTime(it, false) }
    runCatching { ZonedDateTime.parse(text).withZoneSameInstant(HANGSHA_ZONE_ID).toLocalDateTime() }
        .getOrNull()?.let { return PreviewTime(it, false) }
    runCatching { LocalDateTime.parse(text) }.getOrNull()?.let { return PreviewTime(it, false) }
    return runCatching { PreviewTime(LocalDate.parse(text).atStartOfDay(), true) }.getOrNull()
}

/** Keep application dates explicitly labelled, including the fallback used by calendar blocks. */
internal fun formatPreviewSchedule(
    eventStart: String?,
    eventEnd: String?,
    applyStart: String?,
    applyEnd: String?,
    isPeriodEvent: Boolean
): String? {
    val event = parsePreviewTime(eventStart) to parsePreviewTime(eventEnd)
    val application = parsePreviewTime(applyStart) to parsePreviewTime(applyEnd)
    val hasEvent = event.first != null || event.second != null
    val hasApplication = application.first != null || application.second != null
    if (!hasEvent && !hasApplication) return null
    val useApplication = hasApplication && (isPeriodEvent || !hasEvent)
    val (start, end) = if (useApplication) application else event
    val prefix = if (useApplication) "신청 기간 · " else "행사 일정 · "
    val dateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    fun PreviewTime.label(): String = value.format(dateFormat) +
        if (dateOnly) "" else " " + value.format(timeFormat)

    if (start == null) return prefix + end!!.label() + "까지"
    if (end == null) return prefix + start.label() + "부터"
    if (start.value.toLocalDate() == end.value.toLocalDate()) {
        val date = start.value.format(dateFormat)
        if (start.dateOnly && end.dateOnly) return prefix + date + if (useApplication) "" else " · 종일"
        val allDay = !start.dateOnly && !end.dateOnly &&
            start.value.toLocalTime().toSecondOfDay() == 0 &&
            end.value.toLocalTime().toSecondOfDay() >= 23 * 3600 + 59 * 60
        if (allDay) return prefix + date + " · 종일"
        if (!start.dateOnly && !end.dateOnly) {
            val time = start.value.format(timeFormat)
            return prefix + date + " " + time +
                if (start.value == end.value) "" else "–" + end.value.format(timeFormat)
        }
    }
    return prefix + start.label() + " – " + end.label()
}
