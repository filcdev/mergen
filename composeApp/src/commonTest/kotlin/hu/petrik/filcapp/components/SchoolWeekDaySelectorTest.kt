package hu.petrik.filcapp.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

class SchoolWeekDaySelectorTest {
    private val thursday = LocalDate(2026, 11, 19)

    @Test
    fun selectedDayResolvesToExactCalendarDate() {
        assertEquals(LocalDate(2026, 11, 16), schoolDateFor(thursday, 0, 1))
        assertEquals(thursday, schoolDateFor(thursday, 0, 4))
        assertEquals(LocalDate(2026, 11, 20), schoolDateFor(thursday, 0, 5))
    }

    @Test
    fun weekNavigationAdjustsTheSelectedDate() {
        assertEquals(LocalDate(2026, 11, 9), schoolDateFor(thursday, -1, 1))
        assertEquals(LocalDate(2026, 11, 27), schoolDateFor(thursday, 1, 5))
    }

    @Test
    fun shortMonthsAndLeapYearsHaveValidDates() {
        assertEquals(LocalDate(2027, 2, 22), schoolDateFor(LocalDate(2027, 2, 28), 0, 1))
        assertEquals(LocalDate(2028, 2, 28), schoolDateFor(LocalDate(2028, 2, 29), 0, 1))
        assertEquals(LocalDate(2028, 3, 3), schoolDateFor(LocalDate(2028, 2, 29), 0, 5))
    }

    @Test
    fun weekCalculationCrossesYearsCorrectly() {
        assertEquals(LocalDate(2026, 12, 28), schoolWeekMonday(LocalDate(2027, 1, 1), 0))
        assertEquals(LocalDate(2027, 1, 1), schoolDateFor(LocalDate(2027, 1, 1), 0, 5))
    }

    @Test
    fun weekendsDefaultToMondayAsInTimetable() {
        assertEquals(1, schoolDefaultDay(DayOfWeek.SATURDAY))
        assertEquals(1, schoolDefaultDay(DayOfWeek.SUNDAY))
        assertEquals(4, schoolDefaultDay(DayOfWeek.THURSDAY))
    }

    @Test
    fun onlySchoolDaysCanBeSelected() {
        assertFailsWith<IllegalArgumentException> { schoolDateFor(thursday, 0, 0) }
        assertFailsWith<IllegalArgumentException> { schoolDateFor(thursday, 0, 6) }
    }
}
