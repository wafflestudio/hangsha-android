package com.example.hangsha_android.ui.view.eventdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.hangsha_android.ui.theme.Coral60
import com.example.hangsha_android.ui.theme.Cream10
import com.example.hangsha_android.ui.theme.Cream5
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Ink90
import com.example.hangsha_android.ui.theme.Peach20
import com.example.hangsha_android.ui.theme.PureWhite

@Composable
fun EventDetailScreen(
    uiState: EventDetailUiState,
    onNavigateBack: () -> Unit,
    onRetryClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            uiState.errorMessage != null -> {
                EventDetailErrorState(
                    message = uiState.errorMessage,
                    onNavigateBack = onNavigateBack,
                    onRetryClick = onRetryClick
                )
            }

            uiState.item != null -> {
                EventDetailContent(
                    item = uiState.item,
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }
}

@Composable
private fun EventDetailContent(
    item: EventDetailItem,
    onNavigateBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onNavigateBack,
                    shape = CircleShape,
                    color = PureWhite,
                    shadowElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = PureWhite,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!item.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Cream10),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No image",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Ink60
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink90
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.organization ?: "-",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Ink60
                                )
                            }

                            Row {
                                IconButton(onClick = {}) {
                                    Icon(
                                        imageVector = if (item.isInterested) {
                                            Icons.Rounded.Favorite
                                        } else {
                                            Icons.Rounded.FavoriteBorder
                                        },
                                        contentDescription = "Interested",
                                        tint = Coral60
                                    )
                                }
                                IconButton(onClick = {}) {
                                    Icon(
                                        imageVector = if (item.isBookmarked) {
                                            Icons.Rounded.Bookmark
                                        } else {
                                            Icons.Rounded.BookmarkBorder
                                        },
                                        contentDescription = "Bookmark",
                                        tint = Ink60
                                    )
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DetailChip(text = item.operationMode)
                            DetailChip(text = item.statusText)
                            DetailChip(text = "Capacity ${item.capacityText}")
                            DetailChip(text = "Applied ${item.applyCountText}")
                        }

                        DetailInfoRow(label = "Application", value = item.applyPeriodText)
                        DetailInfoRow(label = "Event", value = item.eventPeriodText)
                        DetailInfoRow(label = "Location", value = item.location ?: "-")
                        DetailInfoRow(label = "Apply Link", value = item.applyLink ?: "-")

                        if (item.tags.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Tags",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Ink90
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item.tags.forEach { tag ->
                                        DetailChip(text = "#$tag")
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Detail",
                                style = MaterialTheme.typography.titleSmall,
                                color = Ink90
                            )
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Cream5
                            ) {
                                Text(
                                    text = item.detail ?: "-",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Ink60
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.width(92.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Ink60,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink90
        )
    }
}

@Composable
private fun DetailChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Peach20.copy(alpha = 0.5f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Ink90
        )
    }
}

@Composable
private fun EventDetailErrorState(
    message: String,
    onNavigateBack: () -> Unit,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink90
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onNavigateBack) {
                Text(text = "Back")
            }
            Button(onClick = onRetryClick) {
                Text(text = "Retry")
            }
        }
    }
}
