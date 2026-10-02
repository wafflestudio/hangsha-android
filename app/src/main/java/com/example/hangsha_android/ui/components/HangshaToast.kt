package com.example.hangsha_android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class HangshaToastType {
    Success, Info, Warning, Error
}

private data class HangshaToastData(
    val id: Long,
    val message: String,
    val type: HangshaToastType,
    val durationMillis: Int
)

class HangshaToastState {
    private val pending = ArrayDeque<HangshaToastData>()
    private var nextId = 0L
    private var current by mutableStateOf<HangshaToastData?>(null)

    fun show(message: String, type: HangshaToastType = HangshaToastType.Info) {
        if (message.isBlank()) return
        val toast = HangshaToastData(
            id = nextId++,
            message = message,
            type = type,
            durationMillis = if (type == HangshaToastType.Warning || type == HangshaToastType.Error) 5_000 else 3_000
        )
        if (current == null) current = toast else pending.addLast(toast)
    }

    private fun dismiss(id: Long) {
        if (current?.id != id) return
        current = if (pending.isEmpty()) null else pending.removeFirst()
    }

    @Composable
    internal fun Host(bottomBarHeight: Dp? = null) {
        val toast = current
        var popupOpen by remember { mutableStateOf(toast != null) }
        var animatedToast by remember { mutableStateOf<HangshaToastData?>(null) }
        LaunchedEffect(toast?.id) {
            if (toast != null) {
                delay(toast.durationMillis.toLong())
                dismiss(toast.id)
            }
        }
        LaunchedEffect(toast) {
            if (toast != null) {
                popupOpen = true
                withFrameNanos { }
                animatedToast = toast
            } else {
                animatedToast = null
                delay(220)
                popupOpen = false
            }
        }

        if (popupOpen) {
            Popup(
                alignment = Alignment.BottomCenter,
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .then(
                            if (bottomBarHeight == null) Modifier.navigationBarsPadding()
                            else Modifier.padding(bottom = bottomBarHeight)
                        )
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    AnimatedContent(
                        targetState = animatedToast,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 3 }) togetherWith
                                (fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 3 })
                        },
                        label = "Hangsha toast"
                    ) { item ->
                        if (item != null) {
                            HangshaToastCard(
                                toast = item,
                                onDismiss = { dismiss(item.id) },
                                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

val LocalHangshaToastState = staticCompositionLocalOf<HangshaToastState> {
    error("HangshaToastState is not provided")
}

@Composable
private fun HangshaToastCard(
    toast: HangshaToastData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = toast.type.accentColor()
    val progress = remember(toast.id) { Animatable(1f) }
    LaunchedEffect(toast.id) {
        progress.animateTo(0f, tween(toast.durationMillis, easing = LinearEasing))
    }

    Surface(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(24.dp).background(accent.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = toast.type.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = accent
                    )
                }
                Text(
                    text = toast.message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "알림 닫기",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(3.dp)
                    .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                    .background(accent.copy(alpha = 0.13f))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(progress.value).height(3.dp).background(accent)
                )
            }
        }
    }
}

private fun HangshaToastType.icon(): ImageVector = when (this) {
    HangshaToastType.Success -> Icons.Rounded.Check
    HangshaToastType.Info -> Icons.Rounded.Info
    HangshaToastType.Warning -> Icons.Rounded.Warning
    HangshaToastType.Error -> Icons.Rounded.Error
}

@Composable
private fun HangshaToastType.accentColor(): Color = when (this) {
    HangshaToastType.Success -> Color(0xFF3C9A68)
    HangshaToastType.Info -> Color(0xFF477DCD)
    HangshaToastType.Warning -> Color(0xFFD88A28)
    HangshaToastType.Error -> MaterialTheme.colorScheme.error
}
