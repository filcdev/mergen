package hu.petrik.filcapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import filcapp.composeapp.generated.resources.Res
import filcapp.composeapp.generated.resources.compose_multiplatform
import hu.petrik.filcapp.components.TopBar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

object TimetableTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Timetable"
            val icon = rememberVectorPainter(Icons.Default.CalendarMonth)
            return remember {
                TabOptions(index = 1u, title = title, icon = icon)
            }
        }

    @Composable
    override fun Content() {
        TimetableScreen()
    }
}

@Composable
fun TimetableScreen() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(Res.drawable.compose_multiplatform), null)
            Text(
                "Timetable placeholder",
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}
