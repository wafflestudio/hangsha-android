package com.example.hangsha_android.ui.view.eventdetail

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.hangsha_android.ui.theme.Cream10
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Ink90
import com.example.hangsha_android.ui.theme.Ink100
import com.example.hangsha_android.ui.theme.PureWhite

@Composable
fun EventDetailScreen(
    uiState: EventDetailUiState,
    onNavigateBack: () -> Unit,
    onBookmarkClick: () -> Unit,
    onMemoClick: () -> Unit,
    onMemoContentChanged: (String) -> Unit,
    onMemoTagInputChanged: (String) -> Unit,
    onAddMemoTag: () -> Unit,
    onRemoveMemoTag: (String) -> Unit,
    onSaveMemoClick: () -> Unit,
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
                // 본문
                EventDetailContent(
                    uiState = uiState,
                    item = uiState.item,
                    onNavigateBack = onNavigateBack,
                    onBookmarkClick = onBookmarkClick,
                    onMemoClick = onMemoClick,
                    onMemoContentChanged = onMemoContentChanged,
                    onMemoTagInputChanged = onMemoTagInputChanged,
                    onAddMemoTag = onAddMemoTag,
                    onRemoveMemoTag = onRemoveMemoTag,
                    onSaveMemoClick = onSaveMemoClick
                )
            }
        }
    }
}

@Composable
private fun EventDetailContent(
    uiState: EventDetailUiState,
    item: EventDetailItem,
    onNavigateBack: () -> Unit,
    onBookmarkClick: () -> Unit,
    onMemoClick: () -> Unit,
    onMemoContentChanged: (String) -> Unit,
    onMemoTagInputChanged: (String) -> Unit,
    onAddMemoTag: () -> Unit,
    onRemoveMemoTag: (String) -> Unit,
    onSaveMemoClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 뒤로 가기 버튼
        item {
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                onClick = onNavigateBack,
                shape = RoundedCornerShape(14.dp),
                color = PureWhite,
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Ink60
                    )
                }
            }
        }

        // 네모난 사진
        item {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(370.dp, 241.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(370.dp, 241.dp)
                        .clip(RoundedCornerShape(18.dp))
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
        }

        // 북마크 로고
        item {
            Icon(
                imageVector = if (item.isBookmarked) {
                    Icons.Rounded.Bookmark
                } else {
                    Icons.Rounded.BookmarkBorder
                },
                contentDescription = "Bookmark",
                tint = Ink60,
                modifier = Modifier
                    .size(32.dp)
                    .clickable(onClick = onBookmarkClick)
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 이벤트 이름
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink100,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                // 이벤트 기간
                Text(
                    text = item.eventEndDisplay,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 16.sp,
                    color = Ink90
                )
            }
        }

        // D-day 라벨과 분류 라벨
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlineBadge(text = item.dDayLabel)
                FilledBadge(
                    text = item.eventTypeLabel,
                    backgroundColor = item.eventTypeColor
                )
            }
        }

        // Org 이름
        item {
            Text(
                text = item.organization ?: "-",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink90
            )
        }

        // 지원 링크
        item {
            val applyLink = item.applyLink
            Text(
                text = "지원 링크로 이동하기",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink60,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.then(
                    if (!applyLink.isNullOrBlank()) {
                        Modifier.clickable { uriHandler.openUri(applyLink) }
                    } else {
                        Modifier
                    }
                )
            )
        }

        // WebView로 Detail Html Content - 세부 설명
        item {
            EventDetailHtmlContent(
                html = item.detail,
                onOpenLink = { url ->
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }
            )
        }

        item {
            EventDetailMemoSectionWithSavedMemo(
                isOpen = uiState.isMemoEditorOpen,
                savedMemo = uiState.savedMemo,
                content = uiState.memoContent,
                tagInput = uiState.memoTagInput,
                tagNames = uiState.memoTagNames,
                isSaving = uiState.isMemoSaving,
                onOpen = onMemoClick,
                onContentChanged = onMemoContentChanged,
                onTagInputChanged = onMemoTagInputChanged,
                onAddTag = onAddMemoTag,
                onRemoveTag = onRemoveMemoTag,
                onSaveClick = onSaveMemoClick
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EventDetailMemoSectionWithSavedMemo(
    isOpen: Boolean,
    savedMemo: EventDetailMemo?,
    content: String,
    tagInput: String,
    tagNames: List<String>,
    isSaving: Boolean,
    onOpen: () -> Unit,
    onContentChanged: (String) -> Unit,
    onTagInputChanged: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    if (!isOpen) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MemoSectionHeader()
            if (savedMemo == null) {
                MemoPlaceholder()
            } else {
                MemoDisplay(
                    content = savedMemo.content,
                    tagNames = savedMemo.tagNames
                )
            }
        }
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, Ink60.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = Ink60,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isSaving) "저장 중" else "저장하기",
                    modifier = Modifier
                        .clickable(enabled = !isSaving, onClick = onSaveClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    color = Ink90,
                    fontWeight = FontWeight.SemiBold
                )
            }

            MemoTextInput(
                value = content,
                onValueChange = onContentChanged,
                placeholder = "메모를 입력하세요",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(102.dp)
            )

            if (tagNames.isNotEmpty()) {
                MemoTagRow(
                    tagNames = tagNames,
                    onRemoveTag = onRemoveTag
                )
            }

            // TODO: Connect tag creation/update API for existing memos when the backend contract is ready.
            // TODO: 태그와 관련된 모든 것: 생성, 수정, 삭제 등등을 모두 하지 않음
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MemoTextInput(
                    value = tagInput,
                    onValueChange = onTagInputChanged,
                    placeholder = "태그를 입력하세요",
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    singleLine = true
                )
                Button(
                    onClick = onAddTag,
                    enabled = tagInput.isNotBlank() && !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = Ink90,
                        disabledContainerColor = PureWhite,
                        disabledContentColor = Ink60.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, Ink60.copy(alpha = 0.24f))
                ) {
                    Text(text = "추가")
                }
            }
        }
    }
}

