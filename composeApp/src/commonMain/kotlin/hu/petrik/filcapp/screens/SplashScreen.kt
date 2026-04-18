package hu.petrik.filcapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.petrik.filcapp.auth.AuthState
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onAuthResolved: (loggedIn: Boolean) -> Unit) {
    LaunchedEffect(Unit) {
        AuthState.init()
        val loggedIn = if (AuthState.isLoggedIn) AuthState.fetchSession() else false
        delay(1200)
        onAuthResolved(loggedIn)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        // Logo — swap for actual drawable when assets are ready
        Text(
            text = "\uD83C\uDF3F",
            fontSize = 96.sp,
            color = Color(0xFFD0D0D0),
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Text(
                text = "Petrik App",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFB0B0B0),
                modifier = Modifier.padding(bottom = 48.dp),
            )
        }
    }
}
