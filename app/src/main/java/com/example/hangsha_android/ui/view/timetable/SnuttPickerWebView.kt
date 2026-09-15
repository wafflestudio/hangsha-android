package com.example.hangsha_android.ui.view.timetable

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat

@SuppressLint("SetJavaScriptEnabled", "RequiresFeature")
@Composable
internal fun SnuttPickerWebView(
    config: SnuttPickerConfig,
    onTimetableSelected: (SnuttTimetable) -> Unit,
    onError: (String) -> Unit
) {
    val currentOnTimetableSelected by rememberUpdatedState(onTimetableSelected)
    val currentOnError by rememberUpdatedState(onError)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val container = FrameLayout(context)
            val cookieManager = CookieManager.getInstance().apply {
                setAcceptCookie(true)
            }
            var popupWebView: WebView? = null

            fun closePopup(webView: WebView? = popupWebView) {
                val popup = webView ?: return
                if (popupWebView === popup) popupWebView = null
                popup.dispose()
            }

            fun reportPopupError(webView: WebView, message: String) {
                container.post {
                    if (popupWebView !== webView) return@post
                    closePopup(webView)
                    currentOnError(message)
                }
            }

            fun configureWebView(webView: WebView, supportsMultipleWindows: Boolean) {
                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(supportsMultipleWindows)
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                }

                // The Facebook web SDK can require third-party cookies before its
                // mobile same-frame redirect. Cookie storage is shared by both WebViews.
                cookieManager.setAcceptThirdPartyCookies(webView, true)
            }

            fun createPopupWebView(): WebView {
                return WebView(context).apply {
                    configureWebView(this, supportsMultipleWindows = false)
                    webChromeClient = object : WebChromeClient() {
                        override fun onCloseWindow(window: WebView?) {
                            closePopup(window ?: popupWebView)
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            if (!request.isForMainFrame) return false
                            return when (
                                SnuttNavigationPolicy.classifyPopup(
                                    url = request.url.toString(),
                                    snuttOrigin = config.snuttOrigin
                                )
                            ) {
                                SnuttNavigationDecision.ALLOW -> false
                                SnuttNavigationDecision.BLOCK_GOOGLE -> {
                                    reportPopupError(view, GOOGLE_LOGIN_UNSUPPORTED_MESSAGE)
                                    true
                                }
                                SnuttNavigationDecision.BLOCK_EXTERNAL -> {
                                    reportPopupError(view, BLOCKED_POPUP_MESSAGE)
                                    true
                                }
                            }
                        }

                        override fun onRenderProcessGone(
                            view: WebView,
                            detail: RenderProcessGoneDetail
                        ): Boolean {
                            reportPopupError(view, SOCIAL_LOGIN_PAGE_ERROR)
                            return true
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError
                        ) {
                            if (request.isForMainFrame) {
                                reportPopupError(view, SOCIAL_LOGIN_PAGE_ERROR)
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView,
                            request: WebResourceRequest,
                            errorResponse: WebResourceResponse
                        ) {
                            if (request.isForMainFrame) {
                                reportPopupError(
                                    view,
                                    "$SOCIAL_LOGIN_PAGE_ERROR (${errorResponse.statusCode})"
                                )
                            }
                        }
                    }
                }
            }

            val pickerWebView = WebView(context).apply {
                configureWebView(this, supportsMultipleWindows = true)

                WebViewCompat.addWebMessageListener(
                    this,
                    REACT_NATIVE_BRIDGE_NAME,
                    setOf(config.snuttOrigin)
                ) { view, message, sourceOrigin, isMainFrame, _ ->
                    if (!isMainFrame || sourceOrigin.toString() != config.snuttOrigin) {
                        return@addWebMessageListener
                    }
                    val timetable = message.data?.let(SnuttTimetableMessageParser::parse)
                    view.post {
                        if (timetable == null) {
                            currentOnError(INVALID_MESSAGE)
                        } else {
                            currentOnTimetableSelected(timetable)
                        }
                    }
                }
                WebViewCompat.addDocumentStartJavaScript(
                    this,
                    config.documentStartScript,
                    setOf(config.snuttOrigin)
                )

                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message?
                    ): Boolean {
                        if (!isUserGesture || popupWebView != null || resultMsg == null) {
                            return false
                        }
                        val transport = resultMsg.obj as? WebView.WebViewTransport
                            ?: return false
                        val popup = createPopupWebView()
                        popupWebView = popup
                        container.addView(popup, matchParentLayoutParams())
                        transport.webView = popup
                        resultMsg.sendToTarget()
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun onRenderProcessGone(
                        view: WebView,
                        detail: RenderProcessGoneDetail
                    ): Boolean {
                        currentOnError(WEBVIEW_CRASH_MESSAGE)
                        return true
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        if (!request.isForMainFrame) return false

                        // react-facebook-login uses a same-frame redirect for mobile UAs.
                        return when (
                            SnuttNavigationPolicy.classifyMainFrame(
                                url = request.url.toString(),
                                snuttOrigin = config.snuttOrigin
                            )
                        ) {
                            SnuttNavigationDecision.ALLOW -> false
                            SnuttNavigationDecision.BLOCK_GOOGLE -> {
                                currentOnError(GOOGLE_LOGIN_UNSUPPORTED_MESSAGE)
                                true
                            }
                            SnuttNavigationDecision.BLOCK_EXTERNAL -> {
                                currentOnError(BLOCKED_POPUP_MESSAGE)
                                true
                            }
                        }
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        if (request.isForMainFrame) {
                            currentOnError(errorMessageFor(request.url.toString()))
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse
                    ) {
                        if (request.isForMainFrame) {
                            currentOnError(
                                "${errorMessageFor(request.url.toString())} " +
                                    "(${errorResponse.statusCode})"
                            )
                        }
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        view.scrollTo(0, 0)
                    }
                }
                loadUrl(config.pickerUrl)
            }

            container.addView(pickerWebView, matchParentLayoutParams())
            container
        },
        onRelease = { container ->
            while (container.childCount > 0) {
                val child = container.getChildAt(container.childCount - 1)
                if (child is WebView) child.dispose() else container.removeView(child)
            }
        }
    )
}

