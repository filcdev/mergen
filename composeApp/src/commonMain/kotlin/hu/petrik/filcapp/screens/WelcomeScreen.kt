package hu.petrik.filcapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hu.petrik.filcapp.apiBaseUrl
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.auth.AuthWebView
import kotlinx.coroutines.launch

private data class OnboardingPage(val title: String, val subtitle: String, val illustrationLabel: String)

private val pages = listOf(
    OnboardingPage(
        title = "Stay informed",
        subtitle = "Keep track of substitutions and timetables with the Petrik app.",
        illustrationLabel = "Illustration or lottie",
    ),
    OnboardingPage(
        title = "Follow Petrik News",
        subtitle = "Follow the most important events at Petrik and don't miss a thing.",
        illustrationLabel = "Other Illustration or lottie",
    ),
)

@Composable
fun WelcomeScreen(onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    var showWebView by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState { pages.size }

    if (showWebView) {
        AuthWebView(
            apiBaseUrl = apiBaseUrl,
            onSessionAcquired = { token ->
                AuthState.login(token)
                showWebView = false
                scope.launch {
                    AuthState.fetchSession()
                    onLoggedIn()
                }
            },
            onDismiss = { showWebView = false },
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val p = pages[page]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(p.illustrationLabel, color = Color(0xFF888888))
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    text = p.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = p.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            repeat(pages.size) { i ->
                val selected = pagerState.currentPage == i
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (selected) 24.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color(0xFF888888) else Color(0xFFD0D0D0)),
                )
            }
        }

        Button(
            onClick = { showWebView = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F0F0)),
            elevation = ButtonDefaults.buttonElevation(0.dp),
        ) {
            Box(modifier = Modifier.size(20.dp).background(Color(0xFFCCCCCC)))
            Spacer(Modifier.width(12.dp))
            Text("Sign in with Microsoft", color = Color(0xFF333333), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
