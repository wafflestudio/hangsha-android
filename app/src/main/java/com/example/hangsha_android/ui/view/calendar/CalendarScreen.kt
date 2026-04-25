package com.example.hangsha_android.ui.view.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onOpenFilterClick: () -> Unit,
    onDismissFilterSheet: () -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit,
    onRetryClick: () -> Unit
) {
    val previewEvents = uiState.eventsByDate.entries
        .sortedBy { it.key }
        .flatMap { (date, events) -> events.map { date to it } }
        .take(2)

    if (uiState.isFilterSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissFilterSheet
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (uiState.hasActiveFilters) {
                        "Active filters are applied. Detailed options will be wired next."
                    } else {
                        "Filter UI placeholder. Detailed options will be wired next."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onClearFilters,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Clear")
                    }
                    Button(
                        onClick = onApplyFilters,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Apply")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Calendar",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPreviousMonthClick,
                modifier = Modifier.weight(1f),
                enabled = !uiState.isLoading
            ) {
                Text(text = "Prev")
            }
            OutlinedButton(
                onClick = onOpenFilterClick,
                modifier = Modifier.weight(1f),
                enabled = !uiState.isLoading
            ) {
                Text(text = if (uiState.hasActiveFilters) "Filter On" else "Filter")
            }
            OutlinedButton(
                onClick = onNextMonthClick,
                modifier = Modifier.weight(1f),
                enabled = !uiState.isLoading
            ) {
                Text(text = "Next")
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = uiState.currentMonth.formatMonthTitle(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${uiState.visibleRange.from} to ${uiState.visibleRange.to}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading events...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetryClick) {
                    Text(text = "Retry")
                }
            }

            else -> {
                Text(
                    text = "Preview Events",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (previewEvents.isEmpty()) {
                    Text(
                        text = "No events found for this range.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(previewEvents, key = { (_, event) -> event.id }) { (date, event) ->
                            EventPreviewCard(
                                dateLabel = date.toString(),
                                event = event
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loaded dates: ${uiState.eventsByDate.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EventPreviewCard(
    dateLabel: String,
    event: CalendarEvent
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.large
            )
            .padding(16.dp)
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = event.title,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = event.organization ?: "Unknown organization",
            style = MaterialTheme.typography.bodyMedium
        )
        event.location?.takeIf { it.isNotBlank() }?.let { location ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = location,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun YearMonthTextHint(uiState: CalendarUiState) {
    Text(
        text = uiState.currentMonth.formatMonthTitle(),
        style = MaterialTheme.typography.titleLarge
    )
}

private fun java.time.YearMonth.formatMonthTitle(): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
    return format(formatter)
}
