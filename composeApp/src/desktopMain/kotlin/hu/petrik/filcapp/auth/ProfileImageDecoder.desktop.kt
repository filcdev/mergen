package hu.petrik.filcapp.auth

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.util.Base64

actual fun base64ToImageBitmap(base64: String): ImageBitmap? {
    val data = base64.substringAfter(",", base64)
    return try {
        val bytes = Base64.getDecoder().decode(data)
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (_: Exception) {
        null
    }
}
