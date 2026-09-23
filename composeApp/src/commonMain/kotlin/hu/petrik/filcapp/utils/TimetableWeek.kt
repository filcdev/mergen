package hu.petrik.filcapp.utils

import hu.petrik.filcapp.network.WeekDefinitionDto
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

enum class WeekType {
    A,
    B,
}

/** Monday of the week that contains [date] (ISO week, Monday = day 1). */
fun mondayOf(date: LocalDate): LocalDate = date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - 1))

/** All seven days Mon..Sun of the week starting on [monday]. */
fun weekDates(monday: LocalDate): List<LocalDate> = (0..6).map { offset -> monday.plus(DatePeriod(days = offset)) }

/**
 * A/B week type derived from week parity against [anchorMonday].
 * Even week difference => A, odd => B. Floor division keeps weeks before the anchor alternating too.
 */
fun weekTypeFor(
    monday: LocalDate,
    anchorMonday: LocalDate,
): WeekType {
    val diffWeeks = anchorMonday.daysUntil(monday).floorDiv(7)
    return if (diffWeeks % 2 == 0) WeekType.A else WeekType.B
}

/** Floor division for a positive divisor (Math.floorDiv is JVM-only). */
private fun Int.floorDiv(divisor: Int): Int {
    val quotient = this / divisor
    return if (this >= 0 || this % divisor == 0) quotient else quotient - 1
}

/**
 * Infers a lesson's week type from its week definition, or null when it applies to every week
 * (or the definition cannot be classified). Mirrors the web app's `getDayOrder`/week logic.
 */
fun lessonWeekType(week: WeekDefinitionDto?): WeekType? {
    if (week == null) return null

    when (week.short.trim().uppercase()) {
        "A" -> return WeekType.A
        "B" -> return WeekType.B
    }

    when (normalizeDayText(week.name.trim())) {
        "a", "het a", "a het", "week a" -> return WeekType.A
        "b", "het b", "b het", "week b" -> return WeekType.B
    }

    val weeks = week.weeks.map { it.trim() }.toSet()
    return when {
        weeks.size == 1 && "10" in weeks -> WeekType.A
        weeks.size == 1 && "01" in weeks -> WeekType.B
        else -> null
    }
}

/** Monday = 0 … Sunday = 6, or null when blank/unknown. Tries [dayShort] before [dayName]. */
fun dayIndexOf(
    dayName: String?,
    dayShort: String?,
): Int? {
    listOfNotNull(dayShort, dayName).forEach { raw ->
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return@forEach
        dayIndexLookup[normalizeDayText(trimmed)]?.let { return it }
    }
    return null
}

private val dayIndexLookup =
    mapOf(
        // Monday
        "mon" to 0,
        "monday" to 0,
        "hetfo" to 0,
        "he" to 0,
        "h" to 0,
        // Tuesday
        "tue" to 1,
        "tuesday" to 1,
        "kedd" to 1,
        "ke" to 1,
        "k" to 1,
        // Wednesday
        "wed" to 2,
        "wednesday" to 2,
        "szerda" to 2,
        "sze" to 2,
        "sz" to 2,
        // Thursday
        "thu" to 3,
        "thursday" to 3,
        "csutortok" to 3,
        "cs" to 3,
        "c" to 3,
        // Friday
        "fri" to 4,
        "friday" to 4,
        "pentek" to 4,
        "pe" to 4,
        "p" to 4,
        // Saturday
        "sat" to 5,
        "saturday" to 5,
        "szombat" to 5,
        "szo" to 5,
        // Sunday
        "sun" to 6,
        "sunday" to 6,
        "vasarnap" to 6,
        "va" to 6,
        "v" to 6,
    )

private val accentFold =
    mapOf(
        'á' to 'a',
        'é' to 'e',
        'í' to 'i',
        'ó' to 'o',
        'ö' to 'o',
        'ő' to 'o',
        'ú' to 'u',
        'ü' to 'u',
        'ű' to 'u',
    )

private fun normalizeDayText(value: String): String {
    val lowered = value.lowercase()
    return buildString(lowered.length) {
        lowered.forEach { char -> append(accentFold[char] ?: char) }
    }
}
