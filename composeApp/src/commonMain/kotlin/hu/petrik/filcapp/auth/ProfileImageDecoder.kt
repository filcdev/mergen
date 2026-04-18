package hu.petrik.filcapp.auth

import androidx.compose.ui.graphics.ImageBitmap

expect fun base64ToImageBitmap(base64: String): ImageBitmap?
