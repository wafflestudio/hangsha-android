package com.example.hangsha_android.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class HangshaAdaptiveLayoutTest {
    @Test
    fun `width below 600dp is compact`() {
        val info = calculateHangshaWindowInfo(599.dp)

        assertEquals(HangshaWindowWidthSizeClass.Compact, info.widthSizeClass)
        assertEquals(16.dp, info.horizontalContentPadding)
        assertEquals(false, info.usesNavigationRail)
    }

    @Test
    fun `width from 600dp is medium`() {
        val info = calculateHangshaWindowInfo(600.dp)

        assertEquals(HangshaWindowWidthSizeClass.Medium, info.widthSizeClass)
        assertEquals(24.dp, info.horizontalContentPadding)
        assertEquals(true, info.usesNavigationRail)
    }

    @Test
    fun `width from 840dp is expanded`() {
        val info = calculateHangshaWindowInfo(840.dp)

        assertEquals(HangshaWindowWidthSizeClass.Expanded, info.widthSizeClass)
        assertEquals(32.dp, info.horizontalContentPadding)
        assertEquals(true, info.usesNavigationRail)
    }
}
