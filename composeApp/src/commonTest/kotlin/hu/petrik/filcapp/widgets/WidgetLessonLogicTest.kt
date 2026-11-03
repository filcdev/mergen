package hu.petrik.filcapp.widgets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WidgetLessonLogicTest {
    private val lesson =
        WidgetLesson(
            day = 2,
            period = 3,
            startMinute = 10 * 60,
            endMinute = 10 * 60 + 45,
            subject = "Matematika",
            details = "201",
        )

    @Test
    fun identifiesCurrentLessonAtStartAndUntilEnd() {
        assertEquals(lesson, WidgetLessonLogic.currentLesson(listOf(lesson), 2, 600))
        assertEquals(lesson, WidgetLessonLogic.currentLesson(listOf(lesson), 2, 644))
        assertNull(WidgetLessonLogic.currentLesson(listOf(lesson), 2, 645))
        assertNull(WidgetLessonLogic.currentLesson(listOf(lesson), 3, 620))
    }

    @Test
    fun identifiesNextLessonOnlyAfterNow() {
        assertEquals(lesson, WidgetLessonLogic.nextLesson(listOf(lesson), 2, 599))
        assertNull(WidgetLessonLogic.nextLesson(listOf(lesson), 2, 600))
    }

    @Test
    fun progressIsBoundedAndTimeProportional() {
        assertEquals(0, WidgetLessonLogic.progressPercent(lesson, 599L * 60_000L))
        assertEquals(50, WidgetLessonLogic.progressPercent(lesson, 622L * 60_000L + 30_000L))
        assertEquals(100, WidgetLessonLogic.progressPercent(lesson, 645L * 60_000L))
    }

    @Test
    fun zeroLengthPeriodCannotDivideByZero() {
        val invalid = lesson.copy(endMinute = lesson.startMinute)
        assertEquals(0, WidgetLessonLogic.progressPercent(invalid, 0L))
    }
}
