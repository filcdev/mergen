package hu.petrik.filcapp.notifications

import hu.petrik.filcapp.settings.AppLanguage

/** Localized text for the inbox, independent of how Chronos stored the notification. */
internal data class NotificationDisplay(
    val title: String,
    val content: String,
)

private val teacherUuid =
    Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

private val hungarianDate =
    Regex(
        "(Hétfő|Kedd|Szerda|Csütörtök|Péntek|Szombat|Vasárnap),\\s*(\\d{4})\\.\\s*(\\d{1,2})\\.\\s*(\\d{1,2})\\.,?",
        RegexOption.IGNORE_CASE,
    )

private val announcementPrefix =
    Regex("^(?:New announcement|Új bejelentés|Új közlemény):\\s*", RegexOption.IGNORE_CASE)

private val systemMessagePrefix =
    Regex("^(?:System notice|Rendszerüzenet):\\s*", RegexOption.IGNORE_CASE)

internal fun notificationNeedsTeacherNames(entry: NotificationEntry): Boolean =
    (entry.type == "substitution" || entry.type == "substitution_teacher") && teacherUuid.containsMatchIn(entry.content)

internal fun displayNotification(
    entry: NotificationEntry,
    teacherNames: Map<String, String>,
    language: AppLanguage,
): NotificationDisplay {
    val hu = language == AppLanguage.HU
    val title =
        when (entry.type) {
            "announcement" -> if (hu) "Új közlemény" else "New announcement"
            "substitution" -> if (hu) "Új helyettesítés" else "New substitution"
            "system_message" -> if (hu) "Rendszerüzenet" else "System message"
            "blog_post" -> if (hu) "Új hír" else "New post"
            "moved_lesson" -> if (hu) "Áthelyezett óra" else "Moved lesson"
            else -> entry.title
        }
    val content =
        when (entry.type) {
            "announcement" -> announcementPrefix.replace(entry.content, "")
            "system_message" -> systemMessagePrefix.replace(entry.content, "")
            "substitution", "substitution_teacher" ->
                readableSubstitutionContent(entry.content, teacherNames, hu)
            else -> entry.content
        }
    return NotificationDisplay(title = title, content = content)
}

private fun readableSubstitutionContent(
    content: String,
    teacherNames: Map<String, String>,
    hu: Boolean,
): String {
    val withNames =
        teacherUuid.replace(content) { match ->
            teacherNames[match.value.lowercase()]?.takeIf { it.isNotBlank() }
                ?: if (hu) "Helyettesítő tanár" else "Substitute teacher"
        }
    return hungarianDate.replace(withNames) { match ->
        val weekday =
            when (match.groupValues[1].lowercase()) {
                "hétfő" -> "hétfőn"
                "kedd" -> "kedden"
                "szerda" -> "szerdán"
                "csütörtök" -> "csütörtökön"
                "péntek" -> "pénteken"
                "szombat" -> "szombaton"
                else -> "vasárnap"
            }
        val year = match.groupValues[2]
        val month = match.groupValues[3].padStart(2, '0')
        val day = match.groupValues[4].padStart(2, '0')
        "$weekday ($year. $month. $day.),"
    }
}
