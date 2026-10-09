package com.example.hangsha_android.ui.view.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.hangsha_android.ui.components.EventPreviewHost
import com.example.hangsha_android.ui.view.event.toEventPreview
import com.example.hangsha_android.ui.view.calendar.grid.CalendarGridView
import com.example.hangsha_android.ui.view.calendar.day.CalendarDayView
import com.example.hangsha_android.ui.view.calendar.list.CalendarListView
import com.example.hangsha_android.ui.view.calendar.month.CalendarMonthView
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun CalendarViewHost(
    period: CalendarPeriod,
    anchorDate: LocalDate,
    viewMode: CalendarViewMode,
    page: CalendarPeriodPage,
    organizationNames: Map<Long, String>,
    eventTypeNames: Map<Long, String>,
    showBookmarkAction: Boolean,
    onDateClick: (LocalDate) -> Unit,
    onEventClick: (Long) -> Unit,
    onBookmarkClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val eventCardItems = if (viewMode == CalendarViewMode.CALENDAR) {
        emptyList()
    } else {
        remember(period, anchorDate, page.eventsByDate, organizationNames, eventTypeNames) {
            buildCalendarEventCardItems(
                contentRange = period.contentRange(anchorDate),
                eventsByDate = page.eventsByDate,
                organizationNames = organizationNames,
                eventTypeNames = eventTypeNames
            )
        }
    }
    val emptyMessage = when (period) {
        CalendarPeriod.MONTH -> "이 달에 표시할 행사가 없습니다."
        CalendarPeriod.WEEK -> "이 주에 표시할 행사가 없습니다."
        CalendarPeriod.DAY -> "이 날에 표시할 행사가 없습니다."
    }

    when (viewMode) {
        CalendarViewMode.LIST -> {
            CalendarListView(
                items = eventCardItems,
                isLoading = page.isLoading,
                emptyMessage = emptyMessage,
                showBookmarkAction = showBookmarkAction,
                onEventClick = onEventClick,
                onBookmarkClick = onBookmarkClick,
                modifier = modifier
            )
        }

        CalendarViewMode.GRID -> {
            CalendarGridView(
                items = eventCardItems,
                isLoading = page.isLoading,
                emptyMessage = emptyMessage,
                showBookmarkAction = showBookmarkAction,
                onEventClick = onEventClick,
                onBookmarkClick = onBookmarkClick,
                modifier = modifier
            )
        }

        CalendarViewMode.CALENDAR -> {
            val previews = remember(page.eventsByDate, organizationNames, eventTypeNames) {
                page.eventsByDate.values.flatten().distinctBy { it.id }.associate { event ->
                    event.id to event.toEventPreview(organizationNames, eventTypeNames)
                }
            }
            EventPreviewHost(events = previews, modifier = modifier) {
                CalendarTemporalViewHost(
                    period = period,
                    anchorDate = anchorDate,
                    page = page,
                    onDateClick = onDateClick,
                    onEventClick = onEventClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CalendarTemporalViewHost(
    period: CalendarPeriod,
    anchorDate: LocalDate,
    page: CalendarPeriodPage,
    onDateClick: (LocalDate) -> Unit,
    onEventClick: (Long) -> Unit,
    modifier: Modifier
) {
    when (period) {
        CalendarPeriod.MONTH -> {
            CalendarMonthView(
                visibleDates = period.visibleRange(anchorDate).toDateList(),
                currentMonth = YearMonth.from(anchorDate),
                eventsByDate = page.eventsByDate,
                isLoading = page.isLoading,
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
            CalendarDayView(
                date = anchorDate,
                events = page.eventsByDate[anchorDate].orEmpty(),
                isLoading = page.isLoading,
                onEventClick = onEventClick,
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
