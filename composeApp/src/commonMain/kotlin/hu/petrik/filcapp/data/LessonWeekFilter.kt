package hu.petrik.filcapp.data

import kotlinx.datetime.LocalDate

internal enum class LessonWeekType { A, B, ALL }

internal fun lessonWeekType(definition: WeekDefinitionDto?): LessonWeekType {
    if (definition == null) return LessonWeekType.ALL
    when (definition.short.trim().uppercase()) {
        "A" -> return LessonWeekType.A
        "B" -> return LessonWeekType.B
    }
    val name = definition.name.trim().lowercase().replace('é', 'e')
    when (name) {
        "het a", "a het", "week a" -> return LessonWeekType.A
        "het b", "b het", "week b" -> return LessonWeekType.B
    }
    return when (definition.weeks.map { it.trim() }.toSet()) {
        setOf("10") -> LessonWeekType.A
        setOf("01") -> LessonWeekType.B
        else -> LessonWeekType.ALL
    }
}

internal fun filterLessonsForSchoolWeek(
    lessons: List<LessonDto>,
    week: LessonWeekType,
): List<LessonDto> =
    lessons.filter { item ->
        val lessonType = lessonWeekType(item.weekDefinition)
        week == LessonWeekType.ALL || lessonType == LessonWeekType.ALL || lessonType == week
    }

/** The first school week containing September 1 is A by convention.
 *  The timetable header allows manually inverting this convention when the
 *  school's actual A/B ordering is different. */
internal fun schoolWeekTypeFor(
    monday: LocalDate,
    reversed: Boolean = false,
): LessonWeekType {
    val friday = LocalDate.fromEpochDays(monday.toEpochDays() + 4)
    val academicYear = if (friday.monthNumber >= 9) friday.year else friday.year - 1
    val septemberFirst = LocalDate(academicYear, 9, 1)
    val daysSinceMonday =
        when (septemberFirst.dayOfWeek) {
            kotlinx.datetime.DayOfWeek.MONDAY -> 0
            kotlinx.datetime.DayOfWeek.TUESDAY -> 1
            kotlinx.datetime.DayOfWeek.WEDNESDAY -> 2
            kotlinx.datetime.DayOfWeek.THURSDAY -> 3
            kotlinx.datetime.DayOfWeek.FRIDAY -> 4
            kotlinx.datetime.DayOfWeek.SATURDAY -> 5
            kotlinx.datetime.DayOfWeek.SUNDAY -> 6
        }
    val startMonday = LocalDate.fromEpochDays(septemberFirst.toEpochDays() - daysSinceMonday)
    val weekIndex = (monday.toEpochDays() - startMonday.toEpochDays()) / 7
    val parity = weekIndex + (if (reversed) 1 else 0)
    return if (parity % 2L == 0L) LessonWeekType.A else LessonWeekType.B
}
