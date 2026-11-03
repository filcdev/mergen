package hu.petrik.filcapp.widgets

import kotlinx.serialization.Serializable

@Serializable
data class WidgetLesson(
    val day: Int,
    val period: Int,
    val startMinute: Int,
    val endMinute: Int,
    val subject: String,
    val details: String,
    val week: String = "",
)

@Serializable
data class WidgetSubstitution(
    val date: String,
    val period: Int,
    val subject: String,
    val details: String,
)

@Serializable
data class WidgetSnapshot(
    val lessons: List<WidgetLesson> = emptyList(),
    val substitutions: List<WidgetSubstitution> = emptyList(),
    val updatedAt: Long = 0L,
)

/** No Android dependencies: both widget rendering and unit tests use the same clock logic. */
object WidgetLessonLogic {
    fun currentLesson(
        lessons: List<WidgetLesson>,
        day: Int,
        minute: Int,
    ): WidgetLesson? =
        lessons.firstOrNull { lesson ->
            lesson.day == day && minute >= lesson.startMinute && minute < lesson.endMinute
        }

    fun nextLesson(
        lessons: List<WidgetLesson>,
        day: Int,
        minute: Int,
    ): WidgetLesson? =
        lessons
            .filter { it.day == day && it.startMinute > minute }
            .minWithOrNull(compareBy({ it.startMinute }, { it.period }))

    fun progressPercent(
        lesson: WidgetLesson,
        millisSinceMidnight: Long,
    ): Int {
        val start = lesson.startMinute * 60_000L
        val duration = (lesson.endMinute - lesson.startMinute) * 60_000L
        if (duration <= 0L) {
            return 0
        }
        return (((millisSinceMidnight - start) * 100L) / duration).toInt().coerceIn(0, 100)
    }
}
