package com.example.hangsha_android.ui.view.calendar.week

import kotlin.math.abs

internal enum class WeekSwipeDestination(val pageStep: Int = 0) {
    Pending,
    Grid,
    PreviousWeek(-1),
    NextWeek(1)
}

/** One gesture keeps its original week and scroll destination until the finger is released. */
internal class WeekSwipeSession(
    val startPage: Int,
    private val initialGridOffset: Int,
    private val maxGridOffset: Int,
    initialPageOffset: Float = 0f
) {
    var destination: WeekSwipeDestination = when {
        initialPageOffset > 0f -> WeekSwipeDestination.NextWeek
        initialPageOffset < 0f -> WeekSwipeDestination.PreviousWeek
        else -> WeekSwipeDestination.Pending
    }
        private set

    fun selectDestination(scrollDelta: Float): WeekSwipeDestination {
        if (destination != WeekSwipeDestination.Pending || scrollDelta == 0f) return destination
        destination = when {
            scrollDelta < 0f && initialGridOffset == 0 -> WeekSwipeDestination.PreviousWeek
            scrollDelta > 0f && initialGridOffset == maxGridOffset -> WeekSwipeDestination.NextWeek
            else -> WeekSwipeDestination.Grid
        }
        return destination
    }

    fun constrainPageDelta(delta: Float, offsetFromStart: Float, pageSize: Float): Float {
        val direction = destination.pageStep
        val boundary = direction * pageSize
        val target = (offsetFromStart + delta).coerceIn(minOf(0f, boundary), maxOf(0f, boundary))
        return target - offsetFromStart
    }

    fun targetPage(offsetPages: Float, velocity: Float, velocityThreshold: Float): Int {
        val direction = destination.pageStep
        if (direction == 0) return startPage
        val advance = if (abs(velocity) >= velocityThreshold) {
            velocity * direction > 0f
        } else {
            offsetPages * direction >= 0.5f
        }
        return startPage + if (advance) direction else 0
    }
}
