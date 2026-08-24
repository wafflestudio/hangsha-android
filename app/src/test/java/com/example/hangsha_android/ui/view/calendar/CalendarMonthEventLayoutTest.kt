package com.example.hangsha_android.ui.view.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalendarMonthEventLayoutTest {
    private val sunday: LocalDate = LocalDate.of(2026, 8, 23)

    @Test
    fun mapper_deduplicatesDateBucketsAndUsesApplyRangeForPeriodEvent() {
        val event = event(
            id = 1L,
            date = sunday,
            isPeriodEvent = true,
            applyStart = "2026-08-24T09:00:00+09:00",
            applyEnd = "2026-08-27T18:00:00+09:00",
            eventStart = "2026-08-25T10:00:00+09:00",
            eventEnd = "2026-08-25T11:00:00+09:00"
        )

        val result = CalendarMonthEventMapper.map(
            mapOf(
                sunday to listOf(event),
                sunday.plusDays(1) to listOf(event.copy(date = sunday.plusDays(1)))
            )
        )

        assertEquals(1, result.size)
        assertEquals(LocalDate.of(2026, 8, 24), result.single().startDate)
        assertEquals(LocalDate.of(2026, 8, 27), result.single().endDate)
        assertTrue(result.single().isPeriodEvent)
    }

    @Test
    fun mapper_usesEventRangeForRegularEventAndConvertsOffsetToSeoul() {
        val result = CalendarMonthEventMapper.map(
            mapOf(
                sunday to listOf(
                    event(
                        id = 2L,
                        date = sunday,
                        eventStart = "2026-08-23T16:00:00Z",
                        eventEnd = "2026-08-23T17:00:00Z"
                    )
                )
            )
        )

        assertEquals(LocalDate.of(2026, 8, 24), result.single().startDate)
        assertEquals(LocalDate.of(2026, 8, 24), result.single().endDate)
        assertFalse(result.single().isPeriodEvent)
    }

    @Test
    fun positionWeek_clipsSpanAndMarksWeekContinuation() {
        val layout = CalendarMonthEventLayoutCalculator.positionWeek(
            events = listOf(
                monthEvent(
                    id = 1L,
                    start = sunday.minusDays(2),
                    end = sunday.plusDays(8),
                    isPeriodEvent = true
                )
            ),
            weekStart = sunday
        )

        val segment = layout.visibleEvents.single()
        assertEquals(0, segment.startDay)
        assertEquals(6, segment.endDay)
        assertTrue(segment.continuesBeforeWeek)
        assertTrue(segment.continuesAfterWeek)
    }

    @Test
    fun positionWeek_putsOverlappingEventsInSeparateLanesAndReusesLane() {
        val layout = CalendarMonthEventLayoutCalculator.positionWeek(
            events = listOf(
                monthEvent(1L, sunday, sunday.plusDays(2), isPeriodEvent = true),
                monthEvent(2L, sunday.plusDays(1), sunday.plusDays(3)),
                monthEvent(3L, sunday.plusDays(4), sunday.plusDays(4))
            ),
            weekStart = sunday
        )

        val byId = layout.visibleEvents.associateBy { it.event.eventId }
        assertEquals(0, byId.getValue(1L).laneIndex)
        assertEquals(1, byId.getValue(2L).laneIndex)
        assertEquals(0, byId.getValue(3L).laneIndex)
    }

    @Test
    fun positionWeek_countsHiddenEventsPerCoveredDay() {
        val events = (1L..6L).map { id ->
            monthEvent(id, sunday, sunday.plusDays(1), isPeriodEvent = true)
        }

        val layout = CalendarMonthEventLayoutCalculator.positionWeek(
            events = events,
            weekStart = sunday,
            maxVisibleLanes = 4
        )

        assertEquals(4, layout.visibleEvents.size)
        assertEquals(listOf(2, 2, 0, 0, 0, 0, 0), layout.overflowByDay)
    }

    private fun monthEvent(
        id: Long,
        start: LocalDate,
        end: LocalDate,
        isPeriodEvent: Boolean = false
    ) = CalendarMonthEventItem(
        eventId = id,
        title = "행사 $id",
        eventTypeId = id,
        startDate = start,
        endDate = end,
        isPeriodEvent = isPeriodEvent
    )

    private fun event(
        id: Long,
        date: LocalDate,
        isPeriodEvent: Boolean = false,
        applyStart: String? = null,
        applyEnd: String? = null,
        eventStart: String? = null,
        eventEnd: String? = null
    ) = CalendarEvent(
        id = id,
        date = date,
        title = "행사 $id",
        imageUrl = null,
        operationMode = null,
        statusId = null,
        eventTypeId = id,
        orgId = null,
        applyStart = applyStart,
        applyEnd = applyEnd,
        eventStart = eventStart,
        eventEnd = eventEnd,
        isPeriodEvent = isPeriodEvent,
        capacity = null,
        applyCount = null,
        organization = null,
        location = null,
        applyLink = null,
        tags = null,
        isInterested = false,
        matchedInterestPriority = null,
        isBookmarked = false
    )
}
