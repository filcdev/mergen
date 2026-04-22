@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package hu.petrik.filcapp.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.petrik.filcapp.AppSettings
import hu.petrik.filcapp.api.APIResult
import hu.petrik.filcapp.api.ApiErrorMessage
import hu.petrik.filcapp.api.DoorlockApi
import hu.petrik.filcapp.api.client.APIClient
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.auth.base64ToImageBitmap
import hu.petrik.filcapp.hslColor
import hu.petrik.filcapp.models.Card
import hu.petrik.filcapp.models.Card_authorizedDevices
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
private data class ActivateCardRequest(val deviceId: String? = null)

// ── Settings sheet (opens when profile avatar is tapped) ──────────────────────

@Composable
fun SettingsSheet(onDismiss: () -> Unit, onLogout: () -> Unit) {
    val displayName = AuthState.displayName ?: ""
    val profileImage = AuthState.profileImage
    val avatar: ImageBitmap? = remember(profileImage) {
        profileImage?.let { base64ToImageBitmap(it) }
    }
    var cardsOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Profile header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (avatar != null) {
                        Image(
                            bitmap = avatar,
                            contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = displayName.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
                Column {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Petrik Lajos SZKI",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider()

            // Settings sections
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Account section
                SettingsGroup(title = "Account") {
                    SettingsRow(
                        icon = Icons.Default.CreditCard,
                        label = "Cards",
                        onClick = { cardsOpen = true },
                    )
                }

                // Appearance section
                AppearanceSection()
            }

            HorizontalDivider()

            // Logout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "Log out",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (cardsOpen) {
        CardsSheet(onDismiss = { cardsOpen = false })
    }
}

// ── Settings helpers ──────────────────────────────────────────────────────────

@Composable
private fun SettingsGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                fontWeight = FontWeight.Medium,
            )
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    label: String,
    onClick: () -> Unit = {},
    showChevron: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(iconTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            trailing()
        } else if (showChevron) {
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Appearance section ────────────────────────────────────────────────────────

@Composable
private fun AppearanceSection() {
    var pickerExpanded by remember { mutableStateOf(false) }
    var localHue by remember { mutableFloatStateOf(AppSettings.accentHue) }

    SettingsGroup(title = "Appearance") {
        SettingsRow(
            icon = Icons.Default.Palette,
            label = "Accent Color",
            onClick = { pickerExpanded = !pickerExpanded },
            showChevron = false,
            trailing = {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(hslColor(AppSettings.accentHue, 0.65f, 0.38f)),
                )
            },
        )

        if (pickerExpanded) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                HueSlider(
                    hue = localHue,
                    onHueChange = { hue ->
                        localHue = hue
                        AppSettings.accentHue = hue
                    },
                )

                // Preview row of hue stops
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val presets = listOf(0f, 30f, 60f, 120f, 180f, 220f, 260f, 300f)
                    presets.forEach { preset ->
                        val isSelected = (localHue - preset) in -10f..10f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(hslColor(preset, 0.65f, 0.38f))
                                .clickable {
                                    localHue = preset
                                    AppSettings.accentHue = preset
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HueSlider(hue: Float, onHueChange: (Float) -> Unit) {
    val trackHeight = 36.dp
    val thumbRadius = 16.dp
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val thumbRadiusPx = with(density) { thumbRadius.toPx() }

        val hueGradient = remember {
            Brush.horizontalGradient(
                colors = listOf(
                    Color.Red,
                    Color(1f, 1f, 0f),
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color(1f, 0f, 1f),
                    Color.Red,
                ),
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(50))
                .pointerInput(widthPx) {
                    detectTapGestures { offset ->
                        onHueChange((offset.x / widthPx * 360f).coerceIn(0f, 360f))
                    }
                }
                .pointerInput(widthPx) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onHueChange((offset.x / widthPx * 360f).coerceIn(0f, 360f))
                        },
                        onDrag = { change, _ ->
                            onHueChange((change.position.x / widthPx * 360f).coerceIn(0f, 360f))
                        },
                    )
                },
        ) {
            drawRect(brush = hueGradient)

            val thumbX = (hue / 360f) * size.width
            val centerY = size.height / 2f

            drawCircle(
                color = Color.White,
                radius = thumbRadiusPx,
                center = Offset(thumbX, centerY),
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = thumbRadiusPx,
                center = Offset(thumbX, centerY),
                style = Stroke(width = 2f),
            )
        }
    }
}

