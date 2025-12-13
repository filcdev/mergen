package hu.petrik.filcapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import hu.petrik.filcapp.components.TopBar
import hu.petrik.filcapp.screens.HomeTab
import hu.petrik.filcapp.screens.NewsTab
import hu.petrik.filcapp.screens.SubstitutionTab
import hu.petrik.filcapp.screens.TimetableTab
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        TabNavigator(HomeTab) { tabNavigator ->
            Scaffold(
                topBar = { TopBar() },
                bottomBar = { BottomNavigationBar(tabNavigator) },
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.systemBars,
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    CurrentTab()
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(tabNavigator: TabNavigator) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        listOf(HomeTab, TimetableTab, SubstitutionTab, NewsTab).forEach { tab ->
            val isSelected = tabNavigator.current.options.index == tab.options.index
            NavigationBarItem(
                icon = {
                    tab.options.icon?.let { painter ->
                        Icon(painter, contentDescription = tab.options.title)
                    }
                },
                label = { Text(tab.options.title) },
                selected = isSelected,
                onClick = { tabNavigator.current = tab },
            )
        }
    }
}
