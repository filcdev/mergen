package hu.petrik.filcapp.widgets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WidgetNavigationTest {
    @Test
    fun lessonWidgetRoutesToTimetableWithSelectedTeacher() {
        WidgetNavigation.open("CURRENT_LESSON", "TEACHER", "teacher-1")
        val request = requireNotNull(WidgetNavigation.request)
        assertEquals(WidgetDestination.TIMETABLE, request.destination)
        assertEquals(WidgetSelectionType.TEACHER, request.scope)
        assertEquals("teacher-1", request.selectionId)
        WidgetNavigation.consume(request.serial)
        assertNull(WidgetNavigation.request)
    }

    @Test
    fun substitutionWidgetRoutesToItsSelectedClass() {
        WidgetNavigation.open("SUBSTITUTIONS", "COHORT", "cohort-2")
        val request = requireNotNull(WidgetNavigation.request)
        assertEquals(WidgetDestination.SUBSTITUTIONS, request.destination)
        assertEquals(WidgetSelectionType.COHORT, request.scope)
        assertEquals("cohort-2", request.selectionId)
        WidgetNavigation.consume(request.serial)
        assertNull(WidgetNavigation.request)
    }

    @Test
    fun invalidOrOldRequestsDoNotReplaceTheCurrentRequest() {
        WidgetNavigation.open("CURRENT_LESSON", "TEACHER", "valid-id")
        val request = requireNotNull(WidgetNavigation.request)
        WidgetNavigation.open("INVALID", "TEACHER", "bad-id")
        WidgetNavigation.consume(request.serial - 1)
        assertEquals(request, WidgetNavigation.request)
        WidgetNavigation.consume(request.serial)
        assertNull(WidgetNavigation.request)
    }
}
