package com.example.hangsha_android.ui.view.calendar

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangsha_android.ui.theme.PureWhite
import com.example.hangsha_android.ui.view.event.eventTypeColor
import com.example.hangsha_android.util.currentHangshaDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private const val CalendarDayCount = 7
private const val MaxVisibleEventLanes = 4
private const val DayCellAspectRatio = 0.4f
private val DayCellHorizontalGap = 3.dp
private val WeekVerticalGap = 5.dp
private val DayCellCornerRadius = 3.dp
private val EventAreaTop = 29.dp
private val EventLaneHeight = 15.dp
private val EventLaneGap = 2.dp
private val EventHorizontalInset = 3.dp
private val EventOverflowHeight = 11.dp

private val DayCardShadow = Color(0x16000000)
private val DayCellBorder = Color(0xFFE6E8EB)
private val OutOfMonthText = Color(0xFFC9CDD3)
private val DayRed = Color(0xFFFF2D55)
private val EventTitleText = Color(0xFF1F2937)

@Composable
internal fun ConnectedCalendarMonthGrid(
    visibleDates: List<LocalDate>,
    currentMonth: YearMonth,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    onDateClick: (LocalDate) -> Unit,
    onEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthEvents = remember(eventsByDate) {
        CalendarMonthEventMapper.map(eventsByDate)
    }
    val weeks = visibleDates.chunked(size = CalendarDayCount)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WeekVerticalGap)
    ) {
        weeks.forEach { week ->
            if (week.size == CalendarDayCount) {
                ConnectedCalendarWeek(
                    week = week,
                    currentMonth = currentMonth,
                    monthEvents = monthEvents,
                    onDateClick = onDateClick,
                    onEventClick = onEventClick
                )
            }
        }
    }
}

@Composable
private fun ConnectedCalendarWeek(
    week: List<LocalDate>,
    currentMonth: YearMonth,
    monthEvents: List<CalendarMonthEventItem>,
    onDateClick: (LocalDate) -> Unit,
    onEventClick: (Long) -> Unit
) {
    val layout = remember(monthEvents, week.first()) {
        CalendarMonthEventLayoutCalculator.positionWeek(
            events = monthEvents,
            weekStart = week.first(),
            maxVisibleLanes = MaxVisibleEventLanes
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val dayWidth = (maxWidth - DayCellHorizontalGap * (CalendarDayCount - 1)) /
            CalendarDayCount
        val dayStride = dayWidth + DayCellHorizontalGap
        val weekHeight = dayWidth / DayCellAspectRatio

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(weekHeight)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(DayCellHorizontalGap)
            ) {
                week.forEach { date ->
                    ConnectedCalendarDayCell(
                        date = date,
                        currentMonth = currentMonth,
                        onClick = { onDateClick(date) },
                        modifier = Modifier
                            .width(dayWidth)
                            .fillMaxHeight()
                    )
                }
            }

            layout.visibleEvents.forEach { positioned ->
                val startInset = if (positioned.continuesBeforeWeek) 0.dp else EventHorizontalInset
                val endInset = if (positioned.continuesAfterWeek) 0.dp else EventHorizontalInset
                val startX = dayStride * positioned.startDay + startInset
                val endX = dayStride * positioned.endDay + dayWidth - endInset
                val top = EventAreaTop +
                    (EventLaneHeight + EventLaneGap) * positioned.laneIndex

                CalendarMonthEventSegment(
                    positioned = positioned,
                    onClick = { onEventClick(positioned.event.eventId) },
                    modifier = Modifier
                        .offset(x = startX, y = top)
                        .width(endX - startX)
                        .height(EventLaneHeight)
                )
            }

            layout.overflowByDay.forEachIndexed { dayIndex, overflowCount ->
                if (overflowCount > 0) {
                    val top = EventAreaTop +
                        (EventLaneHeight + EventLaneGap) * MaxVisibleEventLanes
                    Box(
                        modifier = Modifier
                            .offset(
                                x = dayStride * dayIndex + EventHorizontalInset,
                                y = top
                            )
                            .width(dayWidth - EventHorizontalInset * 2)
                            .height(EventOverflowHeight)
                            .clickable { onDateClick(week[dayIndex]) },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "+$overflowCount",
                            color = EventTitleText.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            lineHeight = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedCalendarDayCell(
    date: LocalDate,
    currentMonth: YearMonth,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = currentHangshaDate()
    val isCurrentMonth = YearMonth.from(date) == currentMonth
    val contentAlpha = when {
        date == today -> 1f
        date.dayOfWeek == DayOfWeek.SUNDAY -> 0.5f
        else -> 0.3f
    }
    val dayTextColor = when {
        !isCurrentMonth -> OutOfMonthText
        date.dayOfWeek == DayOfWeek.SUNDAY -> DayRed
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .drawBehind {
                val radius = DayCellCornerRadius.toPx()
                val shadowOffset = 4.dp.toPx()
                val blurRadius = 2.dp.toPx()

                drawIntoCanvas { canvas ->
                    val paint = Paint()
                    paint.asFrameworkPaint().apply {
                        color = DayCardShadow.toArgb()
                        maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.drawRoundRect(
                        left = shadowOffset,
                        top = shadowOffset,
                        right = size.width + shadowOffset,
                        bottom = size.height + shadowOffset,
                        radiusX = radius,
                        radiusY = radius,
                        paint = paint
                    )
                }
            }
            .background(PureWhite, RoundedCornerShape(DayCellCornerRadius))
            .border(0.dp, DayCellBorder, RoundedCornerShape(DayCellCornerRadius))
            .padding(horizontal = 3.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier.size(width = 20.dp, height = 21.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = dayTextColor.copy(alpha = contentAlpha)
            )
        }
    }
}

@Composable
private fun CalendarMonthEventSegment(
    positioned: PositionedCalendarMonthEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = eventTypeColor(positioned.event.eventTypeId)
    if (positioned.event.isPeriodEvent) {
        PeriodEventBand(
            title = positioned.event.title,
            color = color,
            onClick = onClick,
            modifier = modifier
        )
    } else {
        BlockEventBand(
            title = positioned.event.title,
            color = color,
            onClick = onClick,
            modifier = modifier
        )
    }
}

@Composable
private fun BlockEventBand(
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = EventTitleText,
            fontSize = 8.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PeriodEventBand(
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp),
            color = EventTitleText,
            fontSize = 8.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .align(Alignment.BottomCenter)
        ) {
            val centerY = size.height / 2f
            val arrowWidth = minOf(4.dp.toPx(), size.width / 3f)
            val arrowHeight = 3.dp.toPx()
            val strokeWidth = 2.dp.toPx()

            drawLine(
                color = color,
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = strokeWidth
            )
            if (size.width > arrowWidth * 2f) {
                drawPath(
                    path = Path().apply {
                        moveTo(0f, centerY)
                        lineTo(arrowWidth, centerY - arrowHeight)
                        lineTo(arrowWidth, centerY + arrowHeight)
                        close()
                    },
                    color = color
                )
                drawPath(
                    path = Path().apply {
                        moveTo(size.width, centerY)
                        lineTo(size.width - arrowWidth, centerY - arrowHeight)
                        lineTo(size.width - arrowWidth, centerY + arrowHeight)
                        close()
                    },
                    color = color
                )
            }
        }
    }
}
