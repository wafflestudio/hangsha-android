package com.example.hangsha_android.ui.view.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hangsha_android.util.currentHangshaDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private val WeekdayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

@Composable
internal fun CalendarMonthView(
    visibleDates: List<LocalDate>,
    currentMonth: YearMonth,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    isLoading: Boolean,
    onDateClick: (LocalDate) -> Unit,
    onEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        WeekdayHeader()
        Spacer(modifier = Modifier.height(30.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            ConnectedCalendarMonthGrid(
                visibleDates = visibleDates,
                currentMonth = currentMonth,
                eventsByDate = eventsByDate,
                onDateClick = onDateClick,
                onEventClick = onEventClick,
                modifier = Modifier.fillMaxWidth()
            )

            if (isLoading) {
                CalendarLoadingOverlay()
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val todayIndex = when (currentHangshaDate().dayOfWeek) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        WeekdayLabels.forEachIndexed { index, label ->
            val isToday = index == todayIndex
            val isSunday = index == 0
            val textAlpha = when {
                isToday -> 1f
                isSunday -> 0.5f
                else -> 0.3f
            }
            val baseColor = if (isSunday) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = baseColor.copy(alpha = textAlpha)
                )
            }
        }
    }
}

@Composable
private fun CalendarLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
