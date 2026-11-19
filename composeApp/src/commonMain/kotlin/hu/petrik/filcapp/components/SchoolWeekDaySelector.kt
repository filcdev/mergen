package hu.petrik.filcapp.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.petrik.filcapp.settings.tr
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/** Shared date picker for timetable and substitutions. */
@Composable
fun SchoolWeekDaySelector(
    today: LocalDate,
    weekOffset: Int,
    selectedDayOrder: Int,
    onDaySelected: (Int) -> Unit,
) {
    val monday = schoolWeekMonday(today, weekOffset)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        (1..5).forEach { dayIndex ->
            val date = LocalDate.fromEpochDays(monday.toEpochDays() + dayIndex - 1)
            val selected = selectedDayOrder == dayIndex

            Surface(
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = 53.dp)
                        .clickable {
                            onDaySelected(dayIndex)
                        },
                shape = RoundedCornerShape(12.dp),
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                shadowElevation = if (selected) 3.dp else 0.dp,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 5.dp),
                ) {
                    Text(
                        text = schoolShortDayName(dayIndex),
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (selected) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            },
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )

                    Text(
                        text = date.day.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color =
                            if (selected) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                    )
                }
            }
        }
    }
}

internal fun schoolWeekMonday(
    date: LocalDate,
    weekOffset: Int,
): LocalDate {
    val daysSinceMonday =
        when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> 0
            DayOfWeek.TUESDAY -> 1
            DayOfWeek.WEDNESDAY -> 2
            DayOfWeek.THURSDAY -> 3
            DayOfWeek.FRIDAY -> 4
            DayOfWeek.SATURDAY -> 5
            DayOfWeek.SUNDAY -> 6
        }

    return LocalDate.fromEpochDays(
        date.toEpochDays() - daysSinceMonday + weekOffset * 7,
    )
}

internal fun schoolDefaultDay(day: DayOfWeek): Int =
    when (day) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> 1
    }

private fun schoolShortDayName(dayOrder: Int): String =
    when (dayOrder) {
        1 -> tr("Hét", "Mon")
        2 -> tr("Ked", "Tue")
        3 -> tr("Sze", "Wed")
        4 -> tr("Csü", "Thu")
        5 -> tr("Pén", "Fri")
        else -> ""
    }

/** Translate a selected school weekday and week offset to the actual date. */
internal fun schoolDateFor(
    today: LocalDate,
    weekOffset: Int,
    dayOrder: Int,
): LocalDate {
    require(dayOrder in 1..5)
    return LocalDate.fromEpochDays(schoolWeekMonday(today, weekOffset).toEpochDays() + dayOrder - 1)
}
