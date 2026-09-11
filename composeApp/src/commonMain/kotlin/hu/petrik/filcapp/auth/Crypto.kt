package hu.petrik.filcapp.auth

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** SHA-256 digest of [bytes]. */
internal expect fun sha256(bytes: ByteArray): ByteArray

/** [count] cryptographically secure random bytes. */
internal expect fun randomBytes(count: Int): ByteArray

/** Base64url without padding, per RFC 7636 / RFC 4648 section 5. */
@OptIn(ExperimentalEncodingApi::class)
internal fun ByteArray.toBase64Url(): String = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(this)

/** PKCE code verifier: 32 random bytes (43 base64url characters). */
internal fun newCodeVerifier(): String = randomBytes(32).toBase64Url()

/** PKCE code challenge for the S256 method. */
internal fun codeChallengeS256(verifier: String): String = sha256(verifier.encodeToByteArray()).toBase64Url()
