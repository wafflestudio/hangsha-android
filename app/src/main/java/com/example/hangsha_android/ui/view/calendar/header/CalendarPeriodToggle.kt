package com.example.hangsha_android.ui.view.calendar.header

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangsha_android.ui.view.calendar.CalendarPeriod

private val PeriodOrder = listOf(
    CalendarPeriod.MONTH,
    CalendarPeriod.WEEK,
    CalendarPeriod.DAY
)

@Composable
internal fun CalendarPeriodToggle(
    selectedPeriod: CalendarPeriod,
    onPeriodSelected: (CalendarPeriod) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .selectableGroup()
                .padding(3.dp)
        ) {
            PeriodOrder.forEach { period ->
                val selected = selectedPeriod == period
                Surface(
                    modifier = Modifier
                        .size(width = 42.dp, height = 30.dp)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onPeriodSelected(period) }
                        ),
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (selected) 1.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = period.label(),
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun CalendarPeriod.label(): String = when (this) {
    CalendarPeriod.MONTH -> "월"
    CalendarPeriod.WEEK -> "주"
    CalendarPeriod.DAY -> "일"
}
