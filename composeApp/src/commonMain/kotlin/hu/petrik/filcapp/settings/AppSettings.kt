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

const val DEFAULT_PRIMARY_COLOR_ARGB: Long = 0xFF009869L

expect fun loadLanguagePreference(): String?

expect fun saveLanguagePreference(value: String)

expect fun loadThemePreference(): String?

expect fun saveThemePreference(value: String)

expect fun loadPrimaryColorPreference(): String?

expect fun savePrimaryColorPreference(value: String)

object AppSettings {
    val language = mutableStateOf(AppLanguage.HU)
    val themeMode = mutableStateOf(AppThemeMode.SYSTEM)
    val primaryColorArgb = mutableStateOf(DEFAULT_PRIMARY_COLOR_ARGB)

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

        primaryColorArgb.value =
            loadPrimaryColorPreference()
                ?.toLongOrNull()
                ?.takeIf { it in 0L..0xFFFFFFFFL }
                ?: DEFAULT_PRIMARY_COLOR_ARGB
    }

    fun setLanguage(value: AppLanguage) {
        language.value = value
        saveLanguagePreference(value.name)
    }

    fun setThemeMode(value: AppThemeMode) {
        themeMode.value = value
        saveThemePreference(value.name)
    }

    fun setPrimaryColor(
        value: Long,
        persist: Boolean = true,
    ) {
        primaryColorArgb.value = value and 0xFFFFFFFFL

        if (persist) {
            persistPrimaryColor()
        }
    }

    fun persistPrimaryColor() {
        savePrimaryColorPreference(primaryColorArgb.value.toString())
    }

    fun resetPrimaryColor() {
        setPrimaryColor(DEFAULT_PRIMARY_COLOR_ARGB)
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
