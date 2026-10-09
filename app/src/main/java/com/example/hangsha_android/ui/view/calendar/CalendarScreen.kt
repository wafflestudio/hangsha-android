package com.example.hangsha_android.ui.view.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hangsha_android.ui.view.calendar.filter.CalendarFilterBottomSheet
import com.example.hangsha_android.ui.view.calendar.filter.CalendarFilterTab
import com.example.hangsha_android.ui.view.calendar.header.CalendarPeriodHeader
import com.example.hangsha_android.ui.view.calendar.pager.CalendarPeriodPager
import java.time.LocalDate

private val ScreenHorizontalPadding = 15.dp
private val ScreenVerticalPadding = 15.dp

@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onCalendarPeriodChange: (CalendarPeriod) -> Unit,
    onPeriodSelected: (LocalDate) -> Unit,
    onVisibleWeekDayChange: (LocalDate) -> Unit,
    onOpenDayCalendar: (LocalDate) -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = ScreenHorizontalPadding,
                vertical = ScreenVerticalPadding
            )
    ) {
        CalendarPeriodHeader(
            selectedPeriod = uiState.period,
            isLoading = uiState.isLoading,
            onPeriodSelected = onCalendarPeriodChange,
            onSearchClick = onSearchClick
        )
        Spacer(modifier = Modifier.height(4.dp))
        CalendarPeriodPager(
            uiState = uiState,
            onPeriodSelected = onPeriodSelected,
            onVisibleWeekDayChange = onVisibleWeekDayChange,
            onOpenDayCalendar = onOpenDayCalendar,
            onViewModeChange = onViewModeChange,
            onDateClick = onDateClick,
            onEventClick = onEventClick,
            onBookmarkClick = onBookmarkClick,
            showBookmarkAction = showBookmarkAction,
            onOpenFilterClick = onOpenFilterClick,
            onRetryClick = onRetryClick,
            modifier = Modifier.weight(1f)
        )
    }
}
