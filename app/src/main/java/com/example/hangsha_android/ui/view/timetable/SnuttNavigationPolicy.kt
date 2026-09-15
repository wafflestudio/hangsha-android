package com.example.hangsha_android.ui.view.timetable

import java.net.URI

internal enum class SnuttNavigationDecision {
    ALLOW,
    BLOCK_GOOGLE,
    BLOCK_EXTERNAL
}

internal object SnuttNavigationPolicy {
    fun classifyPopup(url: String, snuttOrigin: String): SnuttNavigationDecision {
        if (url == ABOUT_BLANK) return SnuttNavigationDecision.ALLOW

        return classifyHttpsUrl(url, snuttOrigin, POPUP_PROVIDER_DOMAINS)
    }

    fun classifyMainFrame(url: String, snuttOrigin: String): SnuttNavigationDecision {
        return classifyHttpsUrl(url, snuttOrigin, FACEBOOK_DOMAINS)
    }

    fun isFacebookUrl(url: String): Boolean {
        val uri = url.toHttpsUriOrNull() ?: return false
        return FACEBOOK_DOMAINS.any { uri.host.lowercase().matchesDomain(it) }
    }

    private fun classifyHttpsUrl(
        url: String,
        snuttOrigin: String,
        allowedProviderDomains: Set<String>
    ): SnuttNavigationDecision {
        val uri = url.toHttpsUriOrNull() ?: return SnuttNavigationDecision.BLOCK_EXTERNAL
        val snuttUri = snuttOrigin.toHttpsUriOrNull()
            ?: return SnuttNavigationDecision.BLOCK_EXTERNAL
        if (uri.hasSameOrigin(snuttUri)) return SnuttNavigationDecision.ALLOW

        val host = uri.host.lowercase()
        return when {
            isGoogleHost(host) -> SnuttNavigationDecision.BLOCK_GOOGLE
            allowedProviderDomains.any(host::matchesDomain) -> SnuttNavigationDecision.ALLOW
            else -> SnuttNavigationDecision.BLOCK_EXTERNAL
        }
    }

    private fun isGoogleHost(host: String): Boolean {
        return host == "accounts.google.com" ||
            host.startsWith("accounts.google.") ||
            host.matchesDomain("googleusercontent.com")
    }
}

private fun String.toHttpsUriOrNull(): URI? {
    val uri = runCatching { URI(this) }.getOrNull() ?: return null
    if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) return null
    if (uri.userInfo != null) return null
    return uri
}

private fun URI.hasSameOrigin(other: URI): Boolean {
    return scheme.equals(other.scheme, ignoreCase = true) &&
        host.equals(other.host, ignoreCase = true) &&
        effectiveHttpsPort() == other.effectiveHttpsPort()
}

private fun URI.effectiveHttpsPort(): Int = if (port == -1) 443 else port

private fun String.matchesDomain(domain: String): Boolean {
    return this == domain || endsWith(".$domain")
}

private const val ABOUT_BLANK = "about:blank"
private val FACEBOOK_DOMAINS = setOf(
    "facebook.com",
    "facebook.net",
    "fb.com"
)
private val POPUP_PROVIDER_DOMAINS = FACEBOOK_DOMAINS + "kakao.com"
