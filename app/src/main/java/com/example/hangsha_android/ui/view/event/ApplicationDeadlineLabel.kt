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
