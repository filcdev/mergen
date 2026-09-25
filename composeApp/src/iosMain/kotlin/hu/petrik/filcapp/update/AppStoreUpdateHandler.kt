package hu.petrik.filcapp.update

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import hu.petrik.filcapp.settings.tr
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

private data class AppStoreUpdate(
    val version: String,
    val storeUrl: String,
)

private object AppStoreUpdateApi {
    private val client =
        HttpClient(Darwin) {
            expectSuccess = true
        }

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    suspend fun checkForUpdate(): AppStoreUpdate? =
        runCatching {
            val bundleId = NSBundle.mainBundle.bundleIdentifier ?: return null
            val currentVersion =
                NSBundle.mainBundle
                    .objectForInfoDictionaryKey("CFBundleShortVersionString")
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: return null

            val payload =
                client
                    .get(
                        "https://itunes.apple.com/lookup" +
                            "?bundleId=$bundleId&country=hu",
                    ) {
                        header(HttpHeaders.CacheControl, "no-cache")
                    }.bodyAsText()

            val firstResult =
                json
                    .parseToJsonElement(payload)
                    .jsonObject["results"]
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
                    ?: return null

            val latestVersion =
                firstResult["version"]
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?.takeIf { it.isNotBlank() }
                    ?: return null

            val storeUrl =
                firstResult["trackViewUrl"]
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?.takeIf { it.isNotBlank() }
                    ?: return null

            if (isNewerVersion(latestVersion, currentVersion)) {
                AppStoreUpdate(
                    version = latestVersion,
                    storeUrl = storeUrl,
                )
            } else {
                null
            }
        }.getOrNull()

    private fun isNewerVersion(
        latest: String,
        current: String,
    ): Boolean {
        val latestParts = versionParts(latest)
        val currentParts = versionParts(current)
        val maxSize = maxOf(latestParts.size, currentParts.size)

        repeat(maxSize) { index ->
            val latestPart = latestParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }

            if (latestPart != currentPart) {
                return latestPart > currentPart
            }
        }

        return false
    }

    private fun versionParts(version: String): List<Int> =
        version
            .split('.')
            .map { part ->
                part
                    .takeWhile { it.isDigit() }
                    .toIntOrNull()
                    ?: 0
            }
}

@Composable
fun AppStoreUpdateHandler() {
    var update by remember { mutableStateOf<AppStoreUpdate?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        update = AppStoreUpdateApi.checkForUpdate()
        showUpdateDialog = update != null
    }

    if (showUpdateDialog) {
        val availableUpdate = update ?: return

        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = {
                Text(tr("Új Mergen verzió érhető el", "A new Mergen version is available"))
            },
            text = {
                Text(
                    tr(
                        "Az App Store-ban elérhető a Mergen ${availableUpdate.version} verziója. " +
                            "Frissíthetsz most, vagy folytathatod a jelenlegi verzió használatát.",
                        "Mergen ${availableUpdate.version} is available on the App Store. " +
                            "You can update now or continue using the current version.",
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUpdateDialog = false
                        openAppStore(availableUpdate.storeUrl)
                    },
                ) {
                    Text(tr("Frissítés", "Update"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text(tr("Később", "Later"))
                }
            },
        )
    }
}

private fun openAppStore(url: String) {
    val storeUrl = NSURL(string = url) ?: return
    UIApplication.sharedApplication.openURL(storeUrl)
}
