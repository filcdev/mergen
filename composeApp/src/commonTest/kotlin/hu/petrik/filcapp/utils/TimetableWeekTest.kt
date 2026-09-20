package hu.petrik.filcapp.utils

import hu.petrik.filcapp.network.WeekDefinitionDto
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimetableWeekTest {
    @Test
    fun mondayOfSundayReturnsPreviousMonday() {
        assertEquals(LocalDate(2026, 9, 14), mondayOf(LocalDate(2026, 9, 20)))
    }

    @Test
    fun weekDatesReturnsMondayToSundayInOrder() {
        assertEquals(
            listOf(
                LocalDate(2026, 9, 14),
                LocalDate(2026, 9, 15),
                LocalDate(2026, 9, 16),
                LocalDate(2026, 9, 17),
                LocalDate(2026, 9, 18),
                LocalDate(2026, 9, 19),
                LocalDate(2026, 9, 20),
            ),
            weekDates(LocalDate(2026, 9, 14)),
        )
    }

    @Test
    fun weekTypeForAlternatesAroundAnchor() {
        val anchor = LocalDate(2024, 1, 1)
        assertEquals(WeekType.A, weekTypeFor(LocalDate(2024, 1, 1), anchor))
        assertEquals(WeekType.B, weekTypeFor(LocalDate(2024, 1, 8), anchor))
        assertEquals(WeekType.A, weekTypeFor(LocalDate(2024, 1, 15), anchor))
        assertEquals(WeekType.B, weekTypeFor(LocalDate(2023, 12, 25), anchor))
    }

    @Test
    fun lessonWeekTypeFromShort() {
        assertEquals(WeekType.A, lessonWeekType(WeekDefinitionDto(short = "A")))
        assertEquals(WeekType.B, lessonWeekType(WeekDefinitionDto(short = "b")))
    }

    @Test
    fun lessonWeekTypeFromName() {
        assertEquals(WeekType.A, lessonWeekType(WeekDefinitionDto(name = "A hét")))
        assertEquals(WeekType.B, lessonWeekType(WeekDefinitionDto(name = "B hét")))
    }

    @Test
    fun lessonWeekTypeFromWeeks() {
        assertEquals(WeekType.A, lessonWeekType(WeekDefinitionDto(weeks = listOf("10"))))
        assertEquals(WeekType.B, lessonWeekType(WeekDefinitionDto(weeks = listOf("01"))))
    }

    @Test
    fun lessonWeekTypeNullAndUnknown() {
        assertNull(lessonWeekType(null))
        assertNull(lessonWeekType(WeekDefinitionDto(name = "Ismeretlen")))
    }

    @Test
    fun dayIndexOfCoversHungarianAndEnglish() {
        assertEquals(0, dayIndexOf("Hétfő", null))
        assertEquals(0, dayIndexOf("hétfő", null))
        assertEquals(0, dayIndexOf("Monday", null))
        assertEquals(4, dayIndexOf(null, "P"))
        assertEquals(4, dayIndexOf("Péntek", null))
        assertEquals(5, dayIndexOf("Saturday", null))
        assertEquals(6, dayIndexOf("Sunday", null))
        assertEquals(5, dayIndexOf("Szombat", null))
        assertEquals(6, dayIndexOf("Vasárnap", null))
        assertNull(dayIndexOf("Holiday", null))
    }

    @Test
    fun dayIndexOfCoversAccentedThursday() {
        assertEquals(3, dayIndexOf("Csütörtök", null))
        assertEquals(3, dayIndexOf("csutortok", null))
    }

    @Test
    fun lessonWeekTypeFromEnglishName() {
        assertEquals(WeekType.A, lessonWeekType(WeekDefinitionDto(name = "Week A")))
        assertEquals(WeekType.B, lessonWeekType(WeekDefinitionDto(name = "Week B")))
    }

    @Test
    fun lessonWeekTypeBareNameWithEmptyShort() {
        assertEquals(WeekType.A, lessonWeekType(WeekDefinitionDto(short = "", name = "A")))
        assertEquals(WeekType.B, lessonWeekType(WeekDefinitionDto(short = "", name = "B")))
    }

    @Test
    fun lessonWeekTypeUnknownWeeksShowsAlways() {
        assertNull(lessonWeekType(WeekDefinitionDto(weeks = listOf("11"))))
    }

    @Test
    fun mondayOfIsIdentityOnMonday() {
        val monday = LocalDate(2026, 9, 14)
        assertEquals(monday, mondayOf(monday))
    }

    @Test
    fun mondayOfSaturdayReturnsMonday() {
        assertEquals(LocalDate(2026, 9, 14), mondayOf(LocalDate(2026, 9, 19)))
    }

    @Test
    fun weekTypeForTwoWeeksBeforeAnchorIsA() {
        val anchor = LocalDate(2024, 1, 1)
        assertEquals(WeekType.A, weekTypeFor(LocalDate(2023, 12, 18), anchor))
    }
}
