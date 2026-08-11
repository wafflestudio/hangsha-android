package com.example.hangsha_android.ui.view.dailyevents

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangsha_android.ui.theme.Coral60
import com.example.hangsha_android.ui.theme.Cream5
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Ink100
import com.example.hangsha_android.ui.theme.Peach20
import com.example.hangsha_android.ui.theme.PureWhite
import com.example.hangsha_android.ui.view.org.organizationLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DailyHeaderFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREA)
private val DailyBookmarkTint = Color(0xFFB8BBC1)

private data class DailyEventsHeaderState(
    val selectedDate: LocalDate,
    val hasActiveFilters: Boolean,
    val isLoading: Boolean
)

@Composable
fun DailyEventsScreen(
    uiState: DailyEventsUiState,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onOpenFilterClick: () -> Unit,
    onDismissFilterSheet: () -> Unit,
    onSelectFilterTab: (DailyEventsFilterTab) -> Unit,
    onBookmarkedOnlyChange: (Boolean) -> Unit,
    onInterestedOnlyChange: (Boolean) -> Unit,
    onToggleOrgId: (Long) -> Unit,
    onToggleStatus: (Long) -> Unit,
    onToggleEventType: (Long) -> Unit,
    onExcludeKeywordInputChange: (String) -> Unit,
    onAddExcludeKeyword: () -> Unit,
    onRemoveExcludeKeyword: (String) -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit,
    onRetryClick: () -> Unit,
    onEventClick: (Long) -> Unit,
    onBookmarkClick: (Long) -> Unit
) {
    if (uiState.isFilterSheetVisible) {
        DailyEventsFilterBottomSheet(
            uiState = uiState,
            onDismiss = onDismissFilterSheet,
            onSelectTab = onSelectFilterTab,
            onBookmarkedOnlyChange = onBookmarkedOnlyChange,
            onInterestedOnlyChange = onInterestedOnlyChange,
            onToggleOrgId = onToggleOrgId,
            onToggleStatus = onToggleStatus,
            onToggleEventType = onToggleEventType,
            onExcludeKeywordInputChange = onExcludeKeywordInputChange,
            onAddExcludeKeyword = onAddExcludeKeyword,
            onRemoveExcludeKeyword = onRemoveExcludeKeyword,
            onApply = onApplyFilters,
            onClear = onClearFilters
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 15.dp, vertical = 15.dp)
        ) {
            Spacer(modifier = Modifier.height(25.dp))
            // "2026년 n월 n일", 좌우 화살표, 필터 버튼
            DailyEventsHeader(
                state = DailyEventsHeaderState(
                    selectedDate = uiState.selectedDate,
                    hasActiveFilters = uiState.hasActiveFilters,
                    isLoading = uiState.isLoading
                ),
                onPreviousDayClick = onPreviousDayClick,
                onNextDayClick = onNextDayClick,
                onOpenFilterClick = onOpenFilterClick
            )
            Spacer(modifier = Modifier.height(15.dp))

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
                            DailyEventCard(
                                item = item,
                                organizationNames = uiState.organizationNames,
                                onClick = { onEventClick(item.id) },
                                onBookmarkClick = { onBookmarkClick(item.id) }
                            )
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

// "2026년 n월 n일", 좌우 화살표, 필터 버튼
@Composable
private fun DailyEventsHeader(
    state: DailyEventsHeaderState,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit,
    onOpenFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = state.selectedDate.format(DailyHeaderFormatter),
            style = MaterialTheme.typography.labelLarge,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(8.dp))

        // 이전 버튼
        HeaderArrowButton(
            enabled = !state.isLoading,
            onClick = onPreviousDayClick
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "\uC774\uC804 \uB0A0\uC9DC",
                tint = Ink60
            )
        }
        Spacer(modifier = Modifier.width(1.dp))

        // 다음 버튼
        HeaderArrowButton(
            enabled = !state.isLoading,
            onClick = onNextDayClick
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "\uB2E4\uC74C \uB0A0\uC9DC",
                tint = Ink60
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        // 필터 버튼
        FilterButton(
            isLoading = state.isLoading,
            hasActiveFilters = state.hasActiveFilters,
            onOpenFilterClick = onOpenFilterClick
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

// 이전 버튼, 다음 버튼
@Composable
private fun HeaderArrowButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

// 필터 버튼
@Composable
private fun FilterButton(
    isLoading: Boolean,
    hasActiveFilters: Boolean,
    onOpenFilterClick: () -> Unit
) {
    Box {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = RoundedCornerShape(9.dp),
            color = PureWhite,
            shadowElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = !isLoading, onClick = onOpenFilterClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "\uD544\uD130",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (hasActiveFilters) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Coral60)
            )
        }
    }
}

@Composable
private fun DailyEventCard(
    item: DailyEventItem,
    organizationNames: Map<Long, String>,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(PureWhite)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(item.accentColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = item.dDayLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink60,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Ink100,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${item.eventEndDisplay}   ${organizationLabel(item.orgId, organizationNames, item.organization)}",
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
            contentDescription = "\uBD81\uB9C8\uD06C",
            tint = DailyBookmarkTint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(32.dp)
                .clickable(onClick = onBookmarkClick)
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
                text = "해당 날짜에 표시할 행사가 없습니다.",
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
