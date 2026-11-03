package hu.petrik.filcapp.widgets

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal enum class WidgetMode {
    CURRENT_LESSON,
    SUBSTITUTIONS,
}

internal enum class WidgetScope {
    TEACHER,
    COHORT,
}

internal data class WidgetConfig(
    val mode: WidgetMode,
    val scope: WidgetScope,
    val selectionId: String,
    val selectionName: String,
)

internal object WidgetStorage {
    private const val PREFERENCES = "mergen_home_widgets"
    private val json = Json { ignoreUnknownKeys = true }

    fun readConfig(
        context: Context,
        id: Int,
    ): WidgetConfig? {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val mode = preferences.getString("$id.mode", null) ?: return null
        val scope = preferences.getString("$id.scope", null) ?: return null
        val selectionId = preferences.getString("$id.selectionId", null) ?: return null
        val name = preferences.getString("$id.selectionName", "").orEmpty()
        return WidgetConfig(
            mode = runCatching { WidgetMode.valueOf(mode) }.getOrNull() ?: return null,
            scope = runCatching { WidgetScope.valueOf(scope) }.getOrNull() ?: return null,
            selectionId = selectionId,
            selectionName = name,
        )
    }

    fun saveConfig(
        context: Context,
        id: Int,
        config: WidgetConfig,
    ) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putString("$id.mode", config.mode.name)
            .putString("$id.scope", config.scope.name)
            .putString("$id.selectionId", config.selectionId)
            .putString("$id.selectionName", config.selectionName)
            .remove("$id.snapshot")
            .apply()
    }

    fun readSnapshot(
        context: Context,
        id: Int,
    ): WidgetSnapshot? {
        val stored =
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getString("$id.snapshot", null) ?: return null
        return runCatching { json.decodeFromString<WidgetSnapshot>(stored) }.getOrNull()
    }

    fun saveSnapshot(
        context: Context,
        id: Int,
        snapshot: WidgetSnapshot,
    ) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putString("$id.snapshot", json.encodeToString(snapshot))
            .apply()
    }

    fun remove(
        context: Context,
        id: Int,
    ) {
        val prefix = "$id."
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        preferences.all.keys.filter { it.startsWith(prefix) }.forEach(editor::remove)
        editor.apply()
    }
}
