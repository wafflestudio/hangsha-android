package com.example.hangsha_android.ui.view.timetable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun SnuttTimetablePickerDialog(
    onClose: () -> Unit,
    onTimetableSelected: (SnuttTimetable) -> Unit,
    onError: (String) -> Unit
) {
    val config = remember { SnuttPickerConfig.create() }
    val currentOnClose by rememberUpdatedState(onClose)
    val currentOnTimetableSelected by rememberUpdatedState(onTimetableSelected)
    val currentOnError by rememberUpdatedState(onError)

    if (config == null) {
        LaunchedEffect(Unit) {
            currentOnError(UNSUPPORTED_WEBVIEW_MESSAGE)
        }
        return
    }

    Dialog(
        onDismissRequest = currentOnClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SnuttPickerWebView(
                config = config,
                onTimetableSelected = currentOnTimetableSelected,
                onError = currentOnError
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 4.dp
            ) {
                IconButton(onClick = currentOnClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "닫기",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private const val UNSUPPORTED_WEBVIEW_MESSAGE =
    "Android System WebView를 업데이트한 뒤 다시 시도해 주세요."
