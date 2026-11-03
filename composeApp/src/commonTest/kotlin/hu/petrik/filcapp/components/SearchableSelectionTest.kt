package hu.petrik.filcapp.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchableSelectionTest {
    @Test
    fun findsTeacherInLargeListWithoutLimitingVisibleMatches() {
        val teachers = (1..500).map { it.toString() to "Tanár $it" }

        assertEquals(listOf("249" to "Tanár 249"), filterSelectionOptions(teachers, "TANÁR 249"))
        assertEquals(500, filterSelectionOptions(teachers, "").size)
    }

    @Test
    fun matchesAccentedAndUnaccentedHungarianTeacherNames() {
        val teachers =
            listOf(
                "a" to "Merényi Miklós",
                "b" to "Merenyi Anna",
                "c" to "Tóth Őrs",
                "d" to "Szűcs Ágnes",
            )

        assertEquals(teachers.take(2), filterSelectionOptions(teachers, "Merenyi"))
        assertEquals(teachers.take(2), filterSelectionOptions(teachers, "MERÉNYI"))
        assertEquals(listOf(teachers[2]), filterSelectionOptions(teachers, "toth ors"))
        assertEquals(listOf(teachers[3]), filterSelectionOptions(teachers, "szucs agnes"))
    }

    @Test
    fun matchesDecomposedUnicodeAccentsAndKeepsOriginalLabels() {
        val teachers = listOf("a" to "Me\u0301renyi Miklós")

        assertEquals(teachers, filterSelectionOptions(teachers, "merényi"))
        assertEquals(teachers, filterSelectionOptions(teachers, "MeReNyI"))
    }

    @Test
    fun searchesClassNamesWithWhitespaceAndMixedCase() {
        val cohorts = listOf("a" to "13.A", "b" to "13.B", "c" to "2/14.S")

        assertEquals(listOf("b" to "13.B"), filterSelectionOptions(cohorts, " 13.b "))
        assertTrue(filterSelectionOptions(cohorts, "nem létezik").isEmpty())
    }
}
