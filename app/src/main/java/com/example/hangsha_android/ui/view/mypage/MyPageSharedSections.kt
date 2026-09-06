package com.example.hangsha_android.ui.view.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Ink100
import com.example.hangsha_android.ui.theme.PureWhite
import com.example.hangsha_android.ui.view.bookmarks.BookmarkedEventItem
import com.example.hangsha_android.ui.view.event.eventTypeColor
import com.example.hangsha_android.ui.view.event.resolveCountdownLabel
private val MyPageSectionDividerColor = Color(0xFFE7E7E7)
private val MyPageSectionBorderColor = Color(0xFFCACACA)
private val MyPageSectionMutedIconColor = Color(0xFF9B9B9B)
private val MyPageSectionBookmarkPreviewImageBackground = Color(0xFFE8F3EC)
private val MyPageSectionPriorityChipColors = listOf(
    Color(0xFF88D6F8),
    Color(0xFF83C9F4),
    Color(0xFFEAD452),
    Color(0xFFC6A4FF)
)

@Composable
internal fun PrioritySection(
    interests: List<String>,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = PureWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, MyPageSectionBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = MyPageSectionMutedIconColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "행사 보기 우선순위",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink100
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                interests.take(3).forEachIndexed { index, interest ->
                    PriorityChip(
                        text = "${index + 1}순위: $interest",
                        color = MyPageSectionPriorityChipColors[index % MyPageSectionPriorityChipColors.size]
                    )
                }
                if (interests.isEmpty()) {
                    PriorityChip(
                        text = "우선순위를 설정해보세요",
                        color = Color(0xFFE8E8E8)
                    )
                }
            }
        }
    }
}

