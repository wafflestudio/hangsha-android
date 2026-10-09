package com.example.hangsha_android.ui.components

import android.graphics.Rect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.example.hangsha_android.ui.view.event.EventPreviewData
import kotlin.math.roundToInt

private data class PreviewRequest(val owner: Any, val data: EventPreviewData, val touch: IntOffset)

private class PreviewController(val events: Map<Long, EventPreviewData>) {
    var request by mutableStateOf<PreviewRequest?>(null)

    fun dismiss(owner: Any) {
        if (request?.owner === owner) request = null
    }
}

private val LocalPreviewController = staticCompositionLocalOf<PreviewController?> { null }
private class PreviewAnchor { var coordinates: LayoutCoordinates? = null }

/** A window popup escapes pager/scroll clipping without taking the held gesture's focus. */
@Composable
internal fun EventPreviewHost(
    events: Map<Long, EventPreviewData>,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val controller = remember(events) { PreviewController(events) }
    CompositionLocalProvider(LocalPreviewController provides controller) {
        Box(modifier = modifier) {
            content()
            controller.request?.let { EventPreviewPopup(it) }
        }
    }
}

/** A short tap opens details. A long press is consumed until release, then always dismissed. */
internal fun Modifier.eventPreviewClickable(eventId: Long, onClick: () -> Unit): Modifier = composed {
    val controller = LocalPreviewController.current
    if (controller == null) return@composed clickable(onClick = onClick)
    val owner = remember { Any() }
    val anchor = remember { PreviewAnchor() }
    val currentOnClick by rememberUpdatedState(onClick)
    val haptics = LocalHapticFeedback.current
    val interactions = remember { MutableInteractionSource() }
    DisposableEffect(controller, owner) {
        onDispose { controller.dismiss(owner) }
    }
    this
        .onGloballyPositioned { anchor.coordinates = it }
        .indication(interactions, ripple())
        .semantics(mergeDescendants = true) {
            role = Role.Button
            controller.events[eventId]?.let { contentDescription = it.title }
            onClick { currentOnClick(); true }
        }
        .pointerInput(controller, eventId) {
            detectTapGestures(
                onTap = { currentOnClick() },
                onLongPress = { position ->
                    val coordinates = anchor.coordinates
                    val data = controller.events[eventId]
                    if (coordinates != null && coordinates.isAttached && data != null) {
                        val touch = coordinates.localToWindow(position)
                        controller.request = PreviewRequest(
                            owner, data, IntOffset(touch.x.roundToInt(), touch.y.roundToInt())
                        )
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                onPress = { position ->
                    val press = PressInteraction.Press(position)
                    var finished = false
                    try {
                        interactions.emit(press)
                        val released = tryAwaitRelease()
                        controller.dismiss(owner)
                        interactions.emit(
                            if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press)
                        )
                        finished = true
                    } finally {
                        controller.dismiss(owner)
                        if (!finished) interactions.tryEmit(PressInteraction.Cancel(press))
                    }
                }
            )
        }
}

@Composable
private fun EventPreviewPopup(request: PreviewRequest) {
    val view = LocalView.current
    val density = LocalDensity.current
    val margin = with(density) { 12.dp.roundToPx() }
    // Clearance is measured from the finger, not from the potentially tiny event block.
    val fingerGap = with(density) { 64.dp.roundToPx() }
    val bounds = remember(view, density, request) {
        val frame = Rect()
        view.getWindowVisibleDisplayFrame(frame)
        val screen = IntArray(2)
        val window = IntArray(2)
        view.getLocationOnScreen(screen)
        view.getLocationInWindow(window)
        val dx = screen[0] - window[0]
        val dy = screen[1] - window[1]
        IntRect(frame.left - dx + margin, frame.top - dy + margin,
            frame.right - dx - margin, frame.bottom - dy - margin)
    }
    val spaceAbove = (request.touch.y - fingerGap - bounds.top).coerceAtLeast(0)
    val spaceBelow = (bounds.bottom - request.touch.y - fingerGap).coerceAtLeast(0)
    val availableHeight = maxOf(spaceAbove, spaceBelow).coerceAtLeast(1)
    val width = with(density) { minOf(320.dp, bounds.width.coerceAtLeast(1).toDp()) }
    val maxHeight = with(density) { availableHeight.toDp() }
    val positionProvider = remember(request.touch, bounds, fingerGap) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = eventPreviewPosition(request.touch, popupContentSize, bounds, fingerGap)
        }
    }
    Popup(
        popupPositionProvider = positionProvider,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = true
        )
    ) {
        Surface(
            modifier = Modifier.width(width).heightIn(max = maxHeight),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = request.data.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                request.data.schedule?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                request.data.organizationOrType?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
