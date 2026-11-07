package hu.petrik.filcapp.notifications

/** Called before Chronos clears the authenticated session. */
internal expect suspend fun unregisterPushOnSignOut()
