package hu.petrik.filcapp.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class LessonWeekFilterTest {
    private val both = WeekDefinitionDto(id = "all", short = "Mind", name = "Mind hét", weeks = listOf("11"))
    private val a = WeekDefinitionDto(id = "a", short = "A", weeks = listOf("10"))
    private val b = WeekDefinitionDto(id = "b", short = "B", weeks = listOf("01"))
    private val entries = listOf(
        LessonDto(id = "a", weekDefinition = a),
        LessonDto(id = "b", weekDefinition = b),
        LessonDto(id = "both", weekDefinition = both),
        LessonDto(id = "null", weekDefinition = null),
    )

    @Test
    fun aWeekIncludesBothWeekLessons() {
        assertEquals(listOf("a", "both", "null"), filterLessonsForSchoolWeek(entries, LessonWeekType.A).map { it.id })
    }

    @Test
    fun bWeekIncludesBothWeekLessons() {
        assertEquals(listOf("b", "both", "null"), filterLessonsForSchoolWeek(entries, LessonWeekType.B).map { it.id })
    }

    @Test
    fun fallbackClassificationMatchesFilcWebClient() {
        assertEquals(LessonWeekType.A, lessonWeekType(WeekDefinitionDto(weeks = listOf("10"))))
        assertEquals(LessonWeekType.B, lessonWeekType(WeekDefinitionDto(weeks = listOf("01"))))
        assertEquals(LessonWeekType.A, lessonWeekType(WeekDefinitionDto(name = "A hét")))
        assertEquals(LessonWeekType.B, lessonWeekType(WeekDefinitionDto(name = "week b")))
        assertEquals(LessonWeekType.ALL, lessonWeekType(WeekDefinitionDto(weeks = listOf("11"))))
        assertEquals(LessonWeekType.ALL, lessonWeekType(null))
    }

    @Test
    fun alternatingWeeksAndYearBoundaries() {
        assertEquals(LessonWeekType.A, schoolWeekTypeFor(LocalDate(2026, 8, 31)))
        assertEquals(LessonWeekType.B, schoolWeekTypeFor(LocalDate(2026, 9, 7)))
        assertEquals(LessonWeekType.A, schoolWeekTypeFor(LocalDate(2026, 9, 7), reversed = true))
        assertEquals(LessonWeekType.B, schoolWeekTypeFor(LocalDate(2026, 12, 28)))
        assertEquals(LessonWeekType.A, schoolWeekTypeFor(LocalDate(2027, 1, 4)))
    }
}
