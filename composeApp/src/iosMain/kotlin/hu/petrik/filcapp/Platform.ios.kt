package hu.petrik.filcapp

import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

@androidx.compose.runtime.Composable
actual fun appVersionLabel(): String {
    val bundle = platform.Foundation.NSBundle.mainBundle
    val version = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "?"
    val build = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "?"
    return "$version ($build)"
}
