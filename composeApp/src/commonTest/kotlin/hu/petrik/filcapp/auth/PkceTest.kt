package hu.petrik.filcapp.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PkceTest {
    /** RFC 7636 appendix B: the canonical S256 derivation vector. */
    @Test
    fun codeChallengeMatchesRfc7636Vector() {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", codeChallengeS256(verifier))
    }

    @Test
    fun codeVerifierIsUrlSafeAndUnpadded() {
        repeat(20) {
            val verifier = newCodeVerifier()
            assertEquals(43, verifier.length)
            assertTrue(verifier.none { character -> character in "+/=" })
        }
    }

    @Test
    fun redirectCarriesCodeAndState() {
        val redirect = parseRedirect("filcapp://auth?code=abc123&state=xyz")
        assertEquals("abc123", redirect.code)
        assertEquals("xyz", redirect.state)
        assertTrue(redirect.isSuccess)
    }

    @Test
    fun redirectSurfacesEntraError() {
        val redirect = parseRedirect("filcapp://auth?error=access_denied&error_description=User%20cancelled")
        assertEquals("User cancelled", redirect.error)
        assertNull(redirect.code)
    }
}
