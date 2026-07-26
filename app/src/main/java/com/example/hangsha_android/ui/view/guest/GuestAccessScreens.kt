package com.example.hangsha_android.ui.view.guest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Ink90
import com.example.hangsha_android.ui.theme.Ink100
import com.example.hangsha_android.ui.theme.PureWhite

private val GuestBorder = Color(0xFFE1E1E1)
private val GuestButton = Color(0xFF222222)
private val GuestSoft = Color(0xFFF7F7F7)

@Composable
fun GuestMyPageScreen(
    uiState: GuestMyPageUiState,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onTimetableClick: () -> Unit,
    onBookmarkedEventClick: (Long) -> Unit,
    onMemoEventClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 28.dp,
            vertical = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            GuestHeader()
        }
        item {
            GuestNotice()
        }
        item {
            GuestAuthActions(
                onLoginClick = onLoginClick,
                onSignUpClick = onSignUpClick
            )
        }
        item {
            GuestLocalSummary(uiState = uiState)
        }
        item {
            GuestBookmarkPreviewSection(
                eventIds = uiState.bookmarkedEventIds,
                onEventClick = onBookmarkedEventClick
            )
        }
        item {
            GuestMemoPreviewSection(
                items = uiState.memoItems,
                onMemoClick = onMemoEventClick
            )
        }
        item {
            GuestShortcutRow(
                title = "Calendar",
                description = "Browse events and keep using local bookmarks and memos.",
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = Ink100,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = onCalendarClick
            )
        }
        item {
            GuestShortcutRow(
                title = "Timetable",
                description = "Open the current timetable screen.",
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = Ink100,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = onTimetableClick
            )
        }
    }
}

@Composable
fun LoginRequiredScreen(
    title: String,
    message: String,
    onLoginClick: () -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.EventBusy,
                contentDescription = null,
                tint = Ink60,
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Ink100,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink60,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onLoginClick,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GuestButton,
                    contentColor = PureWhite
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Login,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "Log in")
            }
            onNavigateBack?.let { navigateBack ->
                TextButton(onClick = navigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = Ink60,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(text = "Back", color = Ink60)
                }
            }
        }
    }
}

@Composable
private fun GuestHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = Ink100,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Guest mode",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Ink100
            )
        }
        Text(
            text = "You can keep local bookmarks and memos without logging in.",
            style = MaterialTheme.typography.bodyMedium,
            color = Ink60,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun GuestNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, GuestBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Guest data is stored only on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink90,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Text(
                text = "Account features, interest priorities, and server-backed lists require login.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink60,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun GuestAuthActions(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GuestButton,
                contentColor = PureWhite
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Login,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = "Log in", fontWeight = FontWeight.Bold)
        }

        TextButton(
            onClick = onSignUpClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Create account", color = Ink100, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GuestLocalSummary(uiState: GuestMyPageUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GuestCountCard(
            label = "Bookmarks",
            count = uiState.bookmarkCount,
            modifier = Modifier.weight(1f)
        )
        GuestCountCard(
            label = "Memos",
            count = uiState.memoCount,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GuestCountCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, GuestBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = Ink100,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink60,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun GuestBookmarkPreviewSection(
    eventIds: Set<Long>,
    onEventClick: (Long) -> Unit
) {
    GuestSection(title = "Local bookmarks", icon = {
        Icon(
            imageVector = Icons.Rounded.Bookmark,
            contentDescription = null,
            tint = Ink100,
            modifier = Modifier.size(18.dp)
        )
    }) {
        if (eventIds.isEmpty()) {
            EmptyGuestText(text = "No local bookmarks yet.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                eventIds.sortedDescending().take(5).forEach { eventId ->
                    GuestRow(
                        title = "Event #$eventId",
                        description = "Open event detail",
                        onClick = { onEventClick(eventId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GuestMemoPreviewSection(
    items: List<GuestMemoPreviewItem>,
    onMemoClick: (Long) -> Unit
) {
    GuestSection(title = "Local memos", icon = {
        Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = null,
            tint = Ink100,
            modifier = Modifier.size(18.dp)
        )
    }) {
        if (items.isEmpty()) {
            EmptyGuestText(text = "No local memos yet.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.take(5).forEach { memo ->
                    GuestRow(
                        title = memo.eventTitle.ifBlank { "Event #${memo.eventId}" },
                        description = memo.content,
                        onClick = { onMemoClick(memo.eventId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GuestSection(
    title: String,
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink100,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        content()
    }
}

@Composable
private fun GuestRow(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, GuestBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink100,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink60,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Ink60,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun EmptyGuestText(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = Ink60,
        fontSize = 12.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun GuestShortcutRow(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = GuestSoft,
        border = BorderStroke(1.dp, GuestBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink100,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink60,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}