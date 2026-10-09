package com.example.hangsha_android.ui.view.calendar

import java.time.LocalDate

internal fun calendarEvent(
    id: Long = 1,
    date: LocalDate = LocalDate.of(2026, 10, 4),
    start: String? = null,
    end: String? = null,
    isPeriod: Boolean = false,
    applyStart: String? = null,
    applyEnd: String? = null
) = CalendarEvent(
    id = id,
    date = date,
    title = "Event $id",
    imageUrl = null,
    operationMode = null,
    statusId = 2,
    eventTypeId = 1,
    orgId = 1,
    applyStart = applyStart,
    applyEnd = applyEnd,
    eventStart = start,
    eventEnd = end,
    isPeriodEvent = isPeriod,
    capacity = null,
    applyCount = null,
    organization = null,
    location = null,
    applyLink = null,
    tags = null,
    isInterested = false,
    matchedInterestPriority = null,
    isBookmarked = false
)
