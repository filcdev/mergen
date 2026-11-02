package hu.petrik.filcapp.data

/** The visible class label, never a data class's generated toString(). */
val CohortDto.displayLabel: String
    get() = short.trim().ifBlank { name.trim() }

/** Used by timetable, substitutions and moved lessons alike. */
fun List<CohortDto>.cohortLabels(): String =
    map { it.displayLabel }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(", ")

/**
 * API references may use IDs from different timetable snapshots.
 * Prefer matching IDs, but also accept matching class name or short label.
 */
fun CohortDto.matchesCohortSelection(selected: CohortDto): Boolean {
    if (id.isNotBlank() && selected.id.isNotBlank() && id == selected.id) return true
    val labels = listOf(name, short).map(String::trim).filter(String::isNotBlank)
    val selectedLabels = listOf(selected.name, selected.short).map(String::trim).filter(String::isNotBlank)
    return labels.any { label -> selectedLabels.any { it.equals(label, ignoreCase = true) } }
}
