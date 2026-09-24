package hu.petrik.filcapp.calendar

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val SCHOOL_CALENDAR_URL =
    "https://outlook.office365.com/calendar/published/" +
        "d318bbea4a914be2abe826c0b376cc50@petrik.hu/" +
        "240a9f1bbed44e0fb49ae8369939f7773872042457095598299/calendar.ics"

private val petrikTimeZone = TimeZone.of("Europe/Budapest")

data class SchoolCalendarEvent(
    val id: String,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime? = null,
    val allDay: Boolean = false,
    val location: String? = null,
    val description: String? = null,
) {
    fun occursOn(date: LocalDate): Boolean {
        val endDate = end?.date
        return if (allDay) {
            // iCalendar DATE end values are exclusive.
            date >= start.date && (endDate == null || date < endDate)
        } else {
            date >= start.date && date <= (endDate ?: start.date)
        }
    }
}

object SchoolCalendarApi {
    private val client = HttpClient { expectSuccess = true }

    suspend fun getEvents(): List<SchoolCalendarEvent> {
        val payload =
            client
                .get(SCHOOL_CALENDAR_URL) {
                    header(HttpHeaders.Accept, "text/calendar, text/plain;q=0.9, */*;q=0.8")
                }.bodyAsText()

        return OutlookIcsParser.parse(payload)
    }
}

private data class IcsProperty(
    val name: String,
    val params: Map<String, String>,
    val value: String,
)

private data class ParsedDateTime(
    val value: LocalDateTime,
    val allDay: Boolean,
)

private data class RawCalendarEvent(
    val id: String,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime?,
    val allDay: Boolean,
    val location: String?,
    val description: String?,
    val recurrenceRule: String?,
    val recurrenceId: LocalDateTime?,
    val excludedStarts: Set<LocalDateTime>,
    val cancelled: Boolean,
)

private data class RecurrenceRule(
    val frequency: String,
    val interval: Int,
    val count: Int?,
    val until: LocalDateTime?,
    val byDays: List<DayOfWeek>,
    val byMonthDays: List<Int>,
)

private object OutlookIcsParser {
    @OptIn(ExperimentalTime::class)
    fun parse(payload: String): List<SchoolCalendarEvent> {
        val rawEvents = parseRawEvents(unfold(payload))
        val now = Clock.System.now().toLocalDateTime(petrikTimeZone)
        val horizonStart = addDays(now.date, -62)
        val horizonEnd = addDays(now.date, 550)

        return rawEvents
            .groupBy { it.id }
            .values
            .flatMap { series ->
                val overrides = series.filter { it.recurrenceId != null }
                val overriddenStarts = overrides.mapNotNull { it.recurrenceId }.toSet()
                val masters = series.filter { it.recurrenceId == null }

                masters.flatMap { event ->
                    expand(event, horizonStart, horizonEnd, overriddenStarts)
                } + overrides.filterNot { it.cancelled }.map { it.toPublicEvent() }
            }
            .filterNot { it.title.isBlank() }
            .filter { event ->
                (event.end?.date ?: event.start.date) >= horizonStart &&
                    event.start.date <= horizonEnd
            }
            .distinctBy { event -> "${event.id}|${event.start}|${event.title}" }
            .sortedWith(compareBy<SchoolCalendarEvent> { it.start }.thenBy { it.title })
    }

    private fun unfold(payload: String): List<String> {
        val normalized = payload.replace("\r\n", "\n").replace('\r', '\n')
        val result = mutableListOf<String>()

        normalized.lines().forEach { line ->
            if ((line.startsWith(" ") || line.startsWith("\t")) && result.isNotEmpty()) {
                result[result.lastIndex] += line.drop(1)
            } else {
                result += line
            }
        }

        return result
    }

    private fun parseRawEvents(lines: List<String>): List<RawCalendarEvent> {
        val result = mutableListOf<RawCalendarEvent>()
        var current: MutableList<String>? = null

        lines.forEach { line ->
            when (line.trim().uppercase()) {
                "BEGIN:VEVENT" -> current = mutableListOf()
                "END:VEVENT" -> {
                    current?.let { parseEvent(it)?.let(result::add) }
                    current = null
                }
                else -> current?.add(line)
            }
        }

        return result
    }

