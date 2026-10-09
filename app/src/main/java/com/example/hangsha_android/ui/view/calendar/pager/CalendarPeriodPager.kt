package com.example.hangsha_android.ui.view.calendar.pager

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.dp
import com.example.hangsha_android.ui.view.calendar.CalendarHeader
import com.example.hangsha_android.ui.view.calendar.CalendarPageKey
import com.example.hangsha_android.ui.view.calendar.CalendarPeriod
import com.example.hangsha_android.ui.view.calendar.CalendarPeriodPage
import com.example.hangsha_android.ui.view.calendar.CalendarUiState
import com.example.hangsha_android.ui.view.calendar.CalendarViewHost
import com.example.hangsha_android.ui.view.calendar.CalendarViewMode
import com.example.hangsha_android.ui.view.calendar.headerTitle
import com.example.hangsha_android.ui.view.calendar.week.WeekPagerGestures
import com.example.hangsha_android.ui.view.calendar.week.weekPagerGestures
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val PageCount = 200_001
private const val InitialPage = PageCount / 2

@Composable
internal fun CalendarPeriodPager(
    uiState: CalendarUiState,
    onPeriodSelected: (LocalDate) -> Unit,
    onVisibleWeekDayChange: (LocalDate) -> Unit,
    onOpenDayCalendar: (LocalDate) -> Unit,
    onViewModeChange: (CalendarViewMode) -> Unit,
    onDateClick: (LocalDate) -> Unit,
    onEventClick: (Long) -> Unit,
    onBookmarkClick: (Long) -> Unit,
    showBookmarkAction: Boolean,
    onOpenFilterClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    key(uiState.period) {
        val period = uiState.period
        val originDate = rememberSaveable { uiState.anchorDate.toString() }
        val origin = LocalDate.parse(originDate)
        val pagerState = rememberPagerState(
            initialPage = InitialPage,
            pageCount = { PageCount }
        )
        val scope = rememberCoroutineScope()
        val currentAnchor = rememberUpdatedState(uiState.anchorDate)
        val currentOnPeriodSelected = rememberUpdatedState(onPeriodSelected)
        val isWeekCalendar =
            period == CalendarPeriod.WEEK && uiState.viewMode == CalendarViewMode.CALENDAR
        val weekGestures = remember(pagerState) { WeekPagerGestures(pagerState) }
        val weekGestureModifier = if (isWeekCalendar) {
            Modifier.weekPagerGestures(weekGestures)
        } else Modifier
        val pageNestedScrollConnection = if (isWeekCalendar) {
            // Vertical child scrolling must never feed horizontal delta/velocity to this pager.
            remember { object : NestedScrollConnection {} }
        } else {
            PagerDefaults.pageNestedScrollConnection(pagerState, Orientation.Horizontal)
        }
        val flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            pagerSnapDistance = PagerSnapDistance.atMost(1)
        )

        fun anchorForPage(page: Int): LocalDate =
            period.move(origin, (page - InitialPage).toLong())

        LaunchedEffect(pagerState, originDate) {
            var previousPage = pagerState.settledPage
            snapshotFlow { pagerState.settledPage }
                .distinctUntilChanged()
                .collect { page ->
                    val anchor = if (period == CalendarPeriod.WEEK && page != previousPage) {
                        val weekStart = CalendarPageKey.from(period, anchorForPage(page)).startDate
                        weekStart.plusDays(if (page > previousPage) 0 else 6)
                    } else {
                        anchorForPage(page)
                    }
                    previousPage = page
                    if (CalendarPageKey.from(period, anchor) !=
                        CalendarPageKey.from(period, currentAnchor.value)
                    ) {
                        currentOnPeriodSelected.value(anchor)
                    }
                }
        }

        Column(modifier = modifier.fillMaxSize()) {
            CalendarHeader(
                title = period.headerTitle(uiState.anchorDate),
                selectedViewMode = uiState.viewMode,
                hasActiveFilters = uiState.hasActiveFilters,
                isLoading = uiState.isLoading,
                onPreviousPeriodClick = {
                    if (!pagerState.isScrollInProgress) scope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.settledPage - 1).coerceAtLeast(0)
                        )
                    }
                },
                onNextPeriodClick = {
                    if (!pagerState.isScrollInProgress) scope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.settledPage + 1).coerceAtMost(PageCount - 1)
                        )
                    }
                },
                onViewModeChange = onViewModeChange,
                onOpenFilterClick = onOpenFilterClick
            )
            Spacer(modifier = Modifier.height(15.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).then(weekGestureModifier),
                userScrollEnabled = !isWeekCalendar,
                flingBehavior = flingBehavior,
                pageNestedScrollConnection = pageNestedScrollConnection,
                key = { page ->
                    CalendarPageKey.from(period, anchorForPage(page)).startDate.toEpochDay()
                }
            ) { page ->
                val weekScrollState = if (isWeekCalendar) weekGestures.rememberGridState(page) else null
                val anchor = anchorForPage(page)
                val pageKey = CalendarPageKey.from(period, anchor)
                val initialWeekDayIndex = remember(page, uiState.viewMode) {
                    when {
                        page > pagerState.settledPage -> 0
                        page < pagerState.settledPage -> 6
                        else -> ChronoUnit.DAYS.between(pageKey.startDate, uiState.anchorDate)
                            .toInt().coerceIn(0, 6)
                    }
                }
                val pageState = uiState.pageStates[pageKey] ?: CalendarPeriodPage()
                val selectedKey = CalendarPageKey.from(period, uiState.anchorDate)
                val errorMessage = if (pageKey == selectedKey) {
                    uiState.errorMessage ?: pageState.errorMessage
                } else {
                    pageState.errorMessage
                }

                if (errorMessage != null) {
                    CalendarPageError(
                        message = errorMessage,
                        onRetryClick = onRetryClick
                    )
                } else {
                    CalendarViewHost(
                        period = period,
                        anchorDate = anchor,
                        initialWeekDayIndex = initialWeekDayIndex,
                        weekScrollState = weekScrollState,
                        isCurrentPage = page == pagerState.settledPage,
                        viewMode = uiState.viewMode,
                        page = pageState,
                        organizationNames = uiState.organizationNames,
                        eventTypeNames = uiState.eventTypeNames,
                        showBookmarkAction = showBookmarkAction,
                        onDateClick = onDateClick,
                        onOpenDayCalendar = onOpenDayCalendar,
                        onVisibleWeekDayChange = onVisibleWeekDayChange,
                        onEventClick = onEventClick,
                        onBookmarkClick = onBookmarkClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarPageError(
    message: String,
    onRetryClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
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