@Composable
private fun MemoSectionHeader() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = null,
            tint = Ink60,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "메모하기",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            color = Ink90
        )
    }
}

@Composable
private fun MemoPlaceholder() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, Ink60.copy(alpha = 0.24f))
    ) {
        Text(
            text = "메모를 입력하세요",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 15.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            color = Ink60.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun MemoDisplay(
    content: String,
    tagNames: List<String>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, Ink60.copy(alpha = 0.24f))
        ) {
            Text(
                text = content,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = Ink100
            )
        }
        if (tagNames.isNotEmpty()) {
            MemoTagRow(tagNames = tagNames, onRemoveTag = null)
        }
    }
}

@Composable
private fun MemoTagRow(
    tagNames: List<String>,
    onRemoveTag: ((String) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tagNames.forEach { tagName ->
            MemoTagChip(
                text = tagName,
                onClick = onRemoveTag?.let { remove -> { remove(tagName) } }
            )
        }
    }
}

@Composable
private fun EventDetailMemoSection(
    isOpen: Boolean,
    content: String,
    tagInput: String,
    tagNames: List<String>,
    isSaving: Boolean,
    onOpen: () -> Unit,
    onContentChanged: (String) -> Unit,
    onTagInputChanged: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    if (!isOpen) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = Ink60,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "메모하기",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = Ink90
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, Ink60.copy(alpha = 0.24f))
            ) {
                Text(
                    text = "메모를 입력하세요",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 15.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = Ink60.copy(alpha = 0.55f)
                )
            }
        }
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, Ink60.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = Ink60,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isSaving) "저장 중" else "저장하기",
                    modifier = Modifier
                        .clickable(enabled = !isSaving, onClick = onSaveClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    color = Ink90,
                    fontWeight = FontWeight.SemiBold
                )
            }

            MemoTextInput(
                value = content,
                onValueChange = onContentChanged,
                placeholder = "메모를 입력하세요",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(102.dp)
            )

            if (tagNames.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tagNames.forEach { tagName ->
                        MemoTagChip(
                            text = tagName,
                            onClick = { onRemoveTag(tagName) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MemoTextInput(
                    value = tagInput,
                    onValueChange = onTagInputChanged,
                    placeholder = "태그를 입력하세요",
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    singleLine = true
                )
                Button(
                    onClick = onAddTag,
                    enabled = tagInput.isNotBlank() && !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = Ink90,
                        disabledContainerColor = PureWhite,
                        disabledContentColor = Ink60.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, Ink60.copy(alpha = 0.24f))
                ) {
                    Text(text = "추가")
                }
            }
        }
    }
}

