package com.example.hangsha_android.ui.view.calendar

import com.example.hangsha_android.data.repository.model.EventFilters
import com.example.hangsha_android.data.repository.model.RECRUITING_STATUS_ID
import com.example.hangsha_android.ui.view.calendar.filter.applyFilters
import com.example.hangsha_android.ui.view.calendar.filter.resetSelections
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarStateTransformTest {
    private val date = LocalDate.of(2026, 10, 4)

    @Test
    fun bookmarkChangeUpdatesEveryCachedCopyWithoutChangingOrder() {
        val events = linkedMapOf(date to listOf(calendarEvent(90), calendarEvent(1)))
        val page = CalendarPeriodPage(events, events, isLoading = false)
        val state = CalendarUiState(pageStates = listOf(CalendarPeriod.DAY, CalendarPeriod.WEEK)
            .associate { CalendarPageKey.from(it, date) to page })
        val updated = state.withUpdatedBookmark(90, true)
        updated.pageStates.values.forEach {
            assertEquals(listOf(90L, 1L), it.eventsByDate.getValue(date).map { event -> event.id })
            assertTrue(it.eventsByDate.getValue(date).first().isBookmarked)
            assertTrue(it.filterSourceEventsByDate.getValue(date).first().isBookmarked)
            assertFalse(it.eventsByDate.getValue(date).last().isBookmarked)
        }
        assertFalse(page.eventsByDate.getValue(date).first().isBookmarked)
    }

    @Test
    fun filtersRetainSourceOrderAndDropEmptyDates() {
        val events = linkedMapOf(
            date to listOf(calendarEvent(90), calendarEvent(2).copy(statusId = 3), calendarEvent(1)),
            date.plusDays(1) to listOf(calendarEvent(3).copy(orgId = null))
        )
        val filtered = events.applyFilters(EventFilters(orgIds = setOf(1)))
        assertEquals(listOf(date), filtered.keys.toList())
        assertEquals(listOf(90L, 1L), filtered.getValue(date).map { it.id })
    }

    @Test
    fun resettingSelectionsKeepsExcludedKeywordsAndRecruitingDefault() {
        val reset = EventFilters(orgIds = setOf(1), statusIds = setOf(3),
            eventTypeIds = setOf(2), excludedKeywords = listOf("keyword")).resetSelections()
        assertTrue(reset.orgIds.isEmpty())
        assertTrue(reset.eventTypeIds.isEmpty())
        assertEquals(setOf(RECRUITING_STATUS_ID), reset.statusIds)
        assertEquals(listOf("keyword"), reset.excludedKeywords)
    }
}
