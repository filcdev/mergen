package hu.petrik.filcapp.settings

import platform.Foundation.NSUserDefaults

actual fun loadLanguagePreference(): String? =
    NSUserDefaults.standardUserDefaults.stringForKey("mergen_language")

actual fun saveLanguagePreference(value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, forKey = "mergen_language")
}
