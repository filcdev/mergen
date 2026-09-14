package hu.petrik.filcapp.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.petrik.filcapp.settings.tr
import kotlinx.coroutines.delay
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val petrikTimeZone = TimeZone.of("Europe/Budapest")

@OptIn(ExperimentalTime::class)
@Composable
fun DateView(modifier: Modifier = Modifier) {
    var currentTime by remember {
        mutableStateOf(
            Clock.System.now().toLocalDateTime(petrikTimeZone),
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Clock.System.now().toLocalDateTime(petrikTimeZone)
            delay(1000L)
        }
    }

    val dayOfWeek =
        when (currentTime.dayOfWeek) {
            DayOfWeek.MONDAY -> tr("Hétfő", "Monday")
            DayOfWeek.TUESDAY -> tr("Kedd", "Tuesday")
            DayOfWeek.WEDNESDAY -> tr("Szerda", "Wednesday")
            DayOfWeek.THURSDAY -> tr("Csütörtök", "Thursday")
            DayOfWeek.FRIDAY -> tr("Péntek", "Friday")
            DayOfWeek.SATURDAY -> tr("Szombat", "Saturday")
            DayOfWeek.SUNDAY -> tr("Vasárnap", "Sunday")
        }

    val monthName =
        when (currentTime.month) {
            Month.JANUARY -> tr("január", "Jan")
            Month.FEBRUARY -> tr("február", "Feb")
            Month.MARCH -> tr("március", "Mar")
            Month.APRIL -> tr("április", "Apr")
            Month.MAY -> tr("május", "May")
            Month.JUNE -> tr("június", "Jun")
            Month.JULY -> tr("július", "Jul")
            Month.AUGUST -> tr("augusztus", "Aug")
            Month.SEPTEMBER -> tr("szeptember", "Sep")
            Month.OCTOBER -> tr("október", "Oct")
            Month.NOVEMBER -> tr("november", "Nov")
            Month.DECEMBER -> tr("december", "Dec")
        }

    val hour = currentTime.hour.toString().padStart(2, '0')
    val minute = currentTime.minute.toString().padStart(2, '0')
    val dateText =
        tr(
            "${currentTime.year}. $monthName ${currentTime.day}.",
            "${currentTime.day} $monthName ${currentTime.year}",
        )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = dayOfWeek,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "$hour:$minute",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = tr("Petrik idő · Budapest", "Petrik time · Budapest"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.padding(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
