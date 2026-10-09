package com.example.hangsha_android.ui.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

/** All coordinates are relative to the application window, including the safe visible bounds. */
internal fun eventPreviewPosition(
    touch: IntOffset,
    size: IntSize,
    bounds: IntRect,
    fingerGap: Int
): IntOffset {
    val x = (touch.x - size.width / 2).coerceIn(
        bounds.left, (bounds.right - size.width).coerceAtLeast(bounds.left)
    )
    val above = touch.y - fingerGap - size.height
    val below = touch.y + fingerGap
    val y = when {
        above >= bounds.top -> above
        below + size.height <= bounds.bottom -> below
        touch.y - bounds.top >= bounds.bottom - touch.y -> bounds.top
        else -> (bounds.bottom - size.height).coerceAtLeast(bounds.top)
    }
    return IntOffset(x, y.coerceIn(bounds.top, (bounds.bottom - size.height).coerceAtLeast(bounds.top)))
}
