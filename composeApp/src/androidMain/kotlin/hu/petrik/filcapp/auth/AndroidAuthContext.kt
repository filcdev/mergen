package hu.petrik.filcapp.auth

import android.content.Context
import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

/**
 * Platform handles the auth layer needs on Android: an application context for secure storage and
 * the current activity for launching the Custom Tab. Registered by `MainActivity`.
 */
internal object AndroidAuthContext {
    private var applicationContext: Context? = null
    private var activityReference: WeakReference<ComponentActivity>? = null

    fun register(activity: ComponentActivity) {
        applicationContext = activity.applicationContext
        activityReference = WeakReference(activity)
    }

    fun unregister(activity: ComponentActivity) {
        if (activityReference?.get() === activity) {
            activityReference = null
        }
    }

    fun requireContext(): Context = applicationContext ?: error("AndroidAuthContext.register() must run before auth is used.")

    fun currentActivity(): ComponentActivity? = activityReference?.get()
}
