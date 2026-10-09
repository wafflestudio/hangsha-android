package com.example.hangsha_android.ui.view.calendar.header

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.example.hangsha_android.ui.view.calendar.CalendarPeriod
import com.example.hangsha_android.ui.view.calendar.CalendarViewMode
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CalendarHeaderTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun periodHeaderKeepsSearchOnRightAndDispatchesBothActions() {
        var selected: CalendarPeriod? = null
        var searches = 0
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    CalendarPeriodHeader(
                        selectedPeriod = CalendarPeriod.MONTH,
                        isLoading = false,
                        onPeriodSelected = { selected = it },
                        onSearchClick = { searches++ }
                    )
                }
            }
        }
        val day = compose.onNodeWithText("일")
        val search = compose.onNodeWithContentDescription("행사 검색")
        assertTrue(
            search.fetchSemanticsNode().boundsInRoot.left >= day.fetchSemanticsNode().boundsInRoot.right
        )
        day.performClick()
        search.performClick()
        compose.runOnIdle {
            assertEquals(CalendarPeriod.DAY, selected)
            assertEquals(1, searches)
        }
    }

    private fun assertHeaderLayout(period: CalendarPeriod, date: LocalDate) {
        val title = period.headerTitle(date)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    CalendarHeader(
                        title = title,
                        selectedViewMode = CalendarViewMode.CALENDAR,
                        hasActiveFilters = false,
                        isLoading = false,
                        onPreviousPeriodClick = {},
                        onNextPeriodClick = {},
                        onViewModeChange = {},
                        onOpenFilterClick = {}
                    )
                }
            }
        }
        listOf("리스트 보기", "그리드 보기", "캘린더 보기").forEach { description ->
            compose.onNodeWithContentDescription(description, useUnmergedTree = true)
                .assertIsDisplayed()
                .assertWidthIsEqualTo(20.dp)
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(title).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(layouts)
        }
        assertEquals(1, layouts.single().lineCount)
        assertFalse(layouts.single().isLineEllipsized(0))
    }

    @Test
    fun monthHeaderPreservesAllToggleIconSizes() {
        assertHeaderLayout(CalendarPeriod.MONTH, LocalDate.of(2026, 10, 9))
    }

    @Test
    fun dayHeaderPreservesAllToggleIconSizes() {
        assertHeaderLayout(CalendarPeriod.DAY, LocalDate.of(2026, 10, 9))
    }

    @Test
    fun weekHeaderPreservesAllToggleIconSizes() {
        assertHeaderLayout(CalendarPeriod.WEEK, LocalDate.of(2026, 10, 9))
    }

    @Test
    fun weekSpanningTwoMonthsPreservesAllToggleIconSizes() {
        assertHeaderLayout(CalendarPeriod.WEEK, LocalDate.of(2026, 10, 31))
    }
}
