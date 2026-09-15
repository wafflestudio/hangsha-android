package com.example.hangsha_android.ui.view.timetable

import android.net.Uri
import androidx.core.net.toUri
import androidx.webkit.WebViewFeature
import com.example.hangsha_android.BuildConfig

internal data class SnuttPickerConfig(
    val snuttOrigin: String,
    val pickerUrl: String,
    val documentStartScript: String
) {
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
                documentStartScript = buildSnuttCompatibilityScript(snuttOrigin)
            )
        }
    }
}

private fun Uri.toHttpsOrigin(): String? {
    if (scheme != "https" || host.isNullOrBlank() || userInfo != null) return null
    val portPart = if (port == -1 || port == 443) "" else ":$port"
    return "https://$host$portPart"
}
