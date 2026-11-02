package hu.petrik.filcapp.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CohortPresentationTest {
    @Test
    fun displaysShortClassNamesWithoutDtoDump() {
        val cohorts =
            listOf(
                CohortDto(id = "one", name = "13.A", short = "13.A"),
                CohortDto(id = "two", name = "13.B", short = "13.B"),
            )
        val result = cohorts.cohortLabels()
        assertEquals("13.A, 13.B", result)
        assertFalse(result.contains("CohortDto"))
        assertFalse(result.contains("id="))
    }

    @Test
    fun fallsBackToFullNameAndRemovesDuplicateLabels() {
        val cohorts =
            listOf(
                CohortDto(id = "one", name = "2/14.S"),
                CohortDto(id = "two", name = "2/14.S", short = " "),
                CohortDto(id = "three", name = "", short = "13.E"),
            )
        assertEquals("2/14.S, 13.E", cohorts.cohortLabels())
        assertEquals("", emptyList<CohortDto>().cohortLabels())
    }

    @Test
    fun matchesClassByIdOrHumanReadableLabel() {
        val selected = CohortDto(id = "old-id", name = "13.A", short = "13.A")
        assertTrue(CohortDto(id = "old-id", name = "Different").matchesCohortSelection(selected))
        assertTrue(CohortDto(id = "new-id", name = "13.A").matchesCohortSelection(selected))
        assertTrue(CohortDto(id = "new-id", name = "Other", short = "13.a").matchesCohortSelection(selected))
        assertFalse(CohortDto(id = "another", name = "13.B", short = "13.B").matchesCohortSelection(selected))
        assertFalse(CohortDto(id = "", name = "").matchesCohortSelection(CohortDto(id = "", name = "")))
    }
}
