package hu.petrik.filcapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import hu.petrik.filcapp.auth.Auth
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.auth.SchoolProfileState
import hu.petrik.filcapp.components.TopBar
import hu.petrik.filcapp.screens.HomeTab
import hu.petrik.filcapp.screens.LoadingScreen
import hu.petrik.filcapp.screens.LoginScreen
import hu.petrik.filcapp.screens.NewsTab
import hu.petrik.filcapp.screens.SettingsTab
import hu.petrik.filcapp.screens.SigningInScreen
import hu.petrik.filcapp.screens.SubstitutionTab
import hu.petrik.filcapp.screens.TimetableTab
import hu.petrik.filcapp.settings.AppSettings
import hu.petrik.filcapp.theme.FilcTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    FilcTheme {
        val state by Auth.state.collectAsState()
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            AppSettings.initialize()
            Auth.bootstrap()
        }

        LaunchedEffect(state) {
            val signedIn = state as? AuthState.SignedIn
            if (signedIn == null) {
                SchoolProfileState.clear()
            } else {
                SchoolProfileState.refresh(signedIn.user)
            }
        }

        when (val current = state) {
            AuthState.Loading -> LoadingScreen()
            is AuthState.SigningIn -> SigningInScreen(current.message)
            is AuthState.SignedOut ->
                LoginScreen(
                    state = current,
                    onSignIn = { scope.launch { Auth.signIn() } },
                )

            is AuthState.SignedIn -> MainScreen()
        }
    }
}

@Composable
private fun MainScreen() {
    TabNavigator(HomeTab) { tabNavigator ->
        Scaffold(
            topBar = { TopBar() },
            bottomBar = { FilcBottomNavigationBar(tabNavigator) },
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.systemBars,
        ) { paddingValues ->
            Box(
                modifier =
                    Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
            ) {
                CurrentTab()
            }
        }
    }
}

@Composable
private fun FilcBottomNavigationBar(tabNavigator: TabNavigator) {
    val tabs =
        listOf(
            HomeTab,
            TimetableTab,
            SubstitutionTab,
            NewsTab,
            SettingsTab,
        )

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = tabNavigator.current.options.index == tab.options.index

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .background(
                                    color =
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .clickable { tabNavigator.current = tab }
                                .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        tab.options.icon?.let { painter ->
                            Icon(
                                painter = painter,
                                contentDescription = tab.options.title,
                                tint =
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}
