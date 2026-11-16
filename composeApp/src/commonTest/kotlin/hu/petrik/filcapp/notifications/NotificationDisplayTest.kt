package hu.petrik.filcapp.notifications

import hu.petrik.filcapp.settings.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationDisplayTest {
    private val teacherId = "04e6606c-618e-4cfa-a8e8-25d4d27d7183"

    private fun entry(
        type: String,
        title: String,
        content: String,
    ) = NotificationEntry("1", title, content, type, "2026-11-16T08:57:00", false)

    @Test
    fun resolvesTeacherUuidAndFormatsHungarianDate() {
        val item =
            entry(
                "substitution",
                "Új helyettesítés",
                "$teacherId helyettesít Szerda, 2026. 11. 18., 1 órádat tartja meg.",
            )
        val rendered = displayNotification(item, mapOf(teacherId to "Kovács Péter"), AppLanguage.HU)
        assertTrue(notificationNeedsTeacherNames(item))
        assertEquals("Új helyettesítés", rendered.title)
        assertEquals(
            "Kovács Péter helyettesít szerdán (2026. 11. 18.), 1 órádat tartja meg.",
            rendered.content,
        )
    }

    @Test
    fun neverShowsUnknownTeacherUuid() {
        val item = entry("substitution", "Új helyettesítés", "$teacherId helyettesít holnap.")
        assertEquals(
            "Helyettesítő tanár helyettesít holnap.",
            displayNotification(item, emptyMap(), AppLanguage.HU).content,
        )
        assertEquals(
            "Substitute teacher helyettesít holnap.",
            displayNotification(item, emptyMap(), AppLanguage.EN).content,
        )
    }

    @Test
    fun translatesEnglishAnnouncementHeaderAndRemovesDuplicatePrefix() {
        val item =
            entry(
                "announcement",
                "New Announcement",
                "New announcement: Kémia OKTV 14:00 Petrik Lajos Előadó",
            )
        assertEquals(
            NotificationDisplay("Új közlemény", "Kémia OKTV 14:00 Petrik Lajos Előadó"),
            displayNotification(item, emptyMap(), AppLanguage.HU),
        )
        assertEquals(
            NotificationDisplay("New announcement", "Kémia OKTV 14:00 Petrik Lajos Előadó"),
            displayNotification(item, emptyMap(), AppLanguage.EN),
        )
        assertFalse(notificationNeedsTeacherNames(item))
    }

    @Test
    fun leavesUnrelatedNotificationContentUntouched() {
        val item = entry("test", "Custom title", "Reference: $teacherId")
        assertEquals(
            NotificationDisplay("Custom title", "Reference: $teacherId"),
            displayNotification(item, emptyMap(), AppLanguage.HU),
        )
    }
}
