package hu.petrik.filcapp.auth

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.create
import platform.darwin.UInt8

@OptIn(ExperimentalForeignApi::class)
actual fun base64ToImageBitmap(base64: String): ImageBitmap? {
    val data = base64.substringAfter(",", base64)
    return try {
        val nsData = NSData.create(base64Encoding = data) ?: return null
        val length = nsData.length.toInt()
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), nsData.bytes, length.toULong())
        }
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (_: Exception) {
        null
    }
}
