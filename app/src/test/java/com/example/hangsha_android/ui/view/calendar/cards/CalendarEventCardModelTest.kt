package com.example.hangsha_android.ui.view.calendar.cards

import com.example.hangsha_android.ui.view.calendar.CalendarPeriod
import com.example.hangsha_android.ui.view.calendar.calendarEvent
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarEventCardModelTest {
    private val sunday = LocalDate.of(2026, 10, 4)

    @Test
    fun cardsDeduplicateAcrossDatesWithoutChangingServerOrderWithinADate() {
        val first = calendarEvent(90, start = "2026-10-04", end = "2026-10-06")
        val second = first.copy(id = 1)
        val cards = buildCalendarEventCardItems(
            CalendarPeriod.WEEK.contentRange(sunday),
            linkedMapOf(sunday.plusDays(1) to listOf(first), sunday to listOf(first, second)),
            emptyMap(), emptyMap()
        )
        assertEquals(listOf(90L, 1L), cards.map { it.id })
    }
}
