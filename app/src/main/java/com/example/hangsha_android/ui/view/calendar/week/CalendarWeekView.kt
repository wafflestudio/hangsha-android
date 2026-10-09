package com.example.hangsha_android.ui.view.calendar.week

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangsha_android.ui.components.eventPreviewClickable
import com.example.hangsha_android.ui.view.calendar.CalendarEvent
import com.example.hangsha_android.ui.view.event.eventTypeColor
import com.example.hangsha_android.util.currentHangshaDate
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.first

private val AxisWidth = 43.dp
private val MinimumDayWidth = 86.dp
private val DateHeaderHeight = 66.dp
private val AllDayLaneHeight = 32.dp
private val HourHeight = 64.dp

@Composable
internal fun CalendarWeekView(
    weekStart: LocalDate,
    initialDayIndex: Int,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    isLoading: Boolean,
    onOpenDayCalendar: (LocalDate) -> Unit,
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
    else AllDayLaneHeight * minOf(allDayLanes, 4) + 4.dp
    val allDayContentHeight = AllDayLaneHeight * allDayLanes + 4.dp
    val firstHour = remember(layout.timed) {
        val earliest = layout.timed.minOfOrNull { it.startMinute } ?: 7 * 60
        if (earliest < 7 * 60) (earliest / 60 - 1).coerceAtLeast(0)
        else maxOf(7, earliest / 60 - 2)
    }
    val lastHour = remember(layout.timed, firstHour) {
        val latest = layout.timed.maxOfOrNull { it.endMinute } ?: 24 * 60
        maxOf(firstHour + 1, minOf(24, (latest + 59) / 60 + 2))
    }
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val viewportWidth = (maxWidth - AxisWidth).coerceAtLeast(1.dp)
        val dayWidth = maxOf(MinimumDayWidth, viewportWidth / 7)
        val contentWidth = dayWidth * 7
        val timelineHeight = HourHeight * (lastHour - firstHour)

        LaunchedEffect(weekStart, initialDayIndex, dayWidth, viewportWidth) {
            if (contentWidth <= viewportWidth) return@LaunchedEffect
            val maxScroll = snapshotFlow { horizontalScroll.maxValue }.first { it > 0 }
            val target = with(density) {
                (dayWidth * (initialDayIndex.coerceIn(0, 6) + 0.5f) - viewportWidth / 2).toPx().toInt()
            }
            horizontalScroll.scrollTo(target.coerceIn(0, maxScroll))
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.width(AxisWidth).fillMaxHeight()) {
                Spacer(modifier = Modifier.height(DateHeaderHeight))
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
                            modifier = Modifier.offset(y = HourHeight * (hour - firstHour) - scrollOffset),
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
                    .horizontalScroll(horizontalScroll)
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

@Composable
private fun WeekDateHeader(
    weekStart: LocalDate,
    dayWidth: Dp,
    hiddenCounts: Map<Int, Int>,
    onOpenDayCalendar: (LocalDate) -> Unit
) {
    Row(modifier = Modifier.height(DateHeaderHeight)) {
        repeat(7) { dayIndex ->
            val date = weekStart.plusDays(dayIndex.toLong())
            val isToday = date == currentHangshaDate()
            val hiddenCount = hiddenCounts[dayIndex] ?: 0
            Box(
                modifier = Modifier.width(dayWidth).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.width(dayWidth)
                            .height(if (hiddenCount > 0) 38.dp else DateHeaderHeight)
                            .clickable { onOpenDayCalendar(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)}\n${date.dayOfMonth}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 12.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                    }
                    if (hiddenCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(top = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .semantics {
                                    contentDescription = "${date.monthValue}월 ${date.dayOfMonth}일 숨겨진 행사 ${hiddenCount}개, 일별 캘린더 열기"
                                }
                                .clickable { onOpenDayCalendar(date) }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$hiddenCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekAllDayLayer(
    positions: List<PositionedWeekAllDayEvent>,
    dayWidth: Dp,
    contentWidth: Dp,
    contentHeight: Dp,
    onEventClick: (Long) -> Unit
) {
    Box(modifier = Modifier.width(contentWidth).height(contentHeight)) {
        val gridColor = MaterialTheme.colorScheme.outlineVariant
        Canvas(modifier = Modifier.fillMaxSize()) {
            repeat(8) { index ->
                val x = dayWidth.toPx() * index
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            }
        }
        positions.forEach { position ->
            val event = position.event
            val width = dayWidth * (position.endDay - position.startDay + 1) - 4.dp
            val bandModifier = Modifier
                .offset(x = dayWidth * position.startDay + 2.dp, y = AllDayLaneHeight * position.lane + 2.dp)
                .width(width)
                .height(AllDayLaneHeight - 4.dp)
                .eventPreviewClickable(event.eventId) { onEventClick(event.eventId) }
            if (event.isPeriodEvent) {
                WeekPeriodArrow(event, position, bandModifier)
            } else {
                WeekEventBand(
                    title = event.title,
                    eventTypeId = event.eventTypeId,
                    maxLines = 1,
                    modifier = bandModifier
                )
            }
        }
    }
}

@Composable
private fun WeekPeriodArrow(
    event: WeekAllDayEvent,
    position: PositionedWeekAllDayEvent,
    modifier: Modifier
) {
    val color = eventTypeColor(event.eventTypeId)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val start = 3.dp.toPx()
            val end = size.width - 3.dp.toPx()
            val mid = size.height / 2f
            val arrow = 5.dp.toPx()
            drawLine(color, Offset(start, mid), Offset(end, mid), 2.dp.toPx(), StrokeCap.Round)
            drawLine(color, Offset(end, mid), Offset(end - arrow, mid - arrow), 2.dp.toPx())
            drawLine(color, Offset(end, mid), Offset(end - arrow, mid + arrow), 2.dp.toPx())
            if (position.continuesBeforeWeek) {
                drawLine(color, Offset(start, mid), Offset(start + arrow, mid - arrow), 2.dp.toPx())
                drawLine(color, Offset(start, mid), Offset(start + arrow, mid + arrow), 2.dp.toPx())
            }
        }
        Text(
            text = event.title,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                .padding(horizontal = 3.dp),
            color = color,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WeekEventBand(
    title: String,
    eventTypeId: Long?,
    maxLines: Int,
    modifier: Modifier
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(3.dp))
            .background(eventTypeColor(eventTypeId)),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WeekTimedLayer(
    dayWidth: Dp,
    contentWidth: Dp,
    contentHeight: Dp,
    firstHour: Int,
    lastHour: Int,
    positions: PositionedWeekTimedLayout,
    onEventClick: (Long) -> Unit
) {
    Box(modifier = Modifier.width(contentWidth).height(contentHeight)) {
        val lineColor = MaterialTheme.colorScheme.outlineVariant
        Canvas(modifier = Modifier.fillMaxSize()) {
            repeat(8) { index ->
                val x = dayWidth.toPx() * index
                drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            }
            for (minute in firstHour * 60..lastHour * 60 step 30) {
                val y = (minute - firstHour * 60) * size.height / ((lastHour - firstHour) * 60)
                drawLine(
                    lineColor.copy(alpha = if (minute % 60 == 0) 1f else 0.45f),
                    Offset(0f, y), Offset(size.width, y), 1.dp.toPx()
                )
            }
        }

        positions.visible.forEach { placed ->
            val event = placed.event
            val laneWidth = dayWidth / placed.laneCount
            val top = HourHeight * ((event.startMinute - firstHour * 60) / 60f)
            val duration = HourHeight * ((event.endMinute - event.startMinute) / 60f)
            WeekEventBand(
                title = event.title,
                eventTypeId = event.eventTypeId,
                maxLines = 3,
                modifier = Modifier
                    .offset(
                        x = dayWidth * placed.dayIndex + laneWidth * placed.lane + 2.dp,
                        y = top + 2.dp
                    )
                    .width((laneWidth - 4.dp).coerceAtLeast(1.dp))
                    .height((duration - 4.dp).coerceAtLeast(22.dp))
                    .eventPreviewClickable(event.eventId) { onEventClick(event.eventId) }
            )
        }
    }
}
