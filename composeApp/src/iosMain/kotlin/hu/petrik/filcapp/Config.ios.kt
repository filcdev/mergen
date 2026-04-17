package hu.petrik.filcapp

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
actual val apiBaseUrl: String
    get() = if (Platform.isDebugBinary) "http://localhost:3000" else "https://filc.space"
