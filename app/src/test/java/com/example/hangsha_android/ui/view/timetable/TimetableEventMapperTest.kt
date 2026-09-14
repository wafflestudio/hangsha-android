package com.example.hangsha_android.ui.view.timetable

import com.example.hangsha_android.data.network.model.EventSummaryResponse
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableEventMapperTest {
    private val weekStart = LocalDate.of(2026, 8, 17)

    @Test
    fun map_periodEvent_usesApplyRangeBeforeEventRange() {
        val result = TimetableEventMapper.map(
            events = listOf(
                event(
                    id = 1,
                    isPeriodEvent = true,
                    applyStart = "2026-08-18T09:00:00+09:00",
                    applyEnd = "2026-08-21T18:00:00+09:00",
                    eventStart = "2026-08-17T10:00:00+09:00",
                    eventEnd = "2026-08-17T11:00:00+09:00"
                )
            ),
            weekStart = weekStart,
            dayCount = 5
        )

        assertTrue(result.timed.isEmpty())
        assertTrue(result.allDay.isEmpty())
        assertEquals(1, result.period.size)
        assertEquals(LocalDate.of(2026, 8, 18), result.period.single().startDate)
        assertEquals(LocalDate.of(2026, 8, 21), result.period.single().endDate)
    }

    @Test
    fun map_timedEvent_convertsOffsetTimeToKoreaAndWeekday() {
        val result = TimetableEventMapper.map(
            events = listOf(
                event(
                    id = 2,
                    eventStart = "2026-08-19T01:00:00Z",
                    eventEnd = "2026-08-19T02:30:00Z"
                )
            ),
            weekStart = weekStart,
            dayCount = 5
        )

        val timed = result.timed.single()
        assertEquals(2, timed.weekday)
        assertEquals(10 * 60, timed.startMinute)
        assertEquals(11 * 60 + 30, timed.endMinute)
        assertEquals(2L, timed.eventId)
    }

    @Test
    fun map_nonPeriodFullDayEvent_movesItToAllDayTimeline() {
        val result = TimetableEventMapper.map(
            events = listOf(
                event(
                    id = 3,
                    eventStart = "2026-08-20T00:00:00+09:00",
                    eventEnd = "2026-08-21T00:00:00+09:00"
                )
            ),
            weekStart = weekStart,
            dayCount = 5
        )

        assertTrue(result.timed.isEmpty())
        assertEquals(1, result.allDay.size)
        assertEquals(LocalDate.of(2026, 8, 20), result.allDay.single().startDate)
        assertEquals(LocalDate.of(2026, 8, 20), result.allDay.single().endDate)
    }

    @Test
    fun map_deduplicatesEventsRepeatedInByDateBuckets() {
        val duplicate = event(
            id = 4,
            eventStart = "2026-08-17T10:00:00+09:00",
            eventEnd = "2026-08-17T11:00:00+09:00"
        )

        val result = TimetableEventMapper.map(
            events = listOf(duplicate, duplicate),
            weekStart = weekStart,
            dayCount = 5
        )

        assertEquals(1, result.timed.size)
    }

    @Test
    fun map_invalidOrWeekendTimedEvents_areIgnored() {
        val result = TimetableEventMapper.map(
            events = listOf(
                event(id = 5, eventStart = "invalid", eventEnd = "invalid"),
                event(
                    id = 6,
                    eventStart = "2026-08-22T10:00:00+09:00",
                    eventEnd = "2026-08-22T11:00:00+09:00"
                )
            ),
            weekStart = weekStart,
            dayCount = 5
        )

        assertEquals(TimetableWeekEvents(), result)
    }

    @Test
    fun map_sevenDayView_placesSundayFirstAndIncludesSaturday() {
        val sunday = LocalDate.of(2026, 8, 16)
        val result = TimetableEventMapper.map(
            events = listOf(
                event(
                    id = 7,
                    eventStart = "2026-08-16T10:00:00+09:00",
                    eventEnd = "2026-08-16T11:00:00+09:00"
                ),
                event(
                    id = 8,
                    eventStart = "2026-08-22T10:00:00+09:00",
                    eventEnd = "2026-08-22T11:00:00+09:00"
                )
            ),
            weekStart = sunday,
            dayCount = 7
        )

        assertEquals(listOf(0, 6), result.timed.map { event -> event.weekday })
    }

    private fun event(
        id: Long,
        isPeriodEvent: Boolean = false,
        applyStart: String? = null,
        applyEnd: String? = null,
        eventStart: String? = null,
        eventEnd: String? = null
    ): EventSummaryResponse {
        return EventSummaryResponse(
            id = id,
            title = "행사 $id",
            imageUrl = null,
            operationMode = null,
            statusId = null,
            eventTypeId = 5,
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
            isInterested = null,
            matchedInterestPriority = null,
            isBookmarked = null
        )
    }
}
