package com.example.hangsha_android.ui.view.timetable

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.hangsha_android.BuildConfig
import com.example.hangsha_android.ui.theme.Ink100
import com.example.hangsha_android.ui.theme.PureWhite

@Composable
internal fun SnuttTimetablePickerDialog(
    onClose: () -> Unit,
    onTimetableSelected: (SnuttTimetable) -> Unit,
    onError: (String) -> Unit
) {
    val config = remember { SnuttPickerConfig.create() }
    val currentOnClose by rememberUpdatedState(onClose)
    val currentOnTimetableSelected by rememberUpdatedState(onTimetableSelected)
    val currentOnError by rememberUpdatedState(onError)

    if (config == null) {
        LaunchedEffect(Unit) {
            currentOnError(UNSUPPORTED_WEBVIEW_MESSAGE)
        }
        return
    }

    Dialog(
        onDismissRequest = currentOnClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SnuttPickerWebView(
                config = config,
                onTimetableSelected = currentOnTimetableSelected,
                onError = currentOnError
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = CircleShape,
                color = PureWhite.copy(alpha = 0.92f),
                shadowElevation = 4.dp
            ) {
                IconButton(onClick = currentOnClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "\uB2EB\uAE30",
                        tint = Ink100
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "RequiresFeature")
@Composable
private fun SnuttPickerWebView(
    config: SnuttPickerConfig,
    onTimetableSelected: (SnuttTimetable) -> Unit,
    onError: (String) -> Unit
) {
    val currentOnTimetableSelected by rememberUpdatedState(onTimetableSelected)
    val currentOnError by rememberUpdatedState(onError)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebStorage.getInstance().deleteOrigin(config.snuttOrigin)

            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(true)
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW

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
                        currentOnError(SOCIAL_LOGIN_UNSUPPORTED_MESSAGE)
                        return false
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
                        if (!request.isForMainFrame || config.isAllowedUrl(request.url)) {
                            return false
                        }
                        currentOnError(SOCIAL_LOGIN_UNSUPPORTED_MESSAGE)
                        return true
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        if (request.isForMainFrame) currentOnError(PAGE_LOAD_ERROR)
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse
                    ) {
                        if (request.isForMainFrame) {
                            currentOnError(PAGE_LOAD_ERROR + " (" + errorResponse.statusCode + ")")
                        }
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        view.scrollTo(0, 0)
                    }
                }
                loadUrl(config.pickerUrl)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.removeAllViews()
            webView.destroy()
        }
    )
}

private data class SnuttPickerConfig(
    val snuttOrigin: String,
    val pickerUrl: String,
    val documentStartScript: String
) {
    private val snuttOriginUri = snuttOrigin.toUri()

    fun isAllowedUrl(uri: Uri): Boolean {
        return uri.scheme == snuttOriginUri.scheme &&
            uri.host == snuttOriginUri.host &&
            effectivePort(uri) == effectivePort(snuttOriginUri)
    }

    companion object {
        fun create(): SnuttPickerConfig? {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) ||
                !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
            ) {
                return null
            }

            val baseUri = BuildConfig.SNUTT_BASE_URL.toUri()
            val originUri = BuildConfig.TIMETABLE_PICKER_ORIGIN.toUri()
            val snuttOrigin = baseUri.toHttpsOrigin() ?: return null
            val pickerOrigin = originUri.toHttpsOrigin() ?: return null
            return SnuttPickerConfig(
                snuttOrigin = snuttOrigin,
                pickerUrl = baseUri.buildUpon()
                    .appendPath("timetable-picker")
                    .appendQueryParameter("origin", pickerOrigin)
                    .build()
                    .toString(),
                documentStartScript = buildDocumentStartScript(snuttOrigin)
            )
        }
    }
}

