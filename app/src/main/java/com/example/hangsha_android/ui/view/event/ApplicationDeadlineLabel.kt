package com.example.hangsha_android.ui.view.event

import com.example.hangsha_android.util.currentHangshaDate
import java.time.LocalDate

internal fun formatApplicationDeadlineLabel(
    deadline: LocalDate?,
    today: LocalDate = currentHangshaDate()
): String {
    val targetDate = deadline ?: return "-"
    val daysRemaining = targetDate.toEpochDay() - today.toEpochDay()

    return when {
        daysRemaining > 0L -> "지원 D-$daysRemaining"
        daysRemaining == 0L -> "지원 D-DAY"
        else -> "지원 마감"
    }
}

internal fun formatEventCountdownLabel(
    eventStart: LocalDate?,
    eventEnd: LocalDate?,
    today: LocalDate = currentHangshaDate()
): String {
    val startDate = eventStart ?: return "-"
    val endDate = eventEnd?.takeUnless { it.isBefore(startDate) } ?: startDate

    return when {
        today.isBefore(startDate) -> {
            val daysRemaining = startDate.toEpochDay() - today.toEpochDay()
            "행사 D-$daysRemaining"
        }
        today == startDate -> "행사 D-DAY"
        !today.isAfter(endDate) -> "행사 진행중"
        else -> "행사 종료"
    }
}

internal data class CountdownLabelDisplay(
    val text: String,
    val canToggle: Boolean,
    val toggleActionLabel: String
)

internal fun resolveCountdownLabel(
    applicationLabel: String,
    eventLabel: String,
    showEvent: Boolean
): CountdownLabelDisplay {
    val hasApplicationLabel = applicationLabel != "-"
    val hasEventLabel = eventLabel != "-"
    val isShowingEvent = hasEventLabel && (showEvent || !hasApplicationLabel)

    return CountdownLabelDisplay(
        text = if (isShowingEvent) eventLabel else applicationLabel,
        canToggle = hasApplicationLabel && hasEventLabel,
        toggleActionLabel = if (isShowingEvent) "지원 일정 보기" else "행사 일정 보기"
    )
}
