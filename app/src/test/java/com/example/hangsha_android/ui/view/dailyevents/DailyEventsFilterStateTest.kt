package com.example.hangsha_android.ui.view.dailyevents

import com.example.hangsha_android.data.repository.model.RECRUITING_STATUS_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DailyEventsFilterStateTest {
    @Test
    fun resetSelections_preservesExcludedKeywords() {
        val reset = DailyEventsFilterState(
            bookmarkedOnly = true,
            interestedOnly = true,
            orgIds = setOf(1L),
            statusIds = setOf(3L),
            eventTypeIds = setOf(4L),
            excludedKeywords = listOf("contest")
        ).resetSelections()

        assertFalse(reset.bookmarkedOnly)
        assertFalse(reset.interestedOnly)
        assertEquals(emptySet<Long>(), reset.orgIds)
        assertEquals(setOf(RECRUITING_STATUS_ID), reset.statusIds)
        assertEquals(emptySet<Long>(), reset.eventTypeIds)
        assertEquals(listOf("contest"), reset.excludedKeywords)
    }
}
