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
    fun compatibilityScript_removesGoogleLoginButton() {
        val script = buildSnuttCompatibilityScript("https://snutt.wafflestudio.com")

        assertTrue(script.contains("[data-testid=\"google-login\"]"))
        assertTrue(script.contains("?.remove()"))
    }
}
