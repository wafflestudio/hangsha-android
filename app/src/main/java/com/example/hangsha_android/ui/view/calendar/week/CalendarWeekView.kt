package com.example.hangsha_android.ui.view.calendar.week

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.hangsha_android.ui.view.calendar.CalendarEvent
import java.time.LocalDate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

private val AxisWidth = 43.dp
private val MinimumDayWidth = 86.dp

@Composable
internal fun CalendarWeekView(
    weekStart: LocalDate,
    initialDayIndex: Int,
    horizontalScroll: ScrollState,
    isCurrentPage: Boolean,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    isLoading: Boolean,
    onOpenDayCalendar: (LocalDate) -> Unit,
    onVisibleDayChange: (LocalDate) -> Unit,
    onEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val layout = remember(weekStart, eventsByDate) {
        CalendarWeekEventMapper.map(weekStart, eventsByDate)
    }
    val allDayPositions = remember(weekStart, layout.allDay) {
        positionWeekAllDayEvents(weekStart, layout.allDay)
    }
    val timedPositions = remember(weekStart, layout.timed) {
        positionWeekTimedEvents(weekStart, layout.timed)
    }
    val hiddenCounts = remember(timedPositions.overflow) {
        timedPositions.overflow.groupBy { it.dayIndex }
            .mapValues { (_, groups) -> groups.sumOf { it.hiddenCount } }
    }
    val allDayLanes = (allDayPositions.maxOfOrNull { it.lane } ?: -1) + 1
    val allDayViewportHeight = if (allDayLanes == 0) 0.dp
    else WeekAllDayLaneHeight * minOf(allDayLanes, 4) + 4.dp
    val allDayContentHeight = WeekAllDayLaneHeight * allDayLanes + 4.dp
    val firstHour = remember(layout.timed) {
        val earliest = layout.timed.minOfOrNull { it.startMinute } ?: 7 * 60
        if (earliest < 7 * 60) (earliest / 60 - 1).coerceAtLeast(0)
        else maxOf(7, earliest / 60 - 2)
    }
    val lastHour = remember(layout.timed, firstHour) {
        val latest = layout.timed.maxOfOrNull { it.endMinute } ?: 24 * 60
        maxOf(firstHour + 1, minOf(24, (latest + 59) / 60 + 2))
    }
    val verticalScroll = rememberScrollState()
    val density = LocalDensity.current
    val currentIsCurrentPage = rememberUpdatedState(isCurrentPage)
    val currentOnVisibleDayChange = rememberUpdatedState(onVisibleDayChange)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val viewportWidth = (maxWidth - AxisWidth).coerceAtLeast(1.dp)
        val dayWidth = maxOf(MinimumDayWidth, viewportWidth / 7)
        val contentWidth = dayWidth * 7
        val timelineHeight = WeekHourHeight * (lastHour - firstHour)

        LaunchedEffect(weekStart, initialDayIndex, dayWidth, viewportWidth) {
            if (contentWidth > viewportWidth) {
                // ScrollState uses Int.MAX_VALUE until the first measurement. Wait for
                // the real bound so its later clamp is not reported as a user's date change.
                val maxScroll = snapshotFlow { horizontalScroll.maxValue }
                    .first { it != Int.MAX_VALUE }
                val target = with(density) {
                    (dayWidth * (initialDayIndex.coerceIn(0, 6) + 0.5f) - viewportWidth / 2)
                        .toPx().toInt()
                }
                horizontalScroll.scrollTo(target.coerceIn(0, maxScroll))
            }

            // The initial positioning is not a user selection. Track the centered day
            // only after a later horizontal scroll comes to rest.
            var lastReportedOffset = horizontalScroll.value
            snapshotFlow { horizontalScroll.isScrollInProgress }
                .distinctUntilChanged()
                .drop(1)
                .filter { !it }
                .collect {
                    if (horizontalScroll.value == lastReportedOffset) return@collect
                    lastReportedOffset = horizontalScroll.value
                    if (currentIsCurrentPage.value) {
                        val dayIndex = with(density) {
                            ((horizontalScroll.value + viewportWidth.toPx() / 2) / dayWidth.toPx())
                                .toInt().coerceIn(0, 6)
                        }
                        currentOnVisibleDayChange.value(weekStart.plusDays(dayIndex.toLong()))
                    }
                }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.width(AxisWidth).fillMaxHeight()
            ) {
                Spacer(modifier = Modifier.height(WeekDateHeaderHeight))
                if (allDayLanes > 0) {
                    Box(
                        modifier = Modifier.height(allDayViewportHeight),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(
                            text = "종일",
                            modifier = Modifier.padding(top = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(modifier = Modifier.weight(1f).clipToBounds()) {
                    val scrollOffset = with(density) { verticalScroll.value.toDp() }
                    for (hour in firstHour until lastHour) {
                        Text(
                            text = "%02d:00".format(hour),
                            modifier = Modifier.offset(y = WeekHourHeight * (hour - firstHour) - scrollOffset),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(horizontalScroll, enabled = false, overscrollEffect = null)
                    .width(contentWidth)
            ) {
                WeekDateHeader(
                    weekStart = weekStart,
                    dayWidth = dayWidth,
                    hiddenCounts = hiddenCounts,
                    onOpenDayCalendar = onOpenDayCalendar
                )
                if (allDayLanes > 0) {
                    Box(
                        modifier = Modifier
                            .width(contentWidth)
                            .height(allDayViewportHeight)
                            .verticalScroll(rememberScrollState())
                    ) {
                        WeekAllDayLayer(
                            positions = allDayPositions,
                            dayWidth = dayWidth,
                            contentWidth = contentWidth,
                            contentHeight = allDayContentHeight,
                            onEventClick = onEventClick
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(
                    modifier = Modifier
                        .width(contentWidth)
                        .weight(1f)
                        .verticalScroll(verticalScroll)
                ) {
                    WeekTimedLayer(
                        dayWidth = dayWidth,
                        contentWidth = contentWidth,
                        contentHeight = timelineHeight,
                        firstHour = firstHour,
                        lastHour = lastHour,
                        positions = timedPositions,
                        onEventClick = onEventClick
                    )
                }
            }
        }

        if (!isLoading && layout.allDay.isEmpty() && layout.timed.isEmpty()) {
            Text(
                text = "이번 주에 표시할 행사가 없습니다.",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
