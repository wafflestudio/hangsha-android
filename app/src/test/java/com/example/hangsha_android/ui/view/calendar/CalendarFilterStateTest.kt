package com.example.hangsha_android.ui.view.calendar

import com.example.hangsha_android.data.repository.model.RECRUITING_STATUS_ID
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarFilterStateTest {
    @Test
    fun resetSelections_preservesExcludedKeywords() {
        val reset = CalendarFilterState(
            orgIds = setOf(1L),
            statusIds = setOf(3L),
            eventTypeIds = setOf(1L),
            excludedKeywords = listOf("contest")
        ).resetSelections()

        assertEquals(emptySet<Long>(), reset.orgIds)
        assertEquals(setOf(RECRUITING_STATUS_ID), reset.statusIds)
        assertEquals(emptySet<Long>(), reset.eventTypeIds)
        assertEquals(listOf("contest"), reset.excludedKeywords)
    }
}
