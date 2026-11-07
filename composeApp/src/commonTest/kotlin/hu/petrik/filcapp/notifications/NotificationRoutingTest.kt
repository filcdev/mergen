package hu.petrik.filcapp.notifications

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationRoutingTest {
    @Test
    fun substitutionNotificationsOpenSubstitutions() {
        assertTrue(opensSubstitutions("substitution"))
        assertTrue(opensSubstitutions("substitution_teacher"))
        assertTrue(opensSubstitutions("moved_lesson"))
    }

    @Test
    fun unrelatedNotificationsOpenInbox() {
        assertFalse(opensSubstitutions("announcement"))
        assertFalse(opensSubstitutions("system_message"))
        assertFalse(opensSubstitutions("test"))
        assertFalse(opensSubstitutions("notification")) // Chronos FCM opens the inbox.
        assertFalse(opensSubstitutions(""))
    }
}
