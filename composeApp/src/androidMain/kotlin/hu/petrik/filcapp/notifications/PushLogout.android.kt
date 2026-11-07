package hu.petrik.filcapp.notifications

import hu.petrik.filcapp.auth.AndroidAuthContext

internal actual suspend fun unregisterPushOnSignOut() {
    MobilePushRegistration.unregisterOnSignOut(AndroidAuthContext.requireContext())
}