private fun buildDocumentStartScript(snuttOrigin: String): String {
    val originLiteral = snuttOrigin.asJavaScriptString()
    return """
        (() => {
          const allowedOrigin = $originLiteral;
          if (window.location.origin !== allowedOrigin ||
              window.location.pathname !== '/timetable-picker') return;

          if (window.opener == null) window.opener = window;

          const applyMobileLayout = () => {
            const idInput = document.querySelector('[data-testid="id-input"]');
            if (idInput) {
              const form = idInput.closest('form');
              const container = form && form.parentElement;
              const wrapper = container && container.parentElement;
              if (wrapper && container && form) {
                wrapper.style.height = 'auto';
                wrapper.style.minHeight = '100vh';
                wrapper.style.alignItems = 'flex-start';
                wrapper.style.overflowY = 'auto';
                wrapper.style.padding = '16px 0';
                container.style.margin = '0 auto';
                container.style.padding = '24px 20px';
                let sibling = form.nextElementSibling;
                while (sibling) {
                  sibling.style.display = 'none';
                  sibling = sibling.nextElementSibling;
                }
              }
            }

            const pickerWrapper = document.querySelector('#root')?.firstElementChild;
            if (!idInput && pickerWrapper?.children.length === 2 && window.innerWidth <= 700) {
              const leftPane = pickerWrapper.children[0];
              const rightPane = pickerWrapper.children[1];
              if (leftPane && rightPane) {
                const wrapper = pickerWrapper;
                wrapper.style.flexDirection = 'column';
                wrapper.style.height = 'auto';
                wrapper.style.minHeight = '100vh';
                wrapper.style.overflowY = 'auto';
                leftPane.style.width = '100%';
                leftPane.style.height = '38vh';
                leftPane.style.minHeight = '220px';
                leftPane.style.borderRight = 'none';
                leftPane.style.borderBottom = '10px solid rgb(232, 235, 240)';
                leftPane.style.boxShadow = '0 3px 10px rgba(0, 0, 0, 0.10)';
                leftPane.style.boxSizing = 'border-box';
                leftPane.style.position = 'relative';
                leftPane.style.zIndex = '1';
                rightPane.style.width = '100%';
                rightPane.style.minHeight = '62vh';
                rightPane.style.padding = '16px 12px 24px';
                rightPane.style.overflowX = 'auto';
                rightPane.style.position = 'relative';
                rightPane.setAttribute('data-hangsha-picker-preview', '');

                const confirmButton = rightPane.querySelector(
                  '[data-testid="timetable-picker-confirm"]'
                );
                if (confirmButton &&
                    !rightPane.querySelector('[data-hangsha-snutt-logout]')) {
                  const logoutButton = document.createElement('button');
                  logoutButton.type = 'button';
                  logoutButton.textContent = 'SNUTT에서 로그아웃';
                  logoutButton.setAttribute('data-hangsha-snutt-logout', '');
                  logoutButton.setAttribute('aria-label', 'SNUTT에서 로그아웃');
                  Object.assign(logoutButton.style, {
                    position: 'absolute',
                    left: '12px',
                    bottom: '24px',
                    height: '36px',
                    padding: '0 16px',
                    border: '1px solid #9a9a9a',
                    borderRadius: '18px',
                    color: '#666',
                    backgroundColor: 'transparent',
                    cursor: 'pointer',
                    fontFamily: 'inherit',
                    fontSize: '13px',
                    lineHeight: '34px',
                    whiteSpace: 'nowrap'
                  });
                  logoutButton.addEventListener('click', () => {
                    localStorage.removeItem('snutt_token');
                    sessionStorage.removeItem('snutt_token');
                    window.location.reload();
                  });
                  rightPane.appendChild(logoutButton);
                }

                const styleId = 'hangsha-picker-mobile-style';
                if (!document.getElementById(styleId)) {
                  const style = document.createElement('style');
                  style.id = styleId;
                  style.textContent = [
                    '@media (max-width: 700px) {',
                    '  [data-hangsha-picker-preview]::before {',
                    '    content: "선택한 시간표";',
                    '    display: block;',
                    '    flex: 0 0 auto;',
                    '    padding: 2px 4px 0;',
                    '    color: rgba(0, 0, 0, 0.62);',
                    '    font-size: 14px;',
                    '    font-weight: 600;',
                    '    letter-spacing: -0.2px;',
                    '  }',
                    '}'
                  ].join('\n');
                  (document.head || document.documentElement).appendChild(style);
                }
              }
            }
          };

          new MutationObserver(applyMobileLayout).observe(
            document.documentElement,
            { childList: true, subtree: true }
          );
          document.addEventListener('DOMContentLoaded', applyMobileLayout);
        })();
    """.trimIndent()
}

private fun Uri.toHttpsOrigin(): String? {
    if (scheme != "https" || host.isNullOrBlank() || userInfo != null) return null
    val portPart = if (port == -1 || port == 443) "" else ":$port"
    return "https://$host$portPart"
}

private fun effectivePort(uri: Uri): Int {
    return if (uri.port != -1) uri.port else if (uri.scheme == "https") 443 else 80
}

private fun String.asJavaScriptString(): String {
    val slash = 92.toChar().toString()
    return "'" + replace(slash, slash + slash).replace("'", slash + "'") + "'"
}

private const val REACT_NATIVE_BRIDGE_NAME = "ReactNativeWebView"
private const val UNSUPPORTED_WEBVIEW_MESSAGE =
    "\u0041\u006E\u0064\u0072\u006F\u0069\u0064 \u0053\u0079\u0073\u0074\u0065\u006D \u0057\u0065\u0062\u0056\u0069\u0065\u0077\uB97C \uC5C5\uB370\uC774\uD2B8\uD55C \uB4A4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."
private const val INVALID_MESSAGE =
    "\u0053\u004E\u0055\u0054\u0054\uC5D0\uC11C \uBC1B\uC740 \uC2DC\uAC04\uD45C \uD615\uC2DD\uC744 \uD655\uC778\uD560 \uC218 \uC5C6\uC5B4\uC694."
private const val WEBVIEW_CRASH_MESSAGE =
    "\u0053\u004E\u0055\u0054\u0054 \uD654\uBA74\uC744 \uC5F4\uB358 \uC911 \uC624\uB958\uAC00 \uBC1C\uC0DD\uD588\uC5B4\uC694."
private const val PAGE_LOAD_ERROR =
    "\u0053\u004E\u0055\u0054\u0054 \uD398\uC774\uC9C0\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC5B4\uC694."
private const val SOCIAL_LOGIN_UNSUPPORTED_MESSAGE =
    "\uC18C\uC15C \uB85C\uADF8\uC778\uC740 \uB2E4\uC74C \uC5C5\uB370\uC774\uD2B8\uC5D0\uC11C \uC9C0\uC6D0\uD560 \uC608\uC815\uC774\uC5D0\uC694. " +
        "\uC774\uBC88\uC5D0\uB294 \u0053\u004E\u0055\u0054\u0054 \uC544\uC774\uB514\u00B7\uBE44\uBC00\uBC88\uD638\uB85C \uB85C\uADF8\uC778\uD574 \uC8FC\uC138\uC694."
