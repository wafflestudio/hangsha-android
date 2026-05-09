package com.example.hangsha_android.ui.view.dailyevents

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangsha_android.ui.theme.Cream5
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Peach20
import com.example.hangsha_android.ui.theme.PureWhite
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DailyHeaderFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREA)
private val DailyBookmarkTint = Color(0xFFB8BBC1)

@Composable
fun DailyEventsScreen(
    uiState: DailyEventsUiState,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            DailyEventsHeader(
                date = uiState.selectedDate,
                onPreviousDayClick = onPreviousDayClick,
                onNextDayClick = onNextDayClick
            )
            Spacer(modifier = Modifier.height(20.dp))

            when {
                uiState.errorMessage != null -> {
                    DailyErrorState(
                        message = uiState.errorMessage,
                        onRetryClick = onRetryClick
                    )
                }

                uiState.items.isEmpty() && !uiState.isLoading -> {
                    DailyEmptyState()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.items,
                            key = { it.id }
                        ) { item ->
                            DailyEventCard(item = item)
                        }
                    }
                }
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun DailyEventsHeader(
    date: LocalDate,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date.format(DailyHeaderFormatter),
            style = MaterialTheme.typography.labelLarge,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(8.dp))
        HeaderArrowButton(onClick = onPreviousDayClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Previous day",
                tint = Ink60
            )
        }
        HeaderArrowButton(onClick = onNextDayClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Next day",
                tint = Ink60
            )
        }
    }
}

@Composable
private fun HeaderArrowButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = PureWhite,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun DailyEventCard(item: DailyEventItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(PureWhite)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(top = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(item.accentColor)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.dDayLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink60
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = listOf(item.displayDate, item.organization.orEmpty())
                    .filter { it.isNotBlank() }
                    .joinToString("   "),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink60
            )
        }

        Icon(
            imageVector = if (item.isBookmarked) {
                Icons.Rounded.Bookmark
            } else {
                Icons.Rounded.BookmarkBorder
            },
            contentDescription = "Bookmark",
            tint = DailyBookmarkTint,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun DailyEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Cream5
        ) {
            Text(
                text = "해당 날짜에 모집중인 행사가 없습니다.",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DailyErrorState(
    message: String,
    onRetryClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Peach20.copy(alpha = 0.45f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = onRetryClick) {
                    Text(text = "다시 시도")
                }
            }
        }
    }
}
