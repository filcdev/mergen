package hu.petrik.filcapp

import java.util.prefs.Preferences

actual object AppPreferences {
    private val prefs = Preferences.userRoot().node("hu/petrik/filcapp")

    actual fun getAccentHue(): Float = prefs.getFloat("accent_hue", 220f)
    actual fun setAccentHue(hue: Float) { prefs.putFloat("accent_hue", hue) }
    actual fun getThemeMode(): Int = prefs.getInt("theme_mode", 0)
    actual fun setThemeMode(mode: Int) { prefs.putInt("theme_mode", mode) }
}
