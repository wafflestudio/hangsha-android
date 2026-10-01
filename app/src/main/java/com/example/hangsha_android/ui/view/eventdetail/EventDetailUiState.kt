package com.example.hangsha_android.ui.view.eventdetail

import androidx.compose.ui.graphics.Color
import com.example.hangsha_android.ui.components.HangshaToastType

data class EventDetailUiState(
    val eventId: Long = -1L,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val item: EventDetailItem? = null,
    val isMemoEditorOpen: Boolean = false,
    val memoContent: String = "",
    val memoTagInput: String = "",
    val memoTagNames: List<String> = emptyList(),
    val savedMemo: EventDetailMemo? = null,
    val isMemoSaving: Boolean = false,
    val memoSaveMessage: String? = null,
    val memoSaveToastType: HangshaToastType = HangshaToastType.Info,
    val isBugReportDialogOpen: Boolean = false,
    val bugReportTitle: String = "",
    val bugReportContent: String = "",
    val isSubmittingBugReport: Boolean = false,
    val bugReportMessage: String? = null,
    val bugReportToastType: HangshaToastType = HangshaToastType.Info
)

data class EventDetailMemo(
    val id: Long,
    val eventId: Long,
    val content: String,
    val tagNames: List<String>
)

data class EventDetailItem(
    val id: Long,
    val title: String,
    val imageUrl: String?,
    val organization: String?,
    val location: String?,
    val eventPeriodDisplay: String,
    val applyPeriodDisplay: String,
    val dDayLabel: String,
    val eventDDayLabel: String,
    val eventTypeLabel: String,
    val eventTypeColor: Color,
    val applyLink: String?,
    val detail: String?,
    val isBookmarked: Boolean
)
