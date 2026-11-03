package hu.petrik.filcapp.widgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class WidgetDestination {
    TIMETABLE,
    SUBSTITUTIONS,
}

enum class WidgetSelectionType {
    TEACHER,
    COHORT,
}

data class WidgetOpenRequest(
    val serial: Int,
    val destination: WidgetDestination,
    val scope: WidgetSelectionType,
    val selectionId: String,
)

/** Carries a widget tap across activity creation, authentication and tab navigation. */
object WidgetNavigation {
    const val ACTION_OPEN = "hu.petrik.filcapp.action.OPEN_WIDGET"
    const val EXTRA_MODE = "hu.petrik.filcapp.widget.MODE"
    const val EXTRA_SCOPE = "hu.petrik.filcapp.widget.SCOPE"
    const val EXTRA_SELECTION_ID = "hu.petrik.filcapp.widget.SELECTION_ID"

    private var serial = 0

    var request by mutableStateOf<WidgetOpenRequest?>(null)
        private set

    fun open(
        mode: String?,
        scope: String?,
        selectionId: String?,
    ) {
        val destination =
            when (mode) {
                "CURRENT_LESSON" -> WidgetDestination.TIMETABLE
                "SUBSTITUTIONS" -> WidgetDestination.SUBSTITUTIONS
                else -> return
            }
        val selectionType =
            when (scope) {
                "TEACHER" -> WidgetSelectionType.TEACHER
                "COHORT" -> WidgetSelectionType.COHORT
                else -> return
            }
        val selectedId = selectionId?.takeIf { it.isNotBlank() } ?: return
        serial++
        request = WidgetOpenRequest(serial, destination, selectionType, selectedId)
    }

    fun consume(requestSerial: Int) {
        if (request?.serial == requestSerial) {
            request = null
        }
    }
}
