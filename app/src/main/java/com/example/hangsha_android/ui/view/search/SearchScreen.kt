package com.example.hangsha_android.ui.view.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hangsha_android.ui.theme.Cream10
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Ink90
import com.example.hangsha_android.ui.theme.Ink100
import com.example.hangsha_android.ui.view.event.eventTypeColor
import com.example.hangsha_android.ui.view.event.resolveCountdownLabel

@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onNavigateBack: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onEventClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "뒤로 가기",
                    tint = Ink60
                )
            }
            Text(
                text = if (uiState.submittedQuery.isBlank()) "검색" else "'${uiState.submittedQuery}' 검색 결과",
                style = MaterialTheme.typography.titleMedium,
                color = Ink100,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        OutlinedTextField(
            value = uiState.input,
            onValueChange = onInputChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("검색어를 입력해 주세요") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.input.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Rounded.Close, contentDescription = "검색어 지우기")
                        }
                    }
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Rounded.Search, contentDescription = "검색")
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(14.dp))

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null && uiState.items.isEmpty() -> {
                SearchMessage(
                    message = uiState.errorMessage,
                    actionText = "다시 시도",
                    onAction = onRetry
                )
            }
            !uiState.hasSearched -> SearchMessage("검색어를 입력해 보세요!")
            uiState.items.isEmpty() -> SearchMessage("검색 결과가 없습니다.")
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "총 ${uiState.total}개 결과",
                            color = Ink60,
                            fontSize = 13.sp
                        )
                    }
                    items(uiState.items, key = SearchEventItem::id) { item ->
                        SearchResultCard(item = item, onClick = { onEventClick(item.id) })
                    }
                    if (uiState.errorMessage != null) {
                        item {
                            Text(
                                text = uiState.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    if (uiState.canLoadMore || uiState.isLoadingMore) {
                        item {
                            TextButton(
                                onClick = onLoadMore,
                                enabled = !uiState.isLoadingMore,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (uiState.isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("더 보기")
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(item: SearchEventItem, onClick: () -> Unit) {
    val showEventDDay = rememberSaveable(item.id) { mutableStateOf(false) }
    val countdownLabel = resolveCountdownLabel(
        applicationLabel = item.dDayLabel,
        eventLabel = item.eventDDayLabel,
        showEvent = showEventDDay.value
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Ink100,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                item.snippet?.let {
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = it,
                        color = Ink60,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(9.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchChip(
                        text = countdownLabel.text,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable(
                            enabled = countdownLabel.canToggle,
                            onClickLabel = countdownLabel.toggleActionLabel
                        ) {
                            showEventDDay.value = !showEventDDay.value
                        }
                    )
                    SearchChip(item.eventTypeLabel, eventTypeColor(item.eventTypeId))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${item.eventDateDisplay} · ${item.organization.orEmpty()}",
                    color = Ink60,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(82.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Cream10)
                )
            }
        }
    }
}

@Composable
private fun SearchChip(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = color) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = Ink90,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SearchMessage(
    message: String,
    actionText: String? = null,
    onAction: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = Ink60)
            if (actionText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onAction) { Text(actionText) }
            }
        }
    }
}
