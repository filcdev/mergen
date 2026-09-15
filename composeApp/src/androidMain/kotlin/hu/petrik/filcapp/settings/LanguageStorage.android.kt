package hu.petrik.filcapp.settings

import android.content.Context

private const val SETTINGS_NAME = "mergen_settings"
private const val LANGUAGE_KEY = "language"
private const val THEME_KEY = "theme_mode"

private var applicationContext: Context? = null

fun initializeAndroidLanguageStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun loadLanguagePreference(): String? =
    applicationContext
        ?.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
        ?.getString(LANGUAGE_KEY, null)

actual fun saveLanguagePreference(value: String) {
    applicationContext
        ?.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(LANGUAGE_KEY, value)
        ?.apply()
}

actual fun loadThemePreference(): String? =
    applicationContext
        ?.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
        ?.getString(THEME_KEY, null)

actual fun saveThemePreference(value: String) {
    applicationContext
        ?.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(THEME_KEY, value)
        ?.apply()
}
