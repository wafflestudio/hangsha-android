package com.example.hangsha_android.ui.view.calendar.day

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangsha_android.ui.view.calendar.CalendarEvent
import com.example.hangsha_android.ui.components.eventPreviewClickable
import com.example.hangsha_android.ui.view.event.eventTypeColor
import java.time.LocalDate

private val HourHeight = 64.dp
private val TimeGutterWidth = 48.dp

@Composable
internal fun CalendarDayView(
    date: LocalDate,
    events: List<CalendarEvent>,
    isLoading: Boolean,
    onEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val layout = remember(date, events) { CalendarDayEventMapper.map(date, events) }
    val positioned = remember(layout.timed) { positionCalendarDayEvents(layout.timed) }
    val firstHour = remember(layout.timed) {
        val earliest = layout.timed.minOfOrNull { it.startMinute } ?: 7 * 60
        if (earliest < 7 * 60) {
            (earliest / 60 - 1).coerceAtLeast(0)
        } else {
            maxOf(7, earliest / 60 - 2)
        }
    }
    val lastHour = remember(layout.timed, firstHour) {
        val latest = layout.timed.maxOfOrNull { it.endMinute } ?: 24 * 60
        maxOf(firstHour + 1, minOf(24, (latest + 59) / 60 + 2))
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (layout.allDay.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.width(TimeGutterWidth).padding(top = 9.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(
                            text = "종일",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 216.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        layout.allDay.forEach { event ->
                            DayEventBand(
                                event = event,
                                onClick = { onEventClick(event.eventId) },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                val totalHeight = HourHeight * (lastHour - firstHour)
                val timelineWidth = maxWidth - TimeGutterWidth
                Box(modifier = Modifier.fillMaxWidth().height(totalHeight)) {
                    Column {
                        for (hour in firstHour until lastHour) {
                            Row(modifier = Modifier.fillMaxWidth().height(HourHeight)) {
                                Text(
                                    text = "%02d:00".format(hour),
                                    modifier = Modifier.width(TimeGutterWidth).padding(top = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }

                    positioned.forEach { placed ->
                        val event = placed.event
                        val laneWidth = timelineWidth / placed.laneCount
                        val top = HourHeight * ((event.startMinute - firstHour * 60) / 60f)
                        val duration = HourHeight * ((event.endMinute - event.startMinute) / 60f)
                        DayEventBand(
                            event = event,
                            onClick = { onEventClick(event.eventId) },
                            modifier = Modifier
                                .offset(
                                    x = TimeGutterWidth + laneWidth * placed.lane + 2.dp,
                                    y = top + 2.dp
                                )
                                .width((laneWidth - 4.dp).coerceAtLeast(1.dp))
                                .height((duration - 4.dp).coerceAtLeast(22.dp))
                        )
                    }
                }
            }
        }

        if (!isLoading && layout.allDay.isEmpty() && layout.timed.isEmpty()) {
            Text(
                text = "이 날에 표시할 행사가 없습니다.",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun DayEventBand(
    event: CalendarDayEventItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(eventTypeColor(event.eventTypeId))
            .eventPreviewClickable(event.eventId, onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
