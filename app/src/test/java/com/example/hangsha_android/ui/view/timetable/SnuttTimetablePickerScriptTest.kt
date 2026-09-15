package com.example.hangsha_android.ui.view.timetable

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnuttTimetablePickerScriptTest {
    @Test
    fun documentStartScript_doesNotHideActionsFollowingLoginForm() {
        val script = buildSnuttCompatibilityScript("https://snutt.wafflestudio.com")

        assertFalse(script.contains("form.nextElementSibling"))
        assertFalse(script.contains("sibling.style.display = 'none'"))
    }

    @Test
    fun compatibilityScript_addsGooglePolicyNoticeAfterKakaoLoginButton() {
        val script = buildSnuttCompatibilityScript("https://snutt.wafflestudio.com")

        assertTrue(script.contains("[data-testid=\"google-login\"]"))
        assertTrue(script.contains("[data-testid=\"kakao-login\"]"))
        assertTrue(script.contains("data-hangsha-google-policy-notice"))
        assertTrue(script.contains("Google 정책에 따라 앱 내 웹뷰에서는 Google 로그인을 이용할 수 없습니다."))
        assertTrue(script.contains("kakaoLoginButton.insertAdjacentElement('afterend', policyNotice)"))
    }
}
