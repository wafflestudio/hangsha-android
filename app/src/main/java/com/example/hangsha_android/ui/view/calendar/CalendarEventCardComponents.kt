package com.example.hangsha_android.ui.view.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.hangsha_android.ui.view.event.resolveCountdownLabel

@Composable
internal fun rememberCalendarEventCountdown(
    item: CalendarEventCardItem
): CalendarEventCountdownState {
    var showEvent by rememberSaveable(item.id) { mutableStateOf(false) }
    val label = resolveCountdownLabel(
        applicationLabel = item.applicationCountdownLabel,
        eventLabel = item.eventCountdownLabel,
        showEvent = showEvent
    )
    return CalendarEventCountdownState(
        text = label.text,
        canToggle = label.canToggle,
        toggleActionLabel = label.toggleActionLabel,
        onToggle = if (label.canToggle) {
            { showEvent = !showEvent }
        } else {
            null
        }
    )
}

@Composable
internal fun CalendarBookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isBookmarked) {
                Icons.Rounded.Bookmark
            } else {
                Icons.Rounded.BookmarkBorder
            },
            contentDescription = if (isBookmarked) "북마크 해제" else "북마크 추가",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
internal fun CalendarEventThumbnail(
    item: CalendarEventCardItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        if (!item.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

internal data class CalendarEventCountdownState(
    val text: String,
    val canToggle: Boolean,
    val toggleActionLabel: String,
    val onToggle: (() -> Unit)?
)
