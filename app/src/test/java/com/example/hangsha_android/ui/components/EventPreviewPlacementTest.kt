package com.example.hangsha_android.ui.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventPreviewPlacementTest {
    private val bounds = IntRect(12, 36, 388, 764)
    private val card = IntSize(320, 180)
    private val gap = 64

    @Test
    fun prefersAboveWithFingerClearance() {
        val position = eventPreviewPosition(IntOffset(200, 450), card, bounds, gap)
        assertEquals(IntOffset(40, 206), position)
        assertEquals(450 - gap, position.y + card.height)
    }

    @Test
    fun nearTopMovesBelowWithoutEnteringStatusBar() {
        val position = eventPreviewPosition(IntOffset(200, 70), card, bounds, gap)
        assertEquals(IntOffset(40, 134), position)
    }

    @Test
    fun leftAndRightEdgesKeepFullPanelInsideSafeArea() {
        assertEquals(bounds.left,
            eventPreviewPosition(IntOffset(15, 450), card, bounds, gap).x)
        assertEquals(bounds.right - card.width,
            eventPreviewPosition(IntOffset(385, 450), card, bounds, gap).x)
    }

    @Test
    fun exactFitAboveDoesNotFlipBelow() {
        val touchY = bounds.top + card.height + gap
        assertEquals(bounds.top,
            eventPreviewPosition(IntOffset(200, touchY), card, bounds, gap).y)
    }

    @Test
    fun bottomEdgeAndOffsetWindowKeepPanelVisible() {
        val windowBounds = IntRect(140, 80, 700, 580)
        val result = eventPreviewPosition(IntOffset(680, 570), card, windowBounds, gap)
        assertTrue(result.x >= windowBounds.left)
        assertTrue(result.x + card.width <= windowBounds.right)
        assertTrue(result.y >= windowBounds.top)
        assertTrue(result.y + card.height <= 570 - gap)
    }
}
