package com.example.hangsha_android.ui.view.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private val ScreenHorizontalPadding = 15.dp
private val ScreenVerticalPadding = 15.dp

@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onPreviousPeriodClick: () -> Unit,
    onNextPeriodClick: () -> Unit,
    onViewModeChange: (CalendarViewMode) -> Unit,
    onDateClick: (LocalDate) -> Unit,
    onEventClick: (Long) -> Unit,
    onBookmarkClick: (Long) -> Unit,
    showBookmarkAction: Boolean,
    onSearchClick: () -> Unit,
    onOpenFilterClick: () -> Unit,
    onDismissFilterSheet: () -> Unit,
    onSelectFilterTab: (CalendarFilterTab) -> Unit,
    onToggleOrgId: (Long) -> Unit,
    onToggleStatus: (Long) -> Unit,
    onToggleEventType: (Long) -> Unit,
    onExcludeKeywordInputChange: (String) -> Unit,
    onAddExcludeKeyword: () -> Unit,
    onRemoveExcludeKeyword: (String) -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit,
    onRetryClick: () -> Unit
) {
    if (uiState.isFilterSheetVisible) {
        CalendarFilterBottomSheet(
            uiState = uiState,
            onDismiss = onDismissFilterSheet,
            onSelectTab = onSelectFilterTab,
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

    val eventCardItems = remember(
        uiState.contentRange,
        uiState.eventsByDate,
        uiState.organizationNames,
        uiState.eventTypeNames
    ) {
        buildCalendarEventCardItems(
            contentRange = uiState.contentRange,
            eventsByDate = uiState.eventsByDate,
            organizationNames = uiState.organizationNames,
            eventTypeNames = uiState.eventTypeNames
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = ScreenHorizontalPadding,
                vertical = ScreenVerticalPadding
            )
    ) {
        Spacer(modifier = Modifier.height(25.dp))
        CalendarHeader(
            title = uiState.period.headerTitle(uiState.anchorDate),
            selectedViewMode = uiState.viewMode,
            hasActiveFilters = uiState.hasActiveFilters,
            isLoading = uiState.isLoading,
            onPreviousPeriodClick = onPreviousPeriodClick,
            onNextPeriodClick = onNextPeriodClick,
            onViewModeChange = onViewModeChange,
            onSearchClick = onSearchClick,
            onOpenFilterClick = onOpenFilterClick
        )
        Spacer(modifier = Modifier.height(15.dp))

        val errorMessage = uiState.errorMessage
        if (errorMessage != null) {
            CalendarErrorState(
                message = errorMessage,
                onRetryClick = onRetryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            CalendarViewHost(
                uiState = uiState,
                eventCardItems = eventCardItems,
                showBookmarkAction = showBookmarkAction,
                onDateClick = onDateClick,
                onEventClick = onEventClick,
                onBookmarkClick = onBookmarkClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CalendarErrorState(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetryClick) {
                Text("다시 시도")
            }
        }
    }
}
