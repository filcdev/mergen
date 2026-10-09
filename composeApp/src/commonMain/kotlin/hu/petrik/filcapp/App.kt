package hu.petrik.filcapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.Tab
import hu.petrik.filcapp.auth.Auth
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.auth.SchoolProfileState
import hu.petrik.filcapp.components.TopBar
import hu.petrik.filcapp.screens.HomeTab
import hu.petrik.filcapp.screens.LoadingScreen
import hu.petrik.filcapp.screens.LoginScreen
import hu.petrik.filcapp.screens.NewsScreen
import hu.petrik.filcapp.screens.SettingsScreen
import hu.petrik.filcapp.screens.SigningInScreen
import hu.petrik.filcapp.screens.SubstitutionTab
import hu.petrik.filcapp.screens.TimetableTab
import hu.petrik.filcapp.settings.AppSettings
import hu.petrik.filcapp.settings.tr
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
    val tabs =
        remember {
            listOf<Tab>(
                HomeTab,
                TimetableTab,
                SubstitutionTab,
            )
        }

    val pagerState =
        rememberPagerState(
            initialPage = 0,
            pageCount = { tabs.size },
        )

    val scope = rememberCoroutineScope()
    var showMore by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showNews by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(
                onProfileClick = {
                    showMore = false
                    showNews = false
                    showSettings = true
                },
            )
        },
        bottomBar = {
            FigmaBottomNavigation(
                tabs = tabs,
                selectedPage = if (showSettings || showNews) -1 else pagerState.currentPage,
                moreSelected = showMore || showNews,
                onTabSelected = { page ->
                    showMore = false
                    showSettings = false
                    showNews = false
                    scope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                },
                onMore = {
                    showSettings = false
                    showNews = false
                    showMore = true
                },
            )
        },
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
    ) { paddingValues ->
        if (showSettings) {
            Box(
                modifier =
                    Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
            ) {
                SettingsScreen()
            }
        } else if (showNews) {
            Box(
                modifier =
                    Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
            ) {
                NewsScreen()
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
            ) { page ->
                tabs[page].Content()
            }
        }
    }

    if (showMore) {
        MoreFunctionsSheet(
            onDismiss = { showMore = false },
            onNews = {
                showMore = false
                showSettings = false
                showNews = true
            },
        )
    }
}

@Composable
private fun FigmaBottomNavigation(
    tabs: List<Tab>,
    selectedPage: Int,
    moreSelected: Boolean,
    onTabSelected: (Int) -> Unit,
    onMore: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                FigmaNavItem(
                    modifier = Modifier.weight(1f),
                    selected = selectedPage == index,
                    title = tab.options.title,
                    onClick = { onTabSelected(index) },
                    icon = {
                        tab.options.icon?.let { painter ->
                            Icon(
                                painter = painter,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                )
            }

            FigmaNavItem(
                modifier = Modifier.weight(1f),
                selected = moreSelected,
                title = tr("Továbbiak", "More"),
                onClick = onMore,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun FigmaNavItem(
    modifier: Modifier,
    selected: Boolean,
    title: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .height(56.dp)
                .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .background(
                        color =
                            if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        shape = RoundedCornerShape(999.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            ) {
                icon()
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreFunctionsSheet(
    onDismiss: () -> Unit,
    onNews: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(999.dp),
                        ),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = tr("További funkciók", "More features"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Surface(
                    modifier = Modifier.clickable(onClick = onDismiss),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(16.dp),
                    )
                }
            }

            HorizontalDivider()

            MoreMenuEntry(
                icon = Icons.Default.Campaign,
                title = tr("Hírek", "News"),
                subtitle =
                    tr(
                        "Aktuális közlemények és információk",
                        "Current announcements and information",
                    ),
                onClick = onNews,
            )

            MoreMenuEntry(
                icon = Icons.Default.CalendarMonth,
                title = tr("Eseménynapló", "Event log"),
                subtitle = tr("Iskolai élet, DÖK programok", "School life, student council events"),
                onClick = onDismiss,
            )

            MoreMenuEntry(
                icon = Icons.Default.Groups,
                title = "DÖK",
                subtitle =
                    tr(
                        "Képviselet, javaslatok, események",
                        "Representation, proposals, events",
                    ),
                onClick = onDismiss,
            )

            Box(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MoreMenuEntry(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
