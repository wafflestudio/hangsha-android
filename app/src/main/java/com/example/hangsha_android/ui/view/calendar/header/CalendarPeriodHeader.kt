package com.example.hangsha_android.ui.view.calendar.header

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.hangsha_android.ui.view.calendar.CalendarPeriod

@Composable
internal fun CalendarPeriodHeader(
    selectedPeriod: CalendarPeriod,
    isLoading: Boolean,
    onPeriodSelected: (CalendarPeriod) -> Unit,
    onSearchClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CalendarPeriodToggle(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = onPeriodSelected
        )
        IconButton(
            onClick = onSearchClick,
            enabled = !isLoading,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "행사 검색",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
