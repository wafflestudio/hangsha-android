package com.example.hangsha_android.ui.view.dailyevents

import androidx.compose.ui.graphics.Color
import com.example.hangsha_android.ui.theme.Coral60
import com.example.hangsha_android.ui.theme.Mint70
import java.time.LocalDate

data class DailyEventsUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val items: List<DailyEventItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class DailyEventItem(
    val id: Long,
    val title: String,
    val organization: String?,
    val displayDate: String,
    val dDayLabel: String,
    val accentColor: Color,
    val isBookmarked: Boolean
)

internal val DailyEventAccentPalette = listOf(
    Color(0xFF8FD3F4),
    Color(0xFFFFDB6E),
    Color(0xFFBDA2FF),
    Mint70,
    Coral60
)
