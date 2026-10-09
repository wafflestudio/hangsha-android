package com.example.hangsha_android.ui.view.calendar.week

import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException

private const val WeekSnapDurationMillis = 220
private val WeekFlingVelocityThreshold = 400.dp

/**
 * The only horizontal gesture owner for the week calendar. Both built-in horizontal
 * gesture handlers are disabled; their scroll states still lay out and animate the content.
 */
internal class WeekPagerGestures(private val pager: PagerState) : DraggableState {
    private val grids = mutableMapOf<Int, ScrollState>()
    private var session: WeekSwipeSession? = null
    private var activeDrag: DragScope? = null

    @Composable
    fun rememberGridState(page: Int): ScrollState {
        val state = rememberScrollState()
        DisposableEffect(this, page, state) {
            grids[page] = state
            onDispose { grids.remove(page) }
        }
        return state
    }

    override suspend fun drag(dragPriority: MutatePriority, block: suspend DragScope.() -> Unit) {
        // Taking the scroll locks cancels the preceding animation/fling before reading its position.
        pager.scroll(dragPriority) {
            val pageScroll = this
            val startPage = pager.currentPage
            val grid = grids[startPage] ?: return@scroll
            grid.scroll(dragPriority) {
                val gridScroll = this
                val gesture = WeekSwipeSession(
                    startPage, grid.value, grid.maxValue, pager.currentPageOffsetFraction
                )
                session = gesture
                val scope = object : DragScope {
                    override fun dragBy(pixels: Float) {
                        val delta = -pixels
                        if (gesture.selectDestination(delta) == WeekSwipeDestination.Grid) {
                            gridScroll.scrollBy(delta)
                        } else {
                            val pageSize = (pager.layoutInfo.pageSize + pager.layoutInfo.pageSpacing).toFloat()
                            if (pageSize <= 0f) return
                            val offset = -pager.getOffsetDistanceInPages(gesture.startPage) * pageSize
                            pageScroll.scrollBy(gesture.constrainPageDelta(delta, offset, pageSize))
                        }
                    }
                }
                activeDrag = scope
                try {
                    block(scope)
                } catch (cancelled: CancellationException) {
                    session = null
                    throw cancelled
                } finally {
                    activeDrag = null
                }
            }
        }
    }

    override fun dispatchRawDelta(delta: Float) {
        activeDrag?.dragBy(delta)
    }

    suspend fun settle(velocity: Float, velocityThreshold: Float, gridFling: FlingBehavior) {
        val gesture = session ?: return
        session = null
        if (gesture.destination == WeekSwipeDestination.Grid) {
            val grid = grids[gesture.startPage] ?: return
            grid.scroll {
                // Deliberately discard any residual velocity at the grid's edge.
                with(gridFling) { performFling(-velocity) }
            }
        } else {
            val offset = -pager.getOffsetDistanceInPages(gesture.startPage)
            val target = gesture.targetPage(offset, -velocity, velocityThreshold)
                .coerceIn(0, pager.pageCount - 1)
            // No decay or inherited velocity: one bounded animation ends this gesture.
            pager.animateScrollToPage(target, animationSpec = tween(durationMillis = WeekSnapDurationMillis))
        }
    }
}

@Composable
internal fun Modifier.weekPagerGestures(gestures: WeekPagerGestures): Modifier {
    val velocityThreshold = with(LocalDensity.current) { WeekFlingVelocityThreshold.toPx() }
    val gridFling = ScrollableDefaults.flingBehavior()
    return draggable(
        state = gestures,
        orientation = Orientation.Horizontal,
        reverseDirection = LocalLayoutDirection.current == LayoutDirection.Rtl,
        onDragStopped = { velocity -> gestures.settle(velocity, velocityThreshold, gridFling) }
    )
}
