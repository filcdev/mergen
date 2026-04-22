package hu.petrik.filcapp

import platform.Foundation.NSUserDefaults

actual object AppPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults
    private const val KEY = "accent_hue"

    actual fun getAccentHue(): Float =
        if (defaults.objectForKey(KEY) != null) defaults.floatForKey(KEY) else 220f

    actual fun setAccentHue(hue: Float) { defaults.setFloat(hue, forKey = KEY) }

    actual fun getThemeMode(): Int =
        if (defaults.objectForKey("theme_mode") != null) defaults.integerForKey("theme_mode").toInt() else 0

    actual fun setThemeMode(mode: Int) { defaults.setInteger(mode.toLong(), forKey = "theme_mode") }
}
