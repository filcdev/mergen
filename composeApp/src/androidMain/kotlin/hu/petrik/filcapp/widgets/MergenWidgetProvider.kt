package hu.petrik.filcapp.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import hu.petrik.filcapp.MainActivity
import hu.petrik.filcapp.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MergenWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        WidgetRenderer.renderAll(context)
        WidgetSyncWorker.schedulePeriodic(context)
        WidgetSyncWorker.requestNow(context)
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TICK) {
            WidgetRenderer.renderAll(context)
        }
    }

    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { WidgetStorage.remove(context, it) }
        WidgetTicker.scheduleNext(context)
    }

    override fun onDisabled(context: Context) {
        WidgetSyncWorker.cancel(context)
        WidgetTicker.cancel(context)
    }

    companion object {
        const val ACTION_TICK = "hu.petrik.filcapp.widgets.TICK"

        fun widgetIds(context: Context): IntArray =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, MergenWidgetProvider::class.java))
    }
}

internal object WidgetRenderer {
    fun renderAll(context: Context) {
        MergenWidgetProvider.widgetIds(context).forEach { id -> render(context, id) }
        WidgetTicker.scheduleNext(context)
    }

    fun render(
        context: Context,
        widgetId: Int,
    ) {
        val config = WidgetStorage.readConfig(context, widgetId)
        val snapshot = WidgetStorage.readSnapshot(context, widgetId)
        val views = RemoteViews(context.packageName, R.layout.mergen_home_widget)
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        views.setViewVisibility(R.id.widget_progress, View.GONE)
        views.setViewVisibility(R.id.widget_countdown, View.GONE)

        if (config == null) {
            views.setTextViewText(R.id.widget_heading, "MERGEN")
            views.setTextViewText(R.id.widget_title, "Widget beállítása")
            views.setTextViewText(R.id.widget_details, "Koppints a beállításhoz.")
            views.setTextViewText(R.id.widget_footer, "")
        } else if (snapshot == null) {
            views.setTextViewText(R.id.widget_heading, "MERGEN · ${config.selectionName}")
            views.setTextViewText(R.id.widget_title, "Adatok betöltése…")
            views.setTextViewText(R.id.widget_details, "A bejelentkezéshez nyisd meg a Mergent.")
            views.setTextViewText(R.id.widget_footer, "")
        } else {
            val stale = System.currentTimeMillis() - snapshot.updatedAt > 2 * 60 * 60 * 1000L
            val freshAt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(snapshot.updatedAt))
            val freshness = if (stale) "Elavult · $freshAt" else "Frissítve $freshAt"
            views.setTextViewText(R.id.widget_footer, freshness)
            when (config.mode) {
                WidgetMode.CURRENT_LESSON -> showLesson(views, config, snapshot)
                WidgetMode.SUBSTITUTIONS -> showSubstitutions(views, config, snapshot)
            }
        }
        AppWidgetManager.getInstance(context).updateAppWidget(widgetId, views)
    }

    private fun showLesson(
        views: RemoteViews,
        config: WidgetConfig,
        snapshot: WidgetSnapshot,
    ) {
        val calendar = Calendar.getInstance()
        val day = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
        val minute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val millis =
            minute * 60_000L + calendar.get(Calendar.SECOND) * 1000L + calendar.get(Calendar.MILLISECOND)
        val active = WidgetLessonLogic.currentLesson(snapshot.lessons, day, minute)
        val next = WidgetLessonLogic.nextLesson(snapshot.lessons, day, minute)
        views.setTextViewText(R.id.widget_heading, "MERGEN · ${config.selectionName}")
        when {
            active != null -> {
                val progress = WidgetLessonLogic.progressPercent(active, millis)
                views.setTextViewText(R.id.widget_title, active.subject)
                views.setTextViewText(
                    R.id.widget_details,
                    "${active.period}. óra · ${clock(active.startMinute)}–${clock(active.endMinute)} · ${active.details}",
                )
                views.setProgressBar(R.id.widget_progress, 100, progress, false)
                views.setViewVisibility(R.id.widget_progress, View.VISIBLE)
                val remaining = (active.endMinute * 60_000L - millis).coerceAtLeast(0L)
                views.setChronometer(
                    R.id.widget_countdown,
                    SystemClock.elapsedRealtime() + remaining,
                    null,
                    true,
                )
                views.setChronometerCountDown(R.id.widget_countdown, true)
                views.setViewVisibility(R.id.widget_countdown, View.VISIBLE)
            }

            next != null -> {
                views.setTextViewText(R.id.widget_title, "Következő: ${next.subject}")
                views.setTextViewText(
                    R.id.widget_details,
                    "${next.period}. óra · ${clock(next.startMinute)}–${clock(next.endMinute)} · ${next.details}",
                )
            }

            else -> {
                views.setTextViewText(R.id.widget_title, "Mára nincs több óra")
                views.setTextViewText(R.id.widget_details, "Az órarend a következő tanítási napon folytatódik.")
            }
        }
    }

    private fun showSubstitutions(
        views: RemoteViews,
        config: WidgetConfig,
        snapshot: WidgetSnapshot,
    ) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val upcoming = snapshot.substitutions.filter { it.date >= today }
        val chosenDate = upcoming.minOfOrNull { it.date }
        val items = upcoming.filter { it.date == chosenDate }.sortedBy { it.period }
        views.setTextViewText(R.id.widget_heading, "MERGEN · Helyettesítések")
        if (items.isEmpty()) {
            views.setTextViewText(R.id.widget_title, "Nincs közelgő változás")
            views.setTextViewText(R.id.widget_details, config.selectionName)
        } else {
            val item = items.first()
            views.setTextViewText(R.id.widget_title, "${item.period}. óra · ${item.subject}")
            views.setTextViewText(
                R.id.widget_details,
                "${item.date} · ${item.details}" +
                    if (items.size > 1) " · +${items.size - 1} további" else "",
            )
        }
    }

    private fun clock(minutes: Int): String =
        "%02d:%02d".format(Locale.US, minutes / 60, minutes % 60)

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/** Refresh visible progress around minute boundaries; Android can delay alarms in Doze. */
internal object WidgetTicker {
    fun scheduleNext(context: Context) {
        val calendar = Calendar.getInstance()
        val day = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
        val minute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val nextMinute = (System.currentTimeMillis() / 60_000L + 1) * 60_000L
        val deadlines =
            MergenWidgetProvider.widgetIds(context).mapNotNull { id ->
                val config = WidgetStorage.readConfig(context, id)
                if (config?.mode != WidgetMode.CURRENT_LESSON) {
                    return@mapNotNull null
                }
                val lessons = WidgetStorage.readSnapshot(context, id)?.lessons.orEmpty()
                val active = WidgetLessonLogic.currentLesson(lessons, day, minute)
                if (active != null) {
                    nextMinute
                } else {
                    val next = WidgetLessonLogic.nextLesson(lessons, day, minute)
                    next?.let {
                        val millisInMinute =
                            calendar.get(Calendar.SECOND) * 1000L + calendar.get(Calendar.MILLISECOND)
                        System.currentTimeMillis() + (it.startMinute - minute) * 60_000L - millisInMinute
                    }
                }
            }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = tickIntent(context)
        alarmManager.cancel(pending)
        deadlines.minOrNull()?.let { target ->
            alarmManager.set(AlarmManager.RTC, target.coerceAtLeast(System.currentTimeMillis() + 1000L), pending)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(tickIntent(context))
    }

    private fun tickIntent(context: Context): PendingIntent {
        val intent = Intent(context, MergenWidgetProvider::class.java)
            .setAction(MergenWidgetProvider.ACTION_TICK)
        return PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