    private fun parseEvent(lines: List<String>): RawCalendarEvent? {
        val properties = lines.mapNotNull(::parseProperty)
        val startProperty = properties.firstOrNull { it.name == "DTSTART" } ?: return null
        val start = parseDateTime(startProperty) ?: return null
        val end = properties.firstOrNull { it.name == "DTEND" }?.let(::parseDateTime)?.value
        val id = properties.firstOrNull { it.name == "UID" }?.value?.trim().orEmpty()
        val title = properties.firstOrNull { it.name == "SUMMARY" }?.value?.decodeIcsText().orEmpty()
        val location =
            properties
                .firstOrNull { it.name == "LOCATION" }
                ?.value
                ?.decodeIcsText()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        val description =
            properties
                .firstOrNull { it.name == "DESCRIPTION" }
                ?.value
                ?.decodeIcsText()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        val status = properties.firstOrNull { it.name == "STATUS" }?.value?.trim()?.uppercase()
        val recurrenceRule = properties.firstOrNull { it.name == "RRULE" }?.value?.trim()
        val recurrenceId =
            properties.firstOrNull { it.name == "RECURRENCE-ID" }?.let(::parseDateTime)?.value
        val excludedStarts =
            properties
                .filter { it.name == "EXDATE" }
                .flatMap { property ->
                    property.value.split(',').mapNotNull { value ->
                        parseDateTime(property.copy(value = value.trim()))?.value
                    }
                }.toSet()

        return RawCalendarEvent(
            id = id.ifBlank { "${start.value}|$title" },
            title = title,
            start = start.value,
            end = end,
            allDay = start.allDay,
            location = location,
            description = description,
            recurrenceRule = recurrenceRule,
            recurrenceId = recurrenceId,
            excludedStarts = excludedStarts,
            cancelled = status == "CANCELLED",
        )
    }

    private fun parseProperty(line: String): IcsProperty? {
        val colon = line.indexOf(':')
        if (colon <= 0) return null

        val header = line.substring(0, colon)
        val value = line.substring(colon + 1)
        val headerParts = header.split(';')
        val name = headerParts.first().uppercase()
        val params =
            headerParts
                .drop(1)
                .mapNotNull { part ->
                    val equals = part.indexOf('=')
                    if (equals <= 0) return@mapNotNull null
                    part.substring(0, equals).uppercase() to
                        part.substring(equals + 1).trim().trim('"')
                }.toMap()

        return IcsProperty(name = name, params = params, value = value)
    }

    private fun parseDateTime(property: IcsProperty): ParsedDateTime? {
        val raw = property.value.trim()
        if (raw.length < 8) return null

        val dateOnly =
            property.params["VALUE"]?.equals("DATE", ignoreCase = true) == true ||
                !raw.contains('T')

        val year = raw.substring(0, 4).toIntOrNull() ?: return null
        val month = raw.substring(4, 6).toIntOrNull() ?: return null
        val day = raw.substring(6, 8).toIntOrNull() ?: return null

        if (dateOnly) {
            return runCatching {
                ParsedDateTime(
                    value = LocalDateTime(year, month, day, 0, 0, 0),
                    allDay = true,
                )
            }.getOrNull()
        }

        val timePart = raw.substringAfter('T').removeSuffix("Z")
        val hour = timePart.take(2).toIntOrNull() ?: return null
        val minute = timePart.drop(2).take(2).toIntOrNull() ?: 0
        val second = timePart.drop(4).take(2).toIntOrNull() ?: 0

        val local =
            runCatching { LocalDateTime(year, month, day, hour, minute, second) }.getOrNull()
                ?: return null

        val converted =
            when {
                raw.endsWith("Z", ignoreCase = true) ->
                    runCatching {
                        local.toInstant(TimeZone.UTC).toLocalDateTime(petrikTimeZone)
                    }.getOrDefault(local)

                property.params["TZID"] != null -> {
                    val sourceZone = resolveTimeZone(property.params.getValue("TZID"))
                    runCatching {
                        local.toInstant(sourceZone).toLocalDateTime(petrikTimeZone)
                    }.getOrDefault(local)
                }

                else -> local
            }

        return ParsedDateTime(value = converted, allDay = false)
    }

    private fun resolveTimeZone(rawId: String): TimeZone {
        val normalized = rawId.trim().trim('"')
        val ianaId =
            when (normalized.lowercase()) {
                "central europe standard time",
                "central european standard time",
                "w. europe standard time",
                "romance standard time",
                -> "Europe/Budapest"

                else -> normalized
            }

        return runCatching { TimeZone.of(ianaId) }.getOrDefault(petrikTimeZone)
    }

    private fun expand(
        event: RawCalendarEvent,
        horizonStart: LocalDate,
        horizonEnd: LocalDate,
        overriddenStarts: Set<LocalDateTime> = emptySet(),
    ): List<SchoolCalendarEvent> {
        if (event.cancelled) return emptyList()

        val rule = parseRecurrenceRule(event.recurrenceRule)
            ?: return listOf(event.toPublicEvent())

        val occurrences =
            when (rule.frequency) {
                "DAILY" -> dailyOccurrences(event, rule, horizonEnd)
                "WEEKLY" -> weeklyOccurrences(event, rule, horizonEnd)
                "MONTHLY" -> monthlyOccurrences(event, rule, horizonEnd)
                "YEARLY" -> yearlyOccurrences(event, rule, horizonEnd)
                else -> listOf(event.start)
            }

        return occurrences
            .asSequence()
            .filter { start -> start !in event.excludedStarts && start !in overriddenStarts }
            .filter { start -> start.date >= horizonStart && start.date <= horizonEnd }
            .map { start -> event.toPublicEvent(start) }
            .toList()
    }

