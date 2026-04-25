package com.example.hangsha_android.ui.view.calendar

import java.time.LocalDate

data class CalendarEvent(
    val id: Long,
    val date: LocalDate,
    val title: String,
    val imageUrl: String?,
    val operationMode: String,
    val statusId: Long,
    val eventTypeId: Long,
    val orgId: Long,
    val applyStart: String?,
    val applyEnd: String?,
    val eventStart: String?,
    val eventEnd: String?,
    val capacity: Int?,
    val applyCount: Int?,
    val organization: String?,
    val location: String?,
    val applyLink: String?,
    val tags: String?,
    val isInterested: Boolean,
    val matchedInterestPriority: Int?,
    val isBookmarked: Boolean
)
