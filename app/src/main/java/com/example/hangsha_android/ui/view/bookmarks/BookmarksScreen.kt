package com.example.hangsha_android.ui.view.bookmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Ink100
import com.example.hangsha_android.ui.theme.PureWhite
import com.example.hangsha_android.ui.view.event.eventTypeColor
import com.example.hangsha_android.ui.view.event.resolveCountdownLabel
import kotlinx.coroutines.flow.collect

private val BookmarkIconTint = Color(0xFF858585)
private val EmptyImageBackground = Color(0xFFE8F3EC)

// 찜 목록 화면 구성
@Composable
fun BookmarksScreen(
    uiState: BookmarksUiState,
    onNavigateBack: () -> Unit,
    onEventClick: (Long) -> Unit,
    onBookmarkClick: (Long) -> Unit,
    onRetryClick: () -> Unit,
    onLoadNextPage: () -> Unit,
    onScrollPositionChanged: (Int, Int, Long?) -> Unit
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = uiState.savedScrollIndex,
        initialFirstVisibleItemScrollOffset = uiState.savedScrollOffset
    )

    LaunchedEffect(listState, uiState.items) {
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                uiState.items.getOrNull(listState.firstVisibleItemIndex)?.id
            )
        }.collect { (index, offset, itemId) ->
            onScrollPositionChanged(index, offset, itemId)
        }
    }

    LaunchedEffect(uiState.items) {
        restoreScrollPosition(
            listState = listState,
            items = uiState.items,
            savedIndex = uiState.savedScrollIndex,
            savedOffset = uiState.savedScrollOffset,
            savedAnchorItemId = uiState.savedAnchorItemId
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
        ) {
            BookmarksHeader(onNavigateBack = onNavigateBack)

            when {
                uiState.items.isEmpty() && uiState.isInitialLoading -> Unit
                uiState.items.isEmpty() && uiState.errorMessage != null -> {
                    BookmarksErrorState(
                        message = uiState.errorMessage,
                        onRetryClick = onRetryClick
                    )
                }
                uiState.items.isEmpty() -> {
                    BookmarksEmptyState()
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(top = 34.dp, bottom = 30.dp),
                        verticalArrangement = Arrangement.spacedBy(26.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.items,
                            key = { _, item -> item.id }
                        ) { index, item ->
                            if (index >= uiState.items.lastIndex - 2) {
                                LaunchedEffect(uiState.nextPage, uiState.hasMore) {
                                    onLoadNextPage()
                                }
                            }

                            BookmarkedEventCard(
                                item = item,
                                onClick = { onEventClick(item.id) },
                                onBookmarkClick = { onBookmarkClick(item.id) }
                            )
                        }

                        if (uiState.isLoadingNextPage) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                                }
                            }
                        }

                        if (uiState.errorMessage != null && !uiState.isLoadingNextPage) {
                            item {
                                Text(
                                    text = uiState.errorMessage,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFFF4B4B),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isInitialLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

private suspend fun restoreScrollPosition(
    listState: LazyListState,
    items: List<BookmarkedEventItem>,
    savedIndex: Int,
    savedOffset: Int,
    savedAnchorItemId: Long?
) {
    if (items.isEmpty()) {
        return
    }

    val anchorIndex = savedAnchorItemId?.let { anchorId ->
        items.indexOfFirst { item -> item.id == anchorId }.takeIf { it >= 0 }
    } ?: savedIndex.coerceIn(0, items.lastIndex)

    if (
        listState.firstVisibleItemIndex != anchorIndex ||
        listState.firstVisibleItemScrollOffset != savedOffset
    ) {
        listState.scrollToItem(
            index = anchorIndex,
            scrollOffset = savedOffset
        )
    }
}

// 상단 헤더 구성
@Composable
private fun BookmarksHeader(
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = "뒤로 가기",
            tint = Color(0xFFB5B5B5),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(30.dp)
                .clickable(onClick = onNavigateBack)
        )
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = "내 찜 목록",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink100,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Rounded.Bookmark,
                contentDescription = null,
                tint = BookmarkIconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// 찜 행사 카드 구성
@Composable
private fun BookmarkedEventCard(
    item: BookmarkedEventItem,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    val showEventDDay = rememberSaveable(item.id) { mutableStateOf(false) }
    val countdownLabel = resolveCountdownLabel(
        applicationLabel = item.dDayLabel,
        eventLabel = item.eventDDayLabel,
        showEvent = showEventDDay.value
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.78f)
                .clip(RoundedCornerShape(7.dp))
                .background(EmptyImageBackground)
        ) {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(27.dp)
                            .clip(CircleShape)
                            .background(eventTypeColor(item.eventTypeId))
                    )
                    Spacer(modifier = Modifier.size(11.dp))
                    Text(
                        text = countdownLabel.text,
                        modifier = Modifier.clickable(
                            enabled = countdownLabel.canToggle,
                            onClickLabel = countdownLabel.toggleActionLabel
                        ) {
                            showEventDDay.value = !showEventDDay.value
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink100,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink100,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(9.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.applyPeriodDisplay,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink60,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.organization.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink60,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.Bookmark,
                contentDescription = "북마크 해제",
                tint = BookmarkIconTint,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(35.dp)
                    .clickable(onClick = onBookmarkClick)
            )
        }
    }
}

// 빈 상태 구성
@Composable
private fun BookmarksEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "아직 찜한 행사가 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = Ink60
        )
    }
}

// 오류 상태 구성
@Composable
private fun BookmarksErrorState(
    message: String,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink60
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(onClick = onRetryClick) {
            Text(text = "다시 시도")
        }
    }
}
