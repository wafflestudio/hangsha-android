package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.network.model.DayEventsResponse
import com.example.hangsha_android.data.network.model.EventSummaryResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class EventRepositoryPaginationTest {

    @Test
    fun fullPageAlwaysProbesNextPage() = runBlocking {
        val requestedPages = mutableListOf<Int>()

        val result = collectDayEventPages { page, size ->
            assertEquals(20, size)
            requestedPages += page
            Response.success(
                dayResponse(
                    page = page,
                    total = 20,
                    items = if (page == 1) {
                        (1L..20L).map(::event)
                    } else {
                        emptyList()
                    }
                )
            )
        }

        assertEquals(listOf(1, 2), requestedPages)
        assertEquals((1L..20L).toList(), result.items.map { it.id })
        assertEquals(20, result.total)
    }

    @Test
    fun collectsAllPagesAndRemovesDuplicateEventIds() = runBlocking {
        val requestedPages = mutableListOf<Int>()

        val result = collectDayEventPages { page, _ ->
            requestedPages += page
            val items = when (page) {
                1 -> (1L..20L).map(::event)
                2 -> listOf(event(20L), event(21L))
                else -> error("Unexpected page $page")
            }
            Response.success(dayResponse(page = page, total = 21, items = items))
        }

        assertEquals(listOf(1, 2), requestedPages)
        assertEquals((1L..21L).toList(), result.items.map { it.id })
        assertEquals(21, result.size)
        assertEquals(21, result.total)
    }

    private fun dayResponse(
        page: Int,
        total: Int,
        items: List<EventSummaryResponse>
    ) = DayEventsResponse(
        page = page,
        size = items.size,
        total = total,
        date = "2026-08-22",
        items = items
    )

    private fun event(id: Long) = EventSummaryResponse(
        id = id,
        title = "Event $id",
        imageUrl = null,
        operationMode = null,
        statusId = null,
        eventTypeId = null,
        orgId = null,
        applyStart = null,
        applyEnd = null,
        eventStart = null,
        eventEnd = null,
        isPeriodEvent = false,
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
