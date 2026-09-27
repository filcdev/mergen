package hu.petrik.filcapp.theme

import android.app.Activity
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun ApplyPlatformSystemBars(darkTheme: Boolean) {
    val view = LocalView.current

    if (view.isInEditMode) {
        return
    }

    SideEffect {
        val activity = view.context as? Activity ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(activity.window, view)

        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        @Suppress("DEPRECATION")
        activity.window.statusBarColor = Color.rgb(11, 17, 32)

        @Suppress("DEPRECATION")
        activity.window.navigationBarColor = Color.rgb(11, 17, 32)

        activity.window.isNavigationBarContrastEnforced = false
    }
}