@Composable
private fun MemoTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, Ink60.copy(alpha = 0.28f))
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = Ink100
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            color = Ink60.copy(alpha = 0.55f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun MemoTagChip(
    text: String,
    onClick: (() -> Unit)? = null
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$text",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = Ink90
            )
            if (onClick != null) {
                Text(
                    text = "x",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    color = Ink60
                )
            }
        }
    }

    if (onClick == null) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Cream10,
            border = BorderStroke(1.dp, Ink60.copy(alpha = 0.18f)),
            content = content
        )
    } else {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp),
            color = Cream10,
            border = BorderStroke(1.dp, Ink60.copy(alpha = 0.18f)),
            content = content
        )
    }
}

// 세부 설명
@Composable
private fun EventDetailHtmlContent(
    html: String?,
    onOpenLink: (String) -> Unit
) {
    if (html.isNullOrBlank()) {
        Text(
            text = "-",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            color = Ink90
        )
        return
    }

    var webViewHeightPx by remember(html) { mutableIntStateOf(1) }
    val density = LocalDensity.current
    val wrappedHtml = remember(html) { buildEventDetailHtml(html) }

    AndroidView(
        factory = { androidContext ->
            WebView(androidContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER
                settings.apply {
                    javaScriptEnabled = false
                    domStorageEnabled = false
                    loadsImagesAutomatically = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    defaultTextEncodingName = "utf-8"
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        val url = request.url.toString()
                        onOpenLink(url)
                        return true
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        view.postDelayed({
                            webViewHeightPx = (view.contentHeight * view.scale)
                                .toInt()
                                .coerceAtLeast(1)
                        }, 50)
                    }
                }
                tag = wrappedHtml
                loadDataWithBaseURL(null, wrappedHtml, "text/html", "utf-8", null)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(with(density) { webViewHeightPx.toDp() }),
        update = { webView ->
            if (webView.tag != wrappedHtml) {
                webView.tag = wrappedHtml
                webView.loadDataWithBaseURL(null, wrappedHtml, "text/html", "utf-8", null)
            }
        }
    )
}

// 웹뷰로 설명문을 감싸기
private fun buildEventDetailHtml(bodyHtml: String): String {
    return """
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    background: transparent;
                    color: #1A1D1A;
                    font-family: sans-serif;
                    font-size: 15px;
                    line-height: 1.6;
                    word-break: break-word;
                    overflow-wrap: break-word;
                }
                p, div, span {
                    color: #1A1D1A;
                    line-height: 1.6;
                }
                img {
                    max-width: 100%;
                    height: auto;
                }
                a {
                    color: #5E615B;
                    text-decoration: underline;
                }
            </style>
        </head>
        <body>$bodyHtml</body>
        </html>
    """.trimIndent()
}

// D-day 라벨
@Composable
private fun OutlineBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = PureWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, Ink60.copy(alpha = 0.24f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Ink90
        )
    }
}

// 색칠한 라벨: 여기서는 오직 분류
@Composable
private fun FilledBadge(
    text: String,
    backgroundColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Ink100
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
