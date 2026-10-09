package com.example.hangsha_android.ui.view.calendar.week

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hangsha_android.ui.view.calendar.CalendarPageKey
import com.example.hangsha_android.ui.view.calendar.CalendarPeriod
import com.example.hangsha_android.ui.view.calendar.CalendarPeriodPage
import com.example.hangsha_android.ui.view.calendar.CalendarUiState
import com.example.hangsha_android.ui.view.calendar.pager.CalendarPeriodPager
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WeekCalendarSwipeTest {
    @get:Rule val compose = createComposeRule()

    private val weekStart = LocalDate.of(2026, 10, 4)
    private var state by mutableStateOf(CalendarUiState())
    private val selectedWeeks = mutableListOf<LocalDate>()

    private fun showCalendar(width: Dp, dayIndex: Long) {
        state = CalendarUiState(
            anchorDate = weekStart.plusDays(dayIndex),
            period = CalendarPeriod.WEEK,
            pageStates = (-12L..12L).associate { offset ->
                CalendarPageKey(CalendarPeriod.WEEK, weekStart.plusWeeks(offset)) to
                    CalendarPeriodPage(isLoading = false)
            }
        )
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(width).height(600.dp).testTag("calendar")) {
                    CalendarPeriodPager(
                        uiState = state,
                        onPeriodSelected = {
                            selectedWeeks += CalendarPageKey.from(CalendarPeriod.WEEK, it).startDate
                            state = state.copy(anchorDate = it)
                        },
                        onVisibleWeekDayChange = { state = state.copy(anchorDate = it) },
                        onOpenDayCalendar = {},
                        onViewModeChange = {},
                        onDateClick = {},
                        onEventClick = {},
                        onBookmarkClick = {},
                        showBookmarkAction = false,
                        onOpenFilterClick = {},
                        onRetryClick = {}
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun swipe(forward: Boolean, duration: Long = 64L) {
        compose.onNodeWithTag("calendar").performTouchInput {
            val y = height * 0.65f
            val left = width * 0.15f
            val right = width * 0.9f
            swipe(
                start = Offset(if (forward) right else left, y),
                end = Offset(if (forward) left else right, y),
                durationMillis = duration
            )
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1500L)
        compose.waitForIdle()
    }

    private fun assertWeek(offset: Long) {
        compose.runOnIdle {
            assertEquals(weekStart.plusWeeks(offset), CalendarPageKey.from(CalendarPeriod.WEEK, state.anchorDate).startDate)
        }
    }

    @Test
    fun tabletFastFlingAdvancesExactlyOneWeekAndStops() {
        showCalendar(1000.dp, 0)
        swipe(forward = true)
        assertWeek(1)
        compose.runOnIdle { assertEquals(listOf(weekStart.plusWeeks(1)), selectedWeeks) }
        swipe(forward = false)
        assertWeek(0)
    }

    @Test
    fun phoneInternalFlingStopsAtEdgeAndRequiresAnotherGestureToPage() {
        showCalendar(360.dp, 3)
        swipe(forward = true)
        assertWeek(0)
        compose.runOnIdle { assertEquals(emptyList<LocalDate>(), selectedWeeks) }
        swipe(forward = true)
        assertWeek(1)
    }

    @Test
    fun phoneAtEdgeFastFlingAdvancesExactlyOneWeek() {
        showCalendar(360.dp, 6)
        swipe(forward = true)
        assertWeek(1)
        compose.runOnIdle { assertEquals(listOf(weekStart.plusWeeks(1)), selectedWeeks) }
    }

    @Test
    fun adjacentWeekIsVisibleBeforeFingerIsReleased() {
        showCalendar(1000.dp, 0)
        compose.onNodeWithTag("calendar").performTouchInput {
            down(Offset(width * 0.9f, height * 0.65f))
            moveTo(Offset(width * 0.3f, height * 0.65f), delayMillis = 240L)
        }
        compose.onNodeWithText("일\n11").assertIsDisplayed()
        compose.onNodeWithTag("calendar").performTouchInput { up() }
        compose.waitForIdle()
        assertWeek(1)
    }

    @Test
    fun repeatedTabletSwipesAdvanceOneWeekEachWithoutResidualMomentum() {
        showCalendar(1000.dp, 0)
        repeat(8) { index ->
            swipe(forward = true)
            assertWeek(index + 1L)
        }
        compose.runOnIdle {
            assertEquals((1L..8L).map { weekStart.plusWeeks(it) }, selectedWeeks)
        }
    }

    @Test
    fun navigationButtonMovesExactlyOneWeek() {
        showCalendar(1000.dp, 0)
        compose.onNodeWithContentDescription("다음 기간").performClick()
        compose.waitForIdle()
        assertWeek(1)
        compose.onNodeWithContentDescription("이전 기간").performClick()
        compose.waitForIdle()
        assertWeek(0)
    }
}
