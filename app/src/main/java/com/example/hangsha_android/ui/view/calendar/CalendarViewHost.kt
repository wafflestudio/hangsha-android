package com.example.hangsha_android.ui.view.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.time.LocalDate

@Composable
internal fun CalendarViewHost(
    uiState: CalendarUiState,
    eventCardItems: List<CalendarEventCardItem>,
    showBookmarkAction: Boolean,
    onDateClick: (LocalDate) -> Unit,
    onEventClick: (Long) -> Unit,
    onBookmarkClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState.viewMode) {
        CalendarViewMode.LIST -> {
            CalendarListView(
                items = eventCardItems,
                isLoading = uiState.isLoading,
                showBookmarkAction = showBookmarkAction,
                onEventClick = onEventClick,
                onBookmarkClick = onBookmarkClick,
                modifier = modifier
            )
        }

        CalendarViewMode.GRID -> {
            CalendarGridView(
                items = eventCardItems,
                isLoading = uiState.isLoading,
                showBookmarkAction = showBookmarkAction,
                onEventClick = onEventClick,
                onBookmarkClick = onBookmarkClick,
                modifier = modifier
            )
        }

        CalendarViewMode.CALENDAR -> {
            CalendarTemporalViewHost(
                uiState = uiState,
                onDateClick = onDateClick,
                onEventClick = onEventClick,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun CalendarTemporalViewHost(
    uiState: CalendarUiState,
    onDateClick: (LocalDate) -> Unit,
    onEventClick: (Long) -> Unit,
    modifier: Modifier
) {
    when (uiState.period) {
        CalendarPeriod.MONTH -> {
            CalendarMonthView(
                visibleDates = uiState.visibleDates,
                currentMonth = uiState.currentMonth,
                eventsByDate = uiState.eventsByDate,
                isLoading = uiState.isLoading,
                onDateClick = onDateClick,
                onEventClick = onEventClick,
                modifier = modifier
            )
        }

        CalendarPeriod.WEEK -> {
            FutureCalendarPeriodView(
                message = "주별 일정 보기를 준비 중입니다.",
                modifier = modifier
            )
        }

        CalendarPeriod.DAY -> {
            FutureCalendarPeriodView(
                message = "일별 일정 보기를 준비 중입니다.",
                modifier = modifier
            )
        }
    }
}

@Composable
private fun FutureCalendarPeriodView(
    message: String,
    modifier: Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