// 우선순위 알약 영역
@Composable
private fun PriorityChip(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color)
            .padding(horizontal = 11.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// 시간표 등록 영역
@Composable
internal fun TimetableRegistrationRow(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = PureWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, MyPageSectionBorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.AddCircleOutline,
                contentDescription = null,
                tint = MyPageSectionMutedIconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "내 시간표 등록하기",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink100,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 찜 목록 미리보기 영역
@Composable
internal fun BookmarksPreviewSection(
    items: List<BookmarkedEventItem>,
    isLoading: Boolean,
    errorMessage: String?,
    hasMoreItems: Boolean,
    onHeaderClick: () -> Unit,
    onEventClick: (Long) -> Unit,
    onMoreClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ShortcutSectionHeader(
            title = "찜 목록",
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Bookmark,
                    contentDescription = null,
                    tint = MyPageSectionMutedIconColor,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = onHeaderClick
        )
        Spacer(modifier = Modifier.height(14.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(124.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                }
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink60,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            items.isEmpty() -> {
                Text(
                    text = "아직 찜한 행사가 없습니다.\n관심있는 행사를 찜해보세요.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink60,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = items,
                        key = { item -> item.id }
                    ) { item ->
                        BookmarkedEventPreviewCard(
                            item = item,
                            onClick = { onEventClick(item.id) }
                        )
                    }
                    if (hasMoreItems) {
                        item {
                            MoreBookmarksPreviewCard(onClick = onMoreClick)
                        }
                    }
                }
            }
        }
    }
}

// 섹션 제목 이동
@Composable
private fun ShortcutSectionHeader(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink100,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(3.dp))
        icon()
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "$title 이동",
            tint = MyPageSectionMutedIconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

// 찜 행사 미리보기 카드
@Composable
private fun BookmarkedEventPreviewCard(
    item: BookmarkedEventItem,
    onClick: () -> Unit
) {
    val showEventDDay = rememberSaveable(item.id) { mutableStateOf(false) }
    val countdownLabel = resolveCountdownLabel(
        applicationLabel = item.dDayLabel,
        eventLabel = item.eventDDayLabel,
        showEvent = showEventDDay.value
    )

    Column(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.78f)
                .clip(RoundedCornerShape(7.dp))
                .background(MyPageSectionBookmarkPreviewImageBackground)
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
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(27.dp)
                    .clip(CircleShape)
                    .background(eventTypeColor(item.eventTypeId))
            )
            Spacer(modifier = Modifier.width(9.dp))
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink100,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(7.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.applyPeriodDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink60,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.organization.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink60,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// 추가 찜 목록 안내
@Composable
private fun MoreBookmarksPreviewCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(180.dp)
            .height(202.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(7.dp),
        color = Color(0xFFF7F7F7),
        border = androidx.compose.foundation.BorderStroke(1.dp, MyPageSectionDividerColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "더 자세한 내용은\n찜 목록 페이지에서\n확인해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink60,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "찜 목록 이동",
                tint = MyPageSectionMutedIconColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// 메모 목록 미리보기 영역
@Composable
internal fun MemosPreviewSection(
    items: List<MyPageMemoItem>,
    isLoading: Boolean,
    errorMessage: String?,
    icon: @Composable () -> Unit,
    onHeaderClick: () -> Unit,
    onMemoClick: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ShortcutSectionHeader(
            title = "내 메모 목록",
            icon = icon,
            onClick = onHeaderClick
        )
        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                }
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink60,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            items.isEmpty() -> {
                Text(
                    text = "아직 메모가 없습니다.\n관심있는 행사에 메모를 작성해보세요.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink60,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(
                        items = items,
                        key = { item -> item.id }
                    ) { item ->
                        MemoPreviewCard(
                            item = item,
                            onClick = { onMemoClick(item.eventId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoPreviewCard(
    item: MyPageMemoItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(215.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = item.content,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink100,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(74.dp))
        Text(
            text = item.eventTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink60,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = item.updatedDateDisplay,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink60,
            fontSize = 12.sp
        )
        if (item.tagNames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                maxLines = 1
            ) {
                item.tagNames.take(3).forEach { tagName ->
                    MemoPreviewTagChip(text = tagName)
                }
            }
        }
    }
}

@Composable
private fun MemoPreviewTagChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFE8E8E8))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink60,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// 버그 신고 영역
@Composable
internal fun BugReportSection(
    title: String,
    content: String,
    isSubmitting: Boolean,
    onTitleChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onSubmitClick: () -> Unit
) {
    val canSubmit = title.isNotBlank() && content.isNotBlank() && !isSubmitting

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.BugReport,
                contentDescription = null,
                tint = Ink100,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "버그 신고",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink100,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "사용 중 불편한 문제를 알려주세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = Ink60,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        BugTextField(
            value = title,
            onValueChange = onTitleChanged,
            placeholder = "제목",
            minLines = 1
        )
        Spacer(modifier = Modifier.height(8.dp))
        BugTextField(
            value = content,
            onValueChange = onContentChanged,
            placeholder = "문제가 발생한 상황을 자세히 적어주세요.",
            minLines = 5
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onSubmitClick,
                enabled = canSubmit,
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A4A4A),
                    contentColor = PureWhite,
                    disabledContainerColor = Color(0xFFBDBDBD),
                    disabledContentColor = PureWhite
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 15.dp,
                    vertical = 0.dp
                ),
                modifier = Modifier.height(26.dp)
            ) {
                Text(
                    text = "신고하기",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 버그 입력칸 영역
@Composable
private fun BugTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int
) {
    val shape = RoundedCornerShape(5.dp)
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = Ink100,
        fontSize = 11.sp
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (minLines == 1) 34.dp else 86.dp)
            .background(PureWhite, shape)
            .border(1.dp, MyPageSectionBorderColor, shape),
        minLines = minLines,
        maxLines = if (minLines == 1) 1 else Int.MAX_VALUE,
        textStyle = textStyle,
        cursorBrush = SolidColor(Ink100),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle,
                        color = Color(0xFF9D9D9D)
                    )
                }
                innerTextField()
            }
        }
    )
}

// 계정 작업 영역
@Composable
internal fun Divider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MyPageSectionDividerColor)
    )
}
