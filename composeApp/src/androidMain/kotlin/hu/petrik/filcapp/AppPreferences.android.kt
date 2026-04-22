package hu.petrik.filcapp

import hu.petrik.filcapp.auth.SessionStore

actual object AppPreferences {
    actual fun getAccentHue(): Float = SessionStore.prefs.getFloat("accent_hue", 220f)
    actual fun setAccentHue(hue: Float) { SessionStore.prefs.edit().putFloat("accent_hue", hue).apply() }
    actual fun getThemeMode(): Int = SessionStore.prefs.getInt("theme_mode", 0)
    actual fun setThemeMode(mode: Int) { SessionStore.prefs.edit().putInt("theme_mode", mode).apply() }
}
