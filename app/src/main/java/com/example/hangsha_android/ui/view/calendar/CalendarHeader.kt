package com.example.hangsha_android.ui.view.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
internal fun CalendarHeader(
    title: String,
    selectedViewMode: CalendarViewMode,
    hasActiveFilters: Boolean,
    isLoading: Boolean,
    onPreviousPeriodClick: () -> Unit,
    onNextPeriodClick: () -> Unit,
    onViewModeChange: (CalendarViewMode) -> Unit,
    onSearchClick: () -> Unit,
    onOpenFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(8.dp))
        HeaderNavigationButtons(
            onPreviousPeriodClick = onPreviousPeriodClick,
            onNextPeriodClick = onNextPeriodClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        CalendarFilterButton(
            isLoading = isLoading,
            hasActiveFilters = hasActiveFilters,
            onOpenFilterClick = onOpenFilterClick
        )
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            CalendarViewModeToggle(
                selectedMode = selectedViewMode,
                onModeSelected = onViewModeChange
            )
            IconButton(
                onClick = onSearchClick,
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "행사 검색",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HeaderNavigationButtons(
    onPreviousPeriodClick: () -> Unit,
    onNextPeriodClick: () -> Unit
) {
    HeaderCircleButton(
        enabled = true,
        onClick = onPreviousPeriodClick
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = "이전 기간",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.width(1.dp))
    HeaderCircleButton(
        enabled = true,
        onClick = onNextPeriodClick
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "다음 기간",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalendarFilterButton(
    isLoading: Boolean,
    hasActiveFilters: Boolean,
    onOpenFilterClick: () -> Unit
) {
    Box {
        HeaderCircleButton(
            enabled = !isLoading,
            onClick = onOpenFilterClick,
            isElevated = true
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = "필터",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        if (hasActiveFilters) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
            )
        }
    }
}

@Composable
private fun HeaderCircleButton(
    enabled: Boolean,
    onClick: () -> Unit,
    isElevated: Boolean = false,
    content: @Composable () -> Unit
) {
    if (isElevated) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = RoundedCornerShape(9.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = enabled, onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
