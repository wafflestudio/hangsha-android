package com.example.hangsha_android.ui.view.eventdetail

import androidx.compose.ui.graphics.Color

data class EventDetailUiState(
    val eventId: Long = -1L,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val item: EventDetailItem? = null
)

data class EventDetailItem(
    val id: Long,
    val title: String,
    val imageUrl: String?,
    val organization: String?,
    val location: String?,
    val eventEndDisplay: String,
    val dDayLabel: String,
    val eventTypeLabel: String,
    val eventTypeColor: Color,
    val applyLink: String?,
    val detail: String?,
    val isBookmarked: Boolean
)
