package com.example.hangsha_android.ui.view.eventdetail

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
    val operationMode: String,
    val applyPeriodText: String,
    val eventPeriodText: String,
    val statusText: String,
    val capacityText: String,
    val applyCountText: String,
    val tags: List<String>,
    val applyLink: String?,
    val detail: String?,
    val isBookmarked: Boolean,
    val isInterested: Boolean
)