// ── Cards sheet ───────────────────────────────────────────────────────────────

@Composable
private fun CardsSheet(onDismiss: () -> Unit) {
    val doorlockApi = remember { DoorlockApi(APIClient) }
    val scope = rememberCoroutineScope()
    var cards by remember { mutableStateOf<List<Card>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var devicePickerCard by remember { mutableStateOf<Card?>(null) }
    var activatingCardId by remember { mutableStateOf<String?>(null) }
    var resultDialog by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    LaunchedEffect(Unit) {
        when (val result = doorlockApi.getDoorlockSelfCards()) {
            is APIResult.Success -> { cards = result.data.cards; isLoading = false }
            is APIResult.Failure -> { error = result.error.toString(); isLoading = false }
        }
    }

    suspend fun activateCard(cardId: String, deviceId: String?) {
        activatingCardId = cardId
        try {
            val response = APIClient.post {
                url("/doorlock/self/cards/$cardId/activate")
                contentType(ContentType.Application.Json)
                setBody(ActivateCardRequest(deviceId = deviceId))
            }
            if (response.status.isSuccess()) {
                resultDialog = true to "Door opened!"
            } else {
                val err = response.body<ApiErrorMessage>()
                resultDialog = false to err.message
            }
        } catch (e: Exception) {
            resultDialog = false to (e.message ?: "Unknown error")
        } finally {
            activatingCardId = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Cards", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                error != null -> Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(error!!, color = MaterialTheme.colorScheme.error) }

                cards.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { Text("No cards found", color = MaterialTheme.colorScheme.onSurfaceVariant) }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    items(cards, key = { it.id }) { card ->
                        CardRow(
                            card = card,
                            isActivating = activatingCardId == card.id,
                            onUse = {
                                val devices = card.authorizedDevices
                                when {
                                    devices.isEmpty() -> resultDialog = false to "Card is not authorized on any devices"
                                    devices.size == 1 -> scope.launch { activateCard(card.id, devices[0].id) }
                                    else -> devicePickerCard = card
                                }
                            },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    // Device picker dialog
    devicePickerCard?.let { card ->
        DevicePickerDialog(
            devices = card.authorizedDevices,
            onDismiss = { devicePickerCard = null },
            onSelect = { device ->
                devicePickerCard = null
                scope.launch { activateCard(card.id, device.id) }
            },
        )
    }

    // Result dialog
    resultDialog?.let { (success, message) ->
        AlertDialog(
            onDismissRequest = { resultDialog = null },
            title = { Text(if (success) "Success" else "Error") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { resultDialog = null }) { Text("OK") }
            },
            containerColor = if (success)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer,
        )
    }
}

// ── Card row ──────────────────────────────────────────────────────────────────

@Composable
private fun CardRow(
    card: Card,
    isActivating: Boolean,
    onUse: () -> Unit,
) {
    val canUse = card.enabled && !card.frozen

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (canUse) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!card.enabled) StatusChip(label = "Disabled", color = MaterialTheme.colorScheme.error)
                if (card.frozen) StatusChip(label = "Frozen", color = Color(0xFF2196F3))
                if (canUse) StatusChip(label = "Active", color = Color(0xFF4CAF50))
            }
            if (card.authorizedDevices.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = card.authorizedDevices.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        if (isActivating) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Button(
                onClick = onUse,
                enabled = canUse,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Use")
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Device picker dialog ──────────────────────────────────────────────────────

@Composable
private fun DevicePickerDialog(
    devices: List<Card_authorizedDevices>,
    onDismiss: () -> Unit,
    onSelect: (Card_authorizedDevices) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                devices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(device) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(device.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
