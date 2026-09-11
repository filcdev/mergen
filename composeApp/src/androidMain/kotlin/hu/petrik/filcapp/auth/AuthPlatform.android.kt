package hu.petrik.filcapp.auth

import android.content.Context
import android.content.Intent
import android.net.Uri

private var applicationContext: Context? = null

fun initializeAndroidAuth(context: Context) {
    applicationContext = context.applicationContext
}

actual fun openExternalUrl(url: String) {
    val context = applicationContext ?: return
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}

actual fun loadStoredAuthCookie(): String? =
    applicationContext
        ?.getSharedPreferences("mergen_auth", Context.MODE_PRIVATE)
        ?.getString("session_cookie", null)

actual fun saveStoredAuthCookie(cookie: String?) {
    applicationContext
        ?.getSharedPreferences("mergen_auth", Context.MODE_PRIVATE)
        ?.edit()
        ?.apply {
            if (cookie == null) remove("session_cookie") else putString("session_cookie", cookie)
        }
        ?.apply()
}
