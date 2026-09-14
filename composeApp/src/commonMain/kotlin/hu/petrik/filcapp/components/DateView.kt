package hu.petrik.filcapp.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
        mutableStateOf(Clock.System.now().toLocalDateTime(petrikTimeZone))
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
            Month.JANUARY -> tr("január", "January")
            Month.FEBRUARY -> tr("február", "February")
            Month.MARCH -> tr("március", "March")
            Month.APRIL -> tr("április", "April")
            Month.MAY -> tr("május", "May")
            Month.JUNE -> tr("június", "June")
            Month.JULY -> tr("július", "July")
            Month.AUGUST -> tr("augusztus", "August")
            Month.SEPTEMBER -> tr("szeptember", "September")
            Month.OCTOBER -> tr("október", "October")
            Month.NOVEMBER -> tr("november", "November")
            Month.DECEMBER -> tr("december", "December")
        }

    val hour = currentTime.hour.toString().padStart(2, '0')
    val minute = currentTime.minute.toString().padStart(2, '0')
    val dateText =
        tr(
            "${currentTime.year}. $monthName ${currentTime.day}.",
            "$monthName ${currentTime.day}, ${currentTime.year}",
        )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isSystemInDarkTheme()) 0.dp else 8.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = dayOfWeek,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "$hour:$minute",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
