package hu.petrik.filcapp.widgets

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import hu.petrik.filcapp.auth.AndroidAuthContext
import hu.petrik.filcapp.auth.Auth
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.data.ChronosApi
import hu.petrik.filcapp.data.LessonDto
import hu.petrik.filcapp.data.TimetableFilter
import kotlinx.coroutines.CancellationException
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Fetches over the existing secure Better Auth session; credentials are never put into widget preferences. */
class WidgetSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val ids = MergenWidgetProvider.widgetIds(applicationContext)
        if (ids.isEmpty()) {
            return Result.success()
        }
        AndroidAuthContext.initialize(applicationContext)
        try {
            Auth.refreshSession()
            if (Auth.state.value !is AuthState.SignedIn) {
                WidgetRenderer.renderAll(applicationContext)
                return Result.success()
            }
            val configs =
                ids.toList().mapNotNull { id ->
                    WidgetStorage.readConfig(applicationContext, id)?.let { id to it }
                }
            if (configs.isEmpty()) {
                return Result.success()
            }
            val hasLessons = configs.any { it.second.mode == WidgetMode.CURRENT_LESSON }
            val hasChanges = configs.any { it.second.mode == WidgetMode.SUBSTITUTIONS }
            val timetableId = if (hasLessons) ChronosApi.getLatestValidTimetable().id else null
            val substitutions = if (hasChanges) ChronosApi.getSubstitutions() else emptyList()
            var failed = false
            for ((id, config) in configs) {
                try {
                    val snapshot =
                        when (config.mode) {
                            WidgetMode.CURRENT_LESSON -> {
                                val lessons =
                                    ChronosApi.getLessons(
                                        filter =
                                            when (config.scope) {
                                                WidgetScope.TEACHER -> TimetableFilter.TEACHER
                                                WidgetScope.COHORT -> TimetableFilter.COHORT
                                            },
                                        selectionId = config.selectionId,
                                        timetableId = requireNotNull(timetableId),
                                    )
                                WidgetSnapshot(
                                    lessons = lessons.mapNotNull(::mapLesson),
                                    updatedAt = System.currentTimeMillis(),
                                )
                            }

                            WidgetMode.SUBSTITUTIONS -> {
                                val relevant =
                                    substitutions.filter { item ->
                                        when (config.scope) {
                                            WidgetScope.TEACHER -> {
                                                item.teacher?.id == config.selectionId ||
                                                    item.lessons.any { lesson ->
                                                        lesson.teachers.any { it.id == config.selectionId }
                                                    }
                                            }

                                            WidgetScope.COHORT -> {
                                                item.lessons.any { lesson ->
                                                    lesson.cohorts.any { it.id == config.selectionId }
                                                }
                                            }
                                        }
                                    }
                                WidgetSnapshot(
                                    substitutions =
                                        relevant.map { item ->
                                            val first = item.lessons.firstOrNull()
                                            val substitution = item.teacher?.displayName ?: "Elmarad"
                                            WidgetSubstitution(
                                                date = item.substitution.date.take(10),
                                                period = first?.period?.period ?: 0,
                                                subject = first?.subject?.name.orEmpty().ifBlank { "Órarendi változás" },
                                                details = substitution,
                                            )
                                        },
                                    updatedAt = System.currentTimeMillis(),
                                )
                            }
                        }
                    WidgetStorage.saveSnapshot(applicationContext, id, snapshot)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    failed = true
                }
            }
            WidgetRenderer.renderAll(applicationContext)
            return if (failed) Result.retry() else Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            WidgetRenderer.renderAll(applicationContext)
            return Result.retry()
        }
    }

    private fun mapLesson(lesson: LessonDto): WidgetLesson? {
        val period = lesson.period ?: return null
        val start = parseMinute(period.startTime) ?: return null
        val end = parseMinute(period.endTime) ?: return null
        if (end <= start) {
            return null
        }
        val day = dayIndex(lesson.day?.name.orEmpty())
        if (day !in 1..7) {
            return null
        }
        val rooms = lesson.classrooms.joinToString(", ") { it.short.ifBlank { it.name } }
        val week = lesson.weekDefinition?.short.orEmpty()
        val details = listOf(rooms, week).filter { it.isNotBlank() }.joinToString(" · ")
        return WidgetLesson(
            day = day,
            period = period.period,
            startMinute = start,
            endMinute = end,
            subject = lesson.subject?.name.orEmpty().ifBlank { "Tanóra" },
            details = details,
            week = week,
        )
    }

    private fun parseMinute(raw: String): Int? {
        val parts = raw.split(':')
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) {
            return null
        }
        return hour * 60 + minute
    }

    private fun dayIndex(value: String): Int {
        val day = value.lowercase(Locale.ROOT)
        return when {
            day.contains("hétf") || day.contains("hetf") || day.contains("monday") -> 1
            day.contains("kedd") || day.contains("tuesday") -> 2
            day.contains("szerda") || day.contains("wednesday") -> 3
            day.contains("csüt") || day.contains("csut") || day.contains("thursday") -> 4
            day.contains("pént") || day.contains("pent") || day.contains("friday") -> 5
            day.contains("szomb") || day.contains("saturday") -> 6
            day.contains("vasár") || day.contains("vasar") || day.contains("sunday") -> 7
            else -> 0
        }
    }

    companion object {
        private const val PERIODIC = "mergen_widgets_periodic"
        private const val IMMEDIATE = "mergen_widgets_immediate"
        private val network =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedulePeriodic(context: Context) {
            val work =
                PeriodicWorkRequestBuilder<WidgetSyncWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(network)
                    .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                work,
            )
        }

        fun requestNow(context: Context) {
            val work =
                OneTimeWorkRequestBuilder<WidgetSyncWorker>()
                    .setConstraints(network)
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                work,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
            WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE)
        }
    }
}
