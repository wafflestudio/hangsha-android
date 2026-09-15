package com.example.hangsha_android.ui.view.timetable

import org.junit.Assert.assertEquals
import org.junit.Test

class SnuttNavigationPolicyTest {
    private val snuttOrigin = "https://snutt.wafflestudio.com"

    @Test
    fun classifyPopup_allowsBlankSnuttAndSupportedProviderPages() {
        val allowedUrls = listOf(
            "about:blank",
            "https://snutt.wafflestudio.com/timetable-picker",
            "https://accounts.kakao.com/login",
            "https://kauth.kakao.com/oauth/authorize",
            "https://www.facebook.com/dialog/oauth",
            "https://staticxx.facebook.com/connect/xd_arbiter/"
        )

        allowedUrls.forEach { url ->
            assertEquals(
                url,
                SnuttNavigationDecision.ALLOW,
                SnuttNavigationPolicy.classifyPopup(url, snuttOrigin)
            )
        }
    }

    @Test
    fun classifyPopup_blocksGoogleAuthenticationPages() {
        val googleUrls = listOf(
            "https://accounts.google.com/o/oauth2/v2/auth",
            "https://accounts.google.co.kr/o/oauth2/auth",
            "https://oauth.googleusercontent.com/callback"
        )

        googleUrls.forEach { url ->
            assertEquals(
                url,
                SnuttNavigationDecision.BLOCK_GOOGLE,
                SnuttNavigationPolicy.classifyPopup(url, snuttOrigin)
            )
        }
    }

    @Test
    fun classifyPopup_blocksUntrustedLookalikesAndNonHttpsUrls() {
        val blockedUrls = listOf(
            "https://facebook.com.attacker.example/login",
            "https://kakao.com.attacker.example/login",
            "http://accounts.kakao.com/login",
            "intent://oauth",
            "not a url"
        )

        blockedUrls.forEach { url ->
            assertEquals(
                url,
                SnuttNavigationDecision.BLOCK_EXTERNAL,
                SnuttNavigationPolicy.classifyPopup(url, snuttOrigin)
            )
        }
    }

    @Test
    fun classifyMainFrame_allowsSnuttAndFacebookRedirectFlowOnly() {
        val expected = mapOf(
            "https://snutt.wafflestudio.com/timetable-picker?facebookdirect=state" to
                SnuttNavigationDecision.ALLOW,
            "https://m.facebook.com/dialog/oauth" to SnuttNavigationDecision.ALLOW,
            "https://accounts.kakao.com/login" to SnuttNavigationDecision.BLOCK_EXTERNAL,
            "https://accounts.google.com/o/oauth2/auth" to SnuttNavigationDecision.BLOCK_GOOGLE,
            "https://facebook.com.attacker.example/login" to
                SnuttNavigationDecision.BLOCK_EXTERNAL
        )

        expected.forEach { (url, decision) ->
            assertEquals(
                url,
                decision,
                SnuttNavigationPolicy.classifyMainFrame(url, snuttOrigin)
            )
        }
    }
}
