package com.example.hangsha_android.ui.view.calendar.week

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangsha_android.ui.components.eventPreviewClickable
import com.example.hangsha_android.ui.view.event.eventTypeColor
import com.example.hangsha_android.util.currentHangshaDate
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

internal val WeekDateHeaderHeight = 66.dp
internal val WeekAllDayLaneHeight = 32.dp
internal val WeekHourHeight = 64.dp

@Composable
internal fun WeekDateHeader(
    weekStart: LocalDate,
    dayWidth: Dp,
    hiddenCounts: Map<Int, Int>,
    onOpenDayCalendar: (LocalDate) -> Unit
) {
    Row(modifier = Modifier.height(WeekDateHeaderHeight)) {
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
                            .height(if (hiddenCount > 0) 38.dp else WeekDateHeaderHeight)
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
internal fun WeekAllDayLayer(
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
                .offset(x = dayWidth * position.startDay + 2.dp, y = WeekAllDayLaneHeight * position.lane + 2.dp)
                .width(width)
                .height(WeekAllDayLaneHeight - 4.dp)
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
internal fun WeekTimedLayer(
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
            val top = WeekHourHeight * ((event.startMinute - firstHour * 60) / 60f)
            val duration = WeekHourHeight * ((event.endMinute - event.startMinute) / 60f)
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
