@file:OptIn(kotlin.time.ExperimentalTime::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package hu.petrik.filcapp.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hu.petrik.filcapp.models.Announcement
import hu.petrik.filcapp.models.SystemMessage
import kotlinx.datetime.Instant
import kotlinx.serialization.json.*
import kotlin.time.Clock

object NoticeState {
    var announcements by mutableStateOf<List<Announcement>>(emptyList())
    var systemMessages by mutableStateOf<List<SystemMessage>>(emptyList())
    var loaded by mutableStateOf(false)
    var sectionExpanded by mutableStateOf(true)
}

internal sealed class Notice {
    abstract val id: String
    abstract val title: String
    abstract val content: List<JsonElement>
    abstract val validFrom: String
    abstract val validUntil: String
    abstract val cohortIds: List<String>

    data class Ann(val data: Announcement) : Notice() {
        override val id = data.id
        override val title = data.title
        override val content = data.content
        override val validFrom = data.validFrom
        override val validUntil = data.validUntil
        override val cohortIds = data.cohortIds
    }

    data class Sys(val data: SystemMessage) : Notice() {
        override val id = data.id
        override val title = data.title
        override val content = data.content
        override val validFrom = data.validFrom
        override val validUntil = data.validUntil
        override val cohortIds = data.cohortIds
    }
}

private fun isCurrentlyValid(validFrom: String, validUntil: String): Boolean {
    return try {
        val from = Instant.parse(validFrom)
        val until = Instant.parse(validUntil)
        val now = Clock.System.now()
        now >= from && now <= until
    } catch (_: Exception) {
        true
    }
}

internal fun extractNoticeText(element: JsonElement): String = when (element) {
    is JsonPrimitive -> element.contentOrNull ?: ""
    is JsonObject -> {
        val direct = element["content"]
        if (direct != null) extractNoticeText(direct)
        else element.values.joinToString(" ") { extractNoticeText(it) }
    }
    is JsonArray -> element.joinToString("\n") { extractNoticeText(it) }
}

@Composable
fun NoticesBanner(cohortId: String?, modifier: Modifier = Modifier) {
    val notices = remember(NoticeState.announcements, NoticeState.systemMessages, cohortId) {
        val anns = NoticeState.announcements.map { Notice.Ann(it) }
        val sys = NoticeState.systemMessages.map { Notice.Sys(it) }
        (anns + sys).filter { n ->
            isCurrentlyValid(n.validFrom, n.validUntil) &&
            (n.cohortIds.isEmpty() || cohortId == null || n.cohortIds.contains(cohortId))
        }.sortedByDescending { it.validFrom }
    }

    if (notices.isEmpty()) return

    var detailNotice by remember { mutableStateOf<Notice?>(null) }
    val expanded = NoticeState.sectionExpanded

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { NoticeState.sectionExpanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Announcements",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = "${notices.size}",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                    thickness = 0.5.dp,
                )
                notices.forEachIndexed { index, notice ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            thickness = 0.5.dp,
                        )
                    }
                    NoticeRow(notice = notice, onClick = { detailNotice = notice })
                }
            }
        }
    }

    detailNotice?.let { notice ->
        NoticeDetailSheet(notice = notice, onDismiss = { detailNotice = null })
    }
}

@Composable
private fun NoticeRow(notice: Notice, onClick: () -> Unit) {
    val isSystem = notice is Notice.Sys
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isSystem) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "System message",
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = notice.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NoticeDetailSheet(notice: Notice, onDismiss: () -> Unit) {
    val bodyText = remember(notice.content) {
        notice.content.joinToString("\n\n") { extractNoticeText(it) }.trim()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (notice is Notice.Sys) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "System message",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = notice.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            HorizontalDivider(thickness = 0.5.dp)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            ) {
                if (bodyText.isNotBlank()) {
                    items(bodyText.split("\n\n")) { paragraph ->
                        if (paragraph.isNotBlank()) {
                            Text(
                                text = paragraph.trim(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "No content",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
