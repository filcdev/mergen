package hu.petrik.filcapp

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

@androidx.compose.runtime.Composable
actual fun appVersionLabel(): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    val info = rememberPackageInfo(context)

    @Suppress("DEPRECATION")
    val code = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    return "${info.versionName ?: "?"} ($code)"
}

@androidx.compose.runtime.Composable
private fun rememberPackageInfo(context: android.content.Context): android.content.pm.PackageInfo =
    androidx.compose.runtime.remember(context.packageName) {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
