package hu.petrik.filcapp.admin

import kotlinx.serialization.json.JsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NewsAdminRepositoryTest {
    @Test
    fun createsTextBlockCompatibleWithChronos() {
        val block = plainNewsBlock("Hello")
        assertTrue(block is JsonArray)
        assertEquals("Hello", plainNewsText(block))
    }

    @Test
    fun followsBudapestDateBoundaries() {
        assertEquals("2026-11-02T23:00:00Z", newsDateBoundary("2026-11-03", false))
        assertEquals("2026-11-03", newsLocalDay("2026-11-02T23:00:00Z"))
        assertTrue(newsDateBoundary("2026-11-03", true) > newsDateBoundary("2026-11-03", false))
    }

    @Test
    fun detectsSupportedImageSignatures() {
        assertEquals("image/jpeg", supportedNewsImageMime(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte())))
        assertEquals("image/webp", supportedNewsImageMime("RIFFxxxxWEBP".encodeToByteArray()))
        assertNull(supportedNewsImageMime("not-image".encodeToByteArray()))
    }
}
