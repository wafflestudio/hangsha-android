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
import androidx.compose.foundation.layout.width
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
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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

        item {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(228.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(228.dp)
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

        item {
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

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink100,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.eventEndDisplay,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = Ink90
                )
            }
        }

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

        item {
            Text(
                text = item.organization ?: "-",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink90
            )
        }

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

        item {
            EventDetailHtmlContent(
                html = item.detail,
                onOpenLink = { url ->
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

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
