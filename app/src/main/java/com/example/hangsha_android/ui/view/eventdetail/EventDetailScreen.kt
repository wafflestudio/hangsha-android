package com.example.hangsha_android.ui.view.eventdetail

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.Button
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
    showMemberFeatures: Boolean,
    onNavigateBack: () -> Unit,
    onBookmarkClick: () -> Unit,
    onMemoClick: () -> Unit,
    onMemoContentChanged: (String) -> Unit,
    onMemoTagInputChanged: (String) -> Unit,
    onAddMemoTag: () -> Unit,
    onRemoveMemoTag: (String) -> Unit,
    onSaveMemoClick: () -> Unit,
    onOpenBugReport: () -> Unit,
    onDismissBugReport: () -> Unit,
    onBugReportTitleChanged: (String) -> Unit,
    onBugReportContentChanged: (String) -> Unit,
    onSubmitBugReport: () -> Unit,
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
                    showMemberFeatures = showMemberFeatures,
                    onNavigateBack = onNavigateBack,
                    onBookmarkClick = onBookmarkClick,
                    onMemoClick = onMemoClick,
                    onMemoContentChanged = onMemoContentChanged,
                    onMemoTagInputChanged = onMemoTagInputChanged,
                    onAddMemoTag = onAddMemoTag,
                    onRemoveMemoTag = onRemoveMemoTag,
                    onSaveMemoClick = onSaveMemoClick,
                    onBugReportClick = onOpenBugReport
                )
            }
        }
    }

    if (uiState.isBugReportDialogOpen) {
        EventBugReportDialog(
            uiState = uiState,
            onDismissRequest = onDismissBugReport,
            onTitleChanged = onBugReportTitleChanged,
            onContentChanged = onBugReportContentChanged,
            onSubmitClick = onSubmitBugReport
        )
    }
}

@Composable
private fun EventDetailContent(
    uiState: EventDetailUiState,
    item: EventDetailItem,
    showMemberFeatures: Boolean,
    onNavigateBack: () -> Unit,
    onBookmarkClick: () -> Unit,
    onMemoClick: () -> Unit,
    onMemoContentChanged: (String) -> Unit,
    onMemoTagInputChanged: (String) -> Unit,
    onAddMemoTag: () -> Unit,
    onRemoveMemoTag: (String) -> Unit,
    onSaveMemoClick: () -> Unit,
    onBugReportClick: () -> Unit
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
        if (showMemberFeatures) {
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
                    text = item.eventPeriodDisplay,
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

        // 메모 기능 - 별도 파일로 분리
        if (showMemberFeatures) {
            item {
                EventDetailMemoSection(
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
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            EventBugReportButton(onClick = onBugReportClick)
            Spacer(modifier = Modifier.height(24.dp))
        }
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
