package com.example.hangsha_android.ui.view.calendar.week

import org.junit.Assert.assertEquals
import org.junit.Test

class WeekSwipeSessionTest {
    @Test
    fun longDragAndExtremeVelocityStayWithinOneWeekOnPhoneAndTablet() {
        for (width in listOf(320f, 1280f)) {
            for (direction in listOf(-1, 1)) {
                val session = WeekSwipeSession(100, if (direction < 0) 0 else 600, 600)
                assertEquals(
                    if (direction < 0) WeekSwipeDestination.PreviousWeek else WeekSwipeDestination.NextWeek,
                    session.selectDestination(direction.toFloat())
                )
                var offset = 0f
                repeat(100) {
                    offset += session.constrainPageDelta(direction * width, offset, width)
                    assertEquals(direction * width, offset, 0.001f)
                }
                assertEquals(100 + direction, session.targetPage(offset / width, direction * 100_000f, 400f))
            }
        }
    }

    @Test
    fun gestureStartingInsideGridNeverChangesDestinationWhenItReachesEdge() {
        val session = WeekSwipeSession(100, 300, 600)
        repeat(100) {
            assertEquals(WeekSwipeDestination.Grid, session.selectDestination(1000f))
        }
        assertEquals(100, session.targetPage(0f, 100_000f, 400f))
    }

    @Test
    fun tabletWithAllSevenDaysVisibleCanPageInEitherDirection() {
        for (direction in listOf(-1, 1)) {
            val session = WeekSwipeSession(100, 0, 0)
            assertEquals(
                if (direction < 0) WeekSwipeDestination.PreviousWeek else WeekSwipeDestination.NextWeek,
                session.selectDestination(direction.toFloat())
            )
            assertEquals(100 + direction, session.targetPage(0.1f * direction, 5000f * direction, 400f))
        }
    }

    @Test
    fun reversingDragReturnsToOriginWithoutContinuingIntoAnotherWeek() {
        val session = WeekSwipeSession(100, 600, 600)
        session.selectDestination(1f)
        assertEquals(-320f, session.constrainPageDelta(-3200f, 320f, 320f), 0.001f)
        assertEquals(100, session.targetPage(0.2f, -5000f, 400f))
    }

    @Test
    fun shortSlowDragReturnsAndLongSlowDragAdvances() {
        val session = WeekSwipeSession(100, 600, 600)
        session.selectDestination(1f)
        assertEquals(100, session.targetPage(0.2f, 0f, 400f))
        assertEquals(101, session.targetPage(0.8f, 0f, 400f))
    }

    @Test
    fun interruptedPageAnimationStaysInPagerEvenIfGridIsNotAtEdge() {
        val session = WeekSwipeSession(100, 300, 600, initialPageOffset = 0.3f)
        assertEquals(WeekSwipeDestination.NextWeek, session.selectDestination(-20f))
        assertEquals(100, session.targetPage(0.2f, -5000f, 400f))
    }
}
