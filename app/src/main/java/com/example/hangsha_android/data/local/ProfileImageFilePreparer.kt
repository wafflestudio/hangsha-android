package com.example.hangsha_android.data.local

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

@Singleton
class ProfileImageFilePreparer @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    suspend fun prepare(uri: Uri): PreparedProfileImage = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri)
            ?.lowercase(Locale.ROOT)
            ?: throw IllegalArgumentException(
                "JPEG, PNG, WebP 형식의 이미지만 선택할 수 있습니다."
            )
        val extension = SUPPORTED_IMAGE_TYPES[mimeType]
            ?: throw IllegalArgumentException(
                "JPEG, PNG, WebP 형식의 이미지만 선택할 수 있습니다."
            )
        val file = File.createTempFile("profile-image-", extension, context.cacheDir)

        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) {
                    "선택한 이미지를 열 수 없습니다."
                }
                file.outputStream().use { output ->
                    input.copyToWithLimit(output, MAX_PROFILE_IMAGE_BYTES)
                }
            }
            PreparedProfileImage(file = file, mimeType = mimeType)
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
    }

    suspend fun delete(file: File) {
        withContext(NonCancellable + Dispatchers.IO) {
            file.delete()
        }
    }
}

data class PreparedProfileImage(
    val file: File,
    val mimeType: String
)

internal fun InputStream.copyToWithLimit(
    output: OutputStream,
    maxBytes: Long
): Long {
    require(maxBytes >= 0L)
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalBytes = 0L
    while (true) {
        val readBytes = read(buffer)
        if (readBytes < 0) break
        totalBytes += readBytes
        if (totalBytes > maxBytes) {
            throw IllegalArgumentException(
                "프로필 이미지는 10MiB 이하만 선택할 수 있습니다."
            )
        }
        output.write(buffer, 0, readBytes)
    }
    return totalBytes
}

internal const val MAX_PROFILE_IMAGE_BYTES = 10L * 1024L * 1024L

private val SUPPORTED_IMAGE_TYPES = mapOf(
    "image/jpeg" to ".jpg",
    "image/png" to ".png",
    "image/webp" to ".webp"
)
