package hu.petrik.filcapp.settings

import androidx.compose.runtime.mutableStateOf

enum class AppLanguage {
    HU,
    EN,
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

expect fun loadLanguagePreference(): String?

expect fun saveLanguagePreference(value: String)

expect fun loadThemePreference(): String?

expect fun saveThemePreference(value: String)

object AppSettings {
    val language = mutableStateOf(AppLanguage.HU)
    val themeMode = mutableStateOf(AppThemeMode.SYSTEM)

    fun initialize() {
        language.value =
            when (loadLanguagePreference()) {
                AppLanguage.EN.name -> AppLanguage.EN
                else -> AppLanguage.HU
            }

        themeMode.value =
            when (loadThemePreference()) {
                AppThemeMode.LIGHT.name -> AppThemeMode.LIGHT
                AppThemeMode.DARK.name -> AppThemeMode.DARK
                else -> AppThemeMode.SYSTEM
            }
    }

    fun setLanguage(value: AppLanguage) {
        language.value = value
        saveLanguagePreference(value.name)
    }

    fun setThemeMode(value: AppThemeMode) {
        themeMode.value = value
        saveThemePreference(value.name)
    }
}

fun tr(
    hu: String,
    en: String,
): String =
    when (AppSettings.language.value) {
        AppLanguage.HU -> hu
        AppLanguage.EN -> en
    }
