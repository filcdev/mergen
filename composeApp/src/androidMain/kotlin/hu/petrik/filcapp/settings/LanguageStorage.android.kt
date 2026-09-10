package hu.petrik.filcapp.settings

import android.content.Context

private var applicationContext: Context? = null

fun initializeAndroidLanguageStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun loadLanguagePreference(): String? =
    applicationContext
        ?.getSharedPreferences("mergen_settings", Context.MODE_PRIVATE)
        ?.getString("language", null)

actual fun saveLanguagePreference(value: String) {
    applicationContext
        ?.getSharedPreferences("mergen_settings", Context.MODE_PRIVATE)
        ?.edit()
        ?.putString("language", value)
        ?.apply()
}
