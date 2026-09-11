package hu.petrik.filcapp.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

@OptIn(ExperimentalForeignApi::class)
internal actual fun sha256(bytes: ByteArray): ByteArray {
    require(bytes.isNotEmpty()) { "sha256 needs a non-empty input" }
    val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
    bytes.usePinned { input ->
        digest.usePinned { output ->
            CC_SHA256(input.addressOf(0), bytes.size.convert(), output.addressOf(0).reinterpret())
        }
    }
    return digest
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun randomBytes(count: Int): ByteArray {
    val bytes = ByteArray(count)
    bytes.usePinned { pinned ->
        val status = SecRandomCopyBytes(kSecRandomDefault, count.convert(), pinned.addressOf(0))
        if (status != 0) {
            throw AuthException("Secure random number generation failed (OSStatus $status).")
        }
    }
    return bytes
}
