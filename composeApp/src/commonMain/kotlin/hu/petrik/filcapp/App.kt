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
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.components.TopBar
import hu.petrik.filcapp.screens.CohortSwitcher
import hu.petrik.filcapp.screens.HomeTab
import hu.petrik.filcapp.screens.NewsTab
import hu.petrik.filcapp.screens.RoomSwitcher
import hu.petrik.filcapp.screens.SettingsSheet
import hu.petrik.filcapp.screens.SplashScreen
import hu.petrik.filcapp.screens.SubstitutionTab
import hu.petrik.filcapp.screens.TeacherSwitcher
import hu.petrik.filcapp.screens.TimetableMode
import hu.petrik.filcapp.screens.TimetableState
import hu.petrik.filcapp.screens.TimetableTab
import hu.petrik.filcapp.screens.WelcomeScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

private enum class AppScreen { Splash, Welcome, Main }

@Composable
@Preview
fun App() {
    var screen by remember { mutableStateOf(AppScreen.Splash) }

    MaterialTheme {
        when (screen) {
            AppScreen.Splash -> SplashScreen(
                onAuthResolved = { loggedIn ->
                    screen = if (loggedIn) AppScreen.Main else AppScreen.Welcome
                },
            )
            AppScreen.Welcome -> WelcomeScreen(
                onLoggedIn = { screen = AppScreen.Main },
            )
            AppScreen.Main -> MainContent(onLogout = {
                AuthState.logout()
                screen = AppScreen.Welcome
            })
        }
    }
}

@Composable
private fun MainContent(onLogout: () -> Unit = {}) {
    var settingsOpen by remember { mutableStateOf(false) }

    TabNavigator(HomeTab) { tabNavigator ->
        Scaffold(
            topBar = {
            val isTimetable = tabNavigator.current.options.index == TimetableTab.options.index
            val timetableLeading: (@Composable () -> Unit)? = if (isTimetable) {
                when (TimetableState.timetableMode) {
                    TimetableMode.Class -> { { CohortSwitcher() } }
                    TimetableMode.Room -> { { RoomSwitcher() } }
                    TimetableMode.Teacher -> { { TeacherSwitcher() } }
                }
            } else null
            TopBar(leadingContent = timetableLeading, onProfileClick = { settingsOpen = true })
        },
            bottomBar = { BottomNavigationBar(tabNavigator) },
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.systemBars,
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                CurrentTab()
            }
        }

        if (settingsOpen) {
            SettingsSheet(
                onDismiss = { settingsOpen = false },
                onLogout = { settingsOpen = false; onLogout() },
            )
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
