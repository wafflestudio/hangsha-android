package com.example.hangsha_android.data.local

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProfileImageFilePreparerTest {

    @Test
    fun acceptsImageAtExactSizeLimit() {
        val bytes = ByteArray(MAX_PROFILE_IMAGE_BYTES.toInt())
        val output = ByteArrayOutputStream()

        val copied = ByteArrayInputStream(bytes).copyToWithLimit(
            output = output,
            maxBytes = MAX_PROFILE_IMAGE_BYTES
        )

        assertEquals(MAX_PROFILE_IMAGE_BYTES, copied)
        assertEquals(bytes.size, output.size())
    }

    @Test
    fun rejectsImageLargerThanSizeLimit() {
        val bytes = ByteArray(MAX_PROFILE_IMAGE_BYTES.toInt() + 1)

        assertThrows(IllegalArgumentException::class.java) {
            ByteArrayInputStream(bytes).copyToWithLimit(
                output = ByteArrayOutputStream(),
                maxBytes = MAX_PROFILE_IMAGE_BYTES
            )
        }
    }
}
