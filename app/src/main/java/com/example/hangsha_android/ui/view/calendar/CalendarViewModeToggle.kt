package com.example.hangsha_android.ui.view.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
internal fun CalendarViewModeToggle(
    selectedMode: CalendarViewMode,
    onModeSelected: (CalendarViewMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            CalendarViewMode.entries.forEach { mode ->
                val isSelected = mode == selectedMode
                Surface(
                    modifier = Modifier
                        .size(width = 34.dp, height = 30.dp)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onModeSelected(mode) }
                        ),
                    shape = RoundedCornerShape(13.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color.Transparent
                    },
                    shadowElevation = if (isSelected) 1.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = mode.icon(),
                            contentDescription = mode.contentDescription(),
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun CalendarViewMode.icon() = when (this) {
    CalendarViewMode.LIST -> Icons.AutoMirrored.Rounded.ViewList
    CalendarViewMode.GRID -> Icons.Rounded.GridView
    CalendarViewMode.CALENDAR -> Icons.Rounded.CalendarMonth
}

private fun CalendarViewMode.contentDescription() = when (this) {
    CalendarViewMode.LIST -> "리스트 보기"
    CalendarViewMode.GRID -> "그리드 보기"
    CalendarViewMode.CALENDAR -> "캘린더 보기"
}
