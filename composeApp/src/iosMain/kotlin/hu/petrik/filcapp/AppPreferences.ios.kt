package hu.petrik.filcapp

import platform.Foundation.NSUserDefaults

actual object AppPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults
    private const val KEY = "accent_hue"

    actual fun getAccentHue(): Float =
        if (defaults.objectForKey(KEY) != null) defaults.floatForKey(KEY) else 220f

    actual fun setAccentHue(hue: Float) { defaults.setFloat(hue, forKey = KEY) }
}
