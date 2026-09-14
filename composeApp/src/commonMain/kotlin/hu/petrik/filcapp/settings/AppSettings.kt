package hu.petrik.filcapp.settings

import androidx.compose.runtime.mutableStateOf

enum class AppLanguage {
    HU,
    EN,
}

expect fun loadLanguagePreference(): String?

expect fun saveLanguagePreference(value: String)

object AppSettings {
    val language = mutableStateOf(AppLanguage.HU)

    fun initialize() {
        language.value =
            when (loadLanguagePreference()) {
                AppLanguage.EN.name -> AppLanguage.EN
                else -> AppLanguage.HU
            }
    }

    fun setLanguage(value: AppLanguage) {
        language.value = value
        saveLanguagePreference(value.name)
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
