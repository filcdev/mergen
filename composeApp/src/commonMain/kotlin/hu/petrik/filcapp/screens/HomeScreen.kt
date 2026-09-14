package hu.petrik.filcapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import hu.petrik.filcapp.components.DateView
import hu.petrik.filcapp.settings.tr

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 16.dp),
    ) {
        DateView()
    }
}

object HomeTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = tr("Kezdőlap", "Home")
            val icon = rememberVectorPainter(Icons.Default.Home)
            return remember(title) {
                TabOptions(index = 0u, title = title, icon = icon)
            }
        }

    @Composable
    override fun Content() {
        HomeScreen()
    }
}