    private fun parseRecurrenceRule(raw: String?): RecurrenceRule? {
        if (raw.isNullOrBlank()) return null

        val values =
            raw.split(';')
                .mapNotNull { part ->
                    val equals = part.indexOf('=')
                    if (equals <= 0) return@mapNotNull null
                    part.substring(0, equals).uppercase() to part.substring(equals + 1)
                }.toMap()

        val frequency = values["FREQ"]?.uppercase() ?: return null
        val interval = values["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val count = values["COUNT"]?.toIntOrNull()?.takeIf { it > 0 }
        val until =
            values["UNTIL"]?.let { rawUntil ->
                parseDateTime(IcsProperty("UNTIL", emptyMap(), rawUntil))?.value
            }
        val byDays =
            values["BYDAY"]
                ?.split(',')
                ?.mapNotNull(::parseWeekday)
                ?.distinct()
                .orEmpty()
        val byMonthDays =
            values["BYMONTHDAY"]
                ?.split(',')
                ?.mapNotNull { it.toIntOrNull() }
                ?.filter { it in 1..31 }
                ?.distinct()
                .orEmpty()

        return RecurrenceRule(
            frequency = frequency,
            interval = interval,
            count = count,
            until = until,
            byDays = byDays,
            byMonthDays = byMonthDays,
        )
    }

    private fun parseWeekday(value: String): DayOfWeek? {
        val suffix = value.trim().uppercase().takeLast(2)
        return when (suffix) {
            "MO" -> DayOfWeek.MONDAY
            "TU" -> DayOfWeek.TUESDAY
            "WE" -> DayOfWeek.WEDNESDAY
            "TH" -> DayOfWeek.THURSDAY
            "FR" -> DayOfWeek.FRIDAY
            "SA" -> DayOfWeek.SATURDAY
            "SU" -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    private fun dailyOccurrences(
        event: RawCalendarEvent,
        rule: RecurrenceRule,
        horizonEnd: LocalDate,
    ): List<LocalDateTime> {
        val result = mutableListOf<LocalDateTime>()
        var occurrence = event.start
        var emitted = 0
        var iterations = 0

        while (iterations++ < MAX_RECURRENCE_ITERATIONS) {
            if (rule.count != null && emitted >= rule.count) break
            if (rule.until != null && occurrence > rule.until) break
            if (occurrence.date > horizonEnd) break

            result += occurrence
            emitted++
            occurrence = withDate(occurrence, addDays(occurrence.date, rule.interval))
        }

        return result
    }

    private fun weeklyOccurrences(
        event: RawCalendarEvent,
        rule: RecurrenceRule,
        horizonEnd: LocalDate,
    ): List<LocalDateTime> {
        val result = mutableListOf<LocalDateTime>()
        val weekdays = (rule.byDays.ifEmpty { listOf(event.start.date.dayOfWeek) }).sortedBy(::weekdayIndex)
        var weekStart = startOfWeek(event.start.date)
        var emitted = 0
        var iterations = 0

        while (iterations++ < MAX_RECURRENCE_ITERATIONS && weekStart <= horizonEnd) {
            weekdays.forEach { weekday ->
                if (rule.count != null && emitted >= rule.count) return result

                val date = addDays(weekStart, weekdayIndex(weekday))
                val occurrence = withDate(event.start, date)
                if (occurrence < event.start) return@forEach
                if (rule.until != null && occurrence > rule.until) return result
                if (date > horizonEnd) return result

                result += occurrence
                emitted++
            }

            weekStart = addDays(weekStart, 7 * rule.interval)
        }

        return result
    }

    private fun monthlyOccurrences(
        event: RawCalendarEvent,
        rule: RecurrenceRule,
        horizonEnd: LocalDate,
    ): List<LocalDateTime> {
        val result = mutableListOf<LocalDateTime>()
        var year = event.start.year
        var month = event.start.monthNumber
        var emitted = 0
        var iterations = 0

        while (iterations++ < MAX_RECURRENCE_ITERATIONS) {
            val firstOfMonth = LocalDate(year, month, 1)
            if (firstOfMonth > horizonEnd) break

            val monthDays = rule.byMonthDays.ifEmpty { listOf(event.start.day) }
            monthDays.sorted().forEach { day ->
                if (rule.count != null && emitted >= rule.count) return result
                if (day > daysInMonth(year, month)) return@forEach

                val occurrence = withDate(event.start, LocalDate(year, month, day))
                if (occurrence < event.start) return@forEach
                if (rule.until != null && occurrence > rule.until) return result
                if (occurrence.date > horizonEnd) return result

                result += occurrence
                emitted++
            }

            repeat(rule.interval) {
                month++
                if (month > 12) {
                    month = 1
                    year++
                }
            }
        }

        return result
    }

    private fun yearlyOccurrences(
        event: RawCalendarEvent,
        rule: RecurrenceRule,
        horizonEnd: LocalDate,
    ): List<LocalDateTime> {
        val result = mutableListOf<LocalDateTime>()
        var year = event.start.year
        var emitted = 0
        var iterations = 0

        while (iterations++ < MAX_RECURRENCE_ITERATIONS) {
            if (rule.count != null && emitted >= rule.count) break
            if (event.start.monthNumber == 2 && event.start.day == 29 && !isLeapYear(year)) {
                year += rule.interval
                continue
            }

            val occurrence =
                withDate(
                    event.start,
                    LocalDate(year, event.start.monthNumber, event.start.day),
                )
            if (rule.until != null && occurrence > rule.until) break
            if (occurrence.date > horizonEnd) break
            if (occurrence >= event.start) {
                result += occurrence
                emitted++
            }

            year += rule.interval
        }

        return result
    }

    private fun RawCalendarEvent.toPublicEvent(newStart: LocalDateTime = start): SchoolCalendarEvent {
        val shiftedEnd = end?.let { originalEnd -> shiftEnd(start, originalEnd, newStart) }
        return SchoolCalendarEvent(
            id = id,
            title = title,
            start = newStart,
            end = shiftedEnd,
            allDay = allDay,
            location = location,
            description = description,
        )
    }

    private fun shiftEnd(
        originalStart: LocalDateTime,
        originalEnd: LocalDateTime,
        newStart: LocalDateTime,
    ): LocalDateTime {
        val daySpan = daysBetween(originalStart.date, originalEnd.date)
        val newEndDate = addDays(newStart.date, daySpan)
        return LocalDateTime(
            newEndDate.year,
            newEndDate.monthNumber,
            newEndDate.day,
            originalEnd.hour,
            originalEnd.minute,
            originalEnd.second,
        )
    }

    private fun String.decodeIcsText(): String =
        replace("\\n", "\n", ignoreCase = true)
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")

    private fun withDate(
        source: LocalDateTime,
        date: LocalDate,
    ): LocalDateTime =
        LocalDateTime(
            date.year,
            date.monthNumber,
            date.day,
            source.hour,
            source.minute,
            source.second,
        )

    private fun startOfWeek(date: LocalDate): LocalDate = addDays(date, -weekdayIndex(date.dayOfWeek))

    private fun weekdayIndex(day: DayOfWeek): Int =
        when (day) {
            DayOfWeek.MONDAY -> 0
            DayOfWeek.TUESDAY -> 1
            DayOfWeek.WEDNESDAY -> 2
            DayOfWeek.THURSDAY -> 3
            DayOfWeek.FRIDAY -> 4
            DayOfWeek.SATURDAY -> 5
            DayOfWeek.SUNDAY -> 6
        }

    private fun addDays(
        source: LocalDate,
        amount: Int,
    ): LocalDate {
        var result = source
        if (amount >= 0) {
            repeat(amount) { result = nextDay(result) }
        } else {
            repeat(-amount) { result = previousDay(result) }
        }
        return result
    }

    private fun nextDay(date: LocalDate): LocalDate {
        val lastDay = daysInMonth(date.year, date.monthNumber)
        return when {
            date.day < lastDay -> LocalDate(date.year, date.monthNumber, date.day + 1)
            date.monthNumber < 12 -> LocalDate(date.year, date.monthNumber + 1, 1)
            else -> LocalDate(date.year + 1, 1, 1)
        }
    }

    private fun previousDay(date: LocalDate): LocalDate =
        when {
            date.day > 1 -> LocalDate(date.year, date.monthNumber, date.day - 1)
            date.monthNumber > 1 -> {
                val month = date.monthNumber - 1
                LocalDate(date.year, month, daysInMonth(date.year, month))
            }
            else -> LocalDate(date.year - 1, 12, 31)
        }

    private fun daysBetween(
        start: LocalDate,
        end: LocalDate,
    ): Int {
        if (start == end) return 0

        var cursor = start
        var count = 0
        if (end > start) {
            while (cursor < end && count < 5000) {
                cursor = nextDay(cursor)
                count++
            }
        } else {
            while (cursor > end && count > -5000) {
                cursor = previousDay(cursor)
                count--
            }
        }
        return count
    }

    private fun daysInMonth(
        year: Int,
        month: Int,
    ): Int =
        when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 30
        }

    private fun isLeapYear(year: Int): Boolean =
        year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)

    private const val MAX_RECURRENCE_ITERATIONS = 10_000
}
