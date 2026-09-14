package com.example.hangsha_android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class HangshaWindowWidthSizeClass {
    Compact,
    Medium,
    Expanded
}

enum class HangshaContentWidth(val maxWidth: Dp) {
    Form(560.dp),
    Reading(840.dp),
    Workspace(1_200.dp)
}

data class HangshaWindowInfo(
    val width: Dp,
    val widthSizeClass: HangshaWindowWidthSizeClass,
    val horizontalContentPadding: Dp
) {
    val usesNavigationRail: Boolean
        get() = widthSizeClass != HangshaWindowWidthSizeClass.Compact
}

val LocalHangshaWindowInfo = staticCompositionLocalOf {
    calculateHangshaWindowInfo(0.dp)
}

@Composable
fun HangshaAdaptiveLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        CompositionLocalProvider(
            LocalHangshaWindowInfo provides calculateHangshaWindowInfo(maxWidth),
            content = content
        )
    }
}

@Composable
fun HangshaConstrainedContent(
    contentWidth: HangshaContentWidth,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = contentAlignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = contentWidth.maxWidth)
                .fillMaxSize(),
            content = content
        )
    }
}

internal fun calculateHangshaWindowInfo(width: Dp): HangshaWindowInfo {
    val widthSizeClass = when {
        width < 600.dp -> HangshaWindowWidthSizeClass.Compact
        width < 840.dp -> HangshaWindowWidthSizeClass.Medium
        else -> HangshaWindowWidthSizeClass.Expanded
    }
    val horizontalContentPadding = when (widthSizeClass) {
        HangshaWindowWidthSizeClass.Compact -> 16.dp
        HangshaWindowWidthSizeClass.Medium -> 24.dp
        HangshaWindowWidthSizeClass.Expanded -> 32.dp
    }
    return HangshaWindowInfo(
        width = width,
        widthSizeClass = widthSizeClass,
        horizontalContentPadding = horizontalContentPadding
    )
}
