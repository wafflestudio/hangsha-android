package com.example.hangsha_android.ui.view.timetable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.PureWhite
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private const val TimelineDayCount = 5
private val TimelinePeekHeight = 34.dp
private val TimelineAxisLeadingWidth = 26.dp
private val PeriodLaneHeight = 36.dp
private val AllDayLaneHeight = 30.dp
private val TimelineGridColor = Color(0xFFECECEC)
private val TimelineHandleColor = Color(0xFFB8B8B8)

@Composable
internal fun TimetableEventTimelineSheet(
    weekStart: LocalDate,
    periodEvents: List<TimetableTimelineEventItem>,
    allDayEvents: List<TimetableTimelineEventItem>,
    expanded: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onExpandedChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val expandedHeight = minOf(420.dp, maxOf(300.dp, maxHeight * 0.62f)).coerceAtMost(maxHeight)
        val collapsedOffset = with(density) {
            (expandedHeight - TimelinePeekHeight).coerceAtLeast(0.dp).toPx()
        }
        var isDragging by remember { mutableStateOf(false) }
        var dragOffset by remember(collapsedOffset) { mutableFloatStateOf(collapsedOffset) }
        val targetOffset = when {
            isDragging -> dragOffset
            expanded -> 0f
            else -> collapsedOffset
        }
        val displayedOffset by animateFloatAsState(
            targetValue = targetOffset,
            label = "timetable timeline offset"
        )
        val dragState = rememberDraggableState { delta ->
            dragOffset = (dragOffset + delta).coerceIn(0f, collapsedOffset)
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(expandedHeight)
                .offset { IntOffset(x = 0, y = displayedOffset.roundToInt()) },
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            color = PureWhite.copy(alpha = 0.96f),
            shadowElevation = 10.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TimelineDragHandle(
                    expanded = expanded,
                    dragState = dragState,
                    onDragStarted = {
                        dragOffset = if (expanded) 0f else collapsedOffset
                        isDragging = true
                    },
                    onDragStopped = { velocity ->
                        val shouldExpand = when {
                            velocity < -700f -> true
                            velocity > 700f -> false
                            else -> dragOffset < collapsedOffset / 2f
                        }
                        isDragging = false
                        onExpandedChange(shouldExpand)
                    },
                    onClick = { onExpandedChange(!expanded) }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
                ) {
                    TimelineDateHeader(weekStart = weekStart)
                    Spacer(modifier = Modifier.height(6.dp))

                    when {
                        isLoading -> TimelineLoadingState()
                        errorMessage != null -> TimelineErrorState(
                            message = errorMessage,
                            onRetry = onRetry
                        )
                        periodEvents.isEmpty() && allDayEvents.isEmpty() -> TimelineEmptyState()
                        else -> {
                            if (periodEvents.isNotEmpty()) {
                                TimelineSection(
                                    events = periodEvents,
                                    weekStart = weekStart,
                                    isPeriod = true,
                                    onEventClick = onEventClick
                                )
                            }
                            if (periodEvents.isNotEmpty() && allDayEvents.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            if (allDayEvents.isNotEmpty()) {
                                TimelineSection(
                                    events = allDayEvents,
                                    weekStart = weekStart,
                                    isPeriod = false,
                                    onEventClick = onEventClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineDragHandle(
    expanded: Boolean,
    dragState: androidx.compose.foundation.gestures.DraggableState,
    onDragStarted: () -> Unit,
    onDragStopped: (Float) -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TimelinePeekHeight)
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStarted = { onDragStarted() },
                onDragStopped = { velocity -> onDragStopped(velocity) }
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "행사 타임라인 패널"
                stateDescription = if (expanded) "펼쳐짐" else "접힘"
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(TimelineHandleColor)
        )
    }
}

@Composable
private fun TimelineDateHeader(weekStart: LocalDate) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(TimelineAxisLeadingWidth))
        repeat(TimelineDayCount) { dayIndex ->
            val date = weekStart.plusDays(dayIndex.toLong())
            Text(
                text = "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)}\n${date.dayOfMonth}",
                modifier = Modifier.weight(1f),
                color = Ink60,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TimelineSection(
    events: List<TimetableTimelineEventItem>,
    weekStart: LocalDate,
    isPeriod: Boolean,
    onEventClick: (Long) -> Unit
) {
    val positions = remember(events, weekStart) {
        TimetableTimelineLayoutCalculator.positionBlocks(
            blocks = events.map { event ->
                TimetableTimelineBlock(
                    id = event.id,
                    startDate = event.startDate,
                    endDate = event.endDate
                )
            },
            weekStart = weekStart,
            dayCount = TimelineDayCount
        )
    }
    val positionById = remember(positions) { positions.associateBy { it.id } }
    val laneHeight = if (isPeriod) PeriodLaneHeight else AllDayLaneHeight
    val laneCount = positions.maxOfOrNull { it.laneCount } ?: 1

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(laneHeight * laneCount + 4.dp)
    ) {
        val dayWidth = (maxWidth - TimelineAxisLeadingWidth) / TimelineDayCount
        TimelineVerticalGrid()
        events.forEach { event ->
            val position = positionById[event.id] ?: return@forEach
            val x = TimelineAxisLeadingWidth + dayWidth * position.startDay + 2.dp
            val width = dayWidth * (position.endDay - position.startDay + 1) - 4.dp
            val y = laneHeight * position.laneIndex

            if (isPeriod) {
                PeriodArrow(
                    event = event,
                    position = position,
                    modifier = Modifier
                        .offset(x = x, y = y)
                        .width(width)
                        .height(PeriodLaneHeight)
                        .clickable { onEventClick(event.eventId) }
                )
            } else {
                AllDayBand(
                    event = event,
                    modifier = Modifier
                        .offset(x = x, y = y + 2.dp)
                        .width(width)
                        .height(24.dp)
                        .clickable { onEventClick(event.eventId) }
                )
            }
        }
    }
}

@Composable
private fun TimelineVerticalGrid() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val leading = TimelineAxisLeadingWidth.toPx()
        val dayWidth = (size.width - leading) / TimelineDayCount
        repeat(TimelineDayCount + 1) { index ->
            val x = leading + dayWidth * index
            drawLine(
                color = TimelineGridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
private fun PeriodArrow(
    event: TimetableTimelineEventItem,
    position: PositionedTimelineBlock,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.semantics {
            contentDescription = "${event.title}, 기간제 행사"
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val color = event.categoryColor.copy(alpha = 1f)
            val y = size.height / 2f
            val startX = 3.dp.toPx()
            val endX = size.width - 3.dp.toPx()
            val arrow = 5.dp.toPx()
            drawLine(color, Offset(startX, y), Offset(endX, y), 2.dp.toPx(), StrokeCap.Round)
            drawLine(color, Offset(endX, y), Offset(endX - arrow, y - arrow), 2.dp.toPx(), StrokeCap.Round)
            drawLine(color, Offset(endX, y), Offset(endX - arrow, y + arrow), 2.dp.toPx(), StrokeCap.Round)
            if (position.continuesBeforeWeek) {
                drawLine(color, Offset(startX, y), Offset(startX + arrow, y - arrow), 2.dp.toPx(), StrokeCap.Round)
                drawLine(color, Offset(startX, y), Offset(startX + arrow, y + arrow), 2.dp.toPx(), StrokeCap.Round)
            }
        }
        Text(
            text = event.title,
            modifier = Modifier
                .background(PureWhite.copy(alpha = 0.96f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
            color = event.categoryColor.copy(alpha = 1f),
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AllDayBand(
    event: TimetableTimelineEventItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(event.categoryColor.copy(alpha = 0.82f))
            .semantics { contentDescription = "${event.title}, 종일 행사" },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = event.title,
            modifier = Modifier.padding(horizontal = 5.dp),
            color = PureWhite,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TimelineLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun TimelineErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = message,
            color = Ink60,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onRetry) {
            Text("다시 시도")
        }
    }
}

@Composable
private fun TimelineEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "이번 주에 표시할 행사가 없습니다.",
            color = Ink60,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}
