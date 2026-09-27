package hu.petrik.filcapp.settings

import platform.Foundation.NSUserDefaults

private const val LANGUAGE_KEY = "mergen_language"
private const val THEME_KEY = "mergen_theme_mode"
private const val PRIMARY_COLOR_KEY = "mergen_primary_color"

actual fun loadLanguagePreference(): String? =
    NSUserDefaults.standardUserDefaults.stringForKey(LANGUAGE_KEY)

actual fun saveLanguagePreference(value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, forKey = LANGUAGE_KEY)
}

actual fun loadThemePreference(): String? =
    NSUserDefaults.standardUserDefaults.stringForKey(THEME_KEY)

actual fun saveThemePreference(value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, forKey = THEME_KEY)
}

actual fun loadPrimaryColorPreference(): String? =
    NSUserDefaults.standardUserDefaults.stringForKey(PRIMARY_COLOR_KEY)

actual fun savePrimaryColorPreference(value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, forKey = PRIMARY_COLOR_KEY)
}