private fun WebView.dispose() {
    (parent as? ViewGroup)?.removeView(this)
    stopLoading()
    removeAllViews()
    destroy()
}

private fun matchParentLayoutParams() = FrameLayout.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    ViewGroup.LayoutParams.MATCH_PARENT
)

private fun errorMessageFor(url: String): String {
    return if (SnuttNavigationPolicy.isFacebookUrl(url)) {
        SOCIAL_LOGIN_PAGE_ERROR
    } else {
        PAGE_LOAD_ERROR
    }
}

private const val REACT_NATIVE_BRIDGE_NAME = "ReactNativeWebView"
private const val INVALID_MESSAGE =
    "SNUTT에서 받은 시간표 형식을 확인할 수 없어요."
private const val WEBVIEW_CRASH_MESSAGE =
    "SNUTT 화면을 열던 중 오류가 발생했어요."
private const val PAGE_LOAD_ERROR =
    "SNUTT 페이지를 불러오지 못했어요."
private const val GOOGLE_LOGIN_UNSUPPORTED_MESSAGE =
    "Google 로그인은 보안 정책상 앱 내 웹뷰에서 지원할 수 없어요. " +
        "SNUTT 아이디·비밀번호로 로그인해 주세요."
private const val BLOCKED_POPUP_MESSAGE =
    "허용되지 않은 외부 페이지로 이동하려 해 소셜 로그인 창을 닫았어요."
private const val SOCIAL_LOGIN_PAGE_ERROR =
    "소셜 로그인 페이지를 불러오지 못했어요."
