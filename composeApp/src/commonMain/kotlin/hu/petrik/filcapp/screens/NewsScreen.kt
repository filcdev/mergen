@file:OptIn(kotlin.time.ExperimentalTime::class)

package hu.petrik.filcapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import hu.petrik.filcapp.api.APIResult
import hu.petrik.filcapp.api.NewsBlogsApi
import hu.petrik.filcapp.api.client.APIClient
import hu.petrik.filcapp.models.BlogPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.*

// ── Tab ───────────────────────────────────────────────────────────────────────

object NewsTab : Tab {
    override val options: TabOptions
        @Composable get() {
            val icon = rememberVectorPainter(Icons.Default.Campaign)
            return remember { TabOptions(index = 3u, title = "News", icon = icon) }
        }

    @Composable
    override fun Content() {
        val model = rememberScreenModel { NewsScreenModel(NewsBlogsApi(APIClient)) }
        LaunchedEffect(Unit) { model.load() }

        var selectedPost by remember { mutableStateOf<BlogPost?>(null) }

        val post = selectedPost
        if (post != null) {
            NewsDetailScreen(post = post, onBack = { selectedPost = null })
        } else {
            NewsListScreen(
                isLoading = model.isLoading,
                posts = model.posts,
                error = model.error,
                onSelect = { selectedPost = it },
            )
        }
    }
}

// ── List screen ───────────────────────────────────────────────────────────────

@Composable
fun NewsListScreen(
    isLoading: Boolean,
    posts: List<BlogPost>,
    error: String?,
    onSelect: (BlogPost) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(posts, query) {
        if (query.isBlank()) posts
        else posts.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.author?.name?.contains(query, ignoreCase = true) == true
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search news…", style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
            )
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp).clickable { query = "" },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        when {
            isLoading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            else -> LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered, key = { it.id }) { post ->
                    NewsCard(post = post, onClick = { onSelect(post) })
                }
            }
        }
    }
}

@Composable
fun NewsCard(post: BlogPost, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Thumbnail placeholder
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 90.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("News pic", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Tags from title (extract #hashtags)
            val tags = extractTags(post.title)
            if (tags.isNotEmpty()) {
                Text(
                    text = tags.joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildAuthorLine(post),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Detail screen ─────────────────────────────────────────────────────────────

@Composable
fun NewsDetailScreen(post: BlogPost, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Detail News",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.size(48.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                // Hero image placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("News Pic", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(16.dp))

                // Author row + tags
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = post.author?.name ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    val tags = extractTags(post.title)
                    if (tags.isNotEmpty()) {
                        Text(
                            text = tags.joinToString(" ") { "#$it" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = relativeTime(post.publishedAt ?: post.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    post.content.forEach { block ->
                        val text = extractText(block)
                        if (text.isNotBlank()) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── ScreenModel ───────────────────────────────────────────────────────────────

class NewsScreenModel(private val api: NewsBlogsApi) : ScreenModel {
    var posts by mutableStateOf<List<BlogPost>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun load() {
        if (posts.isNotEmpty()) return
        screenModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) { isLoading = true; error = null }
            when (val result = api.getNewsBlogs()) {
                is APIResult.Success -> withContext(Dispatchers.Main) {
                    posts = result.data.sortedByDescending { it.publishedAt ?: it.createdAt }
                }
                is APIResult.Failure -> withContext(Dispatchers.Main) { error = result.error.toString() }
            }
            withContext(Dispatchers.Main) { isLoading = false }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun extractText(element: JsonElement): String = when (element) {
    is JsonPrimitive -> element.contentOrNull ?: ""
    is JsonObject -> {
        val direct = element["content"]
        if (direct != null) extractText(direct)
        else element.values.joinToString(" ") { extractText(it) }
    }
    is JsonArray -> element.joinToString("\n") { extractText(it) }
}

private fun extractTags(title: String): List<String> =
    Regex("""#(\w+)""").findAll(title).map { it.groupValues[1] }.toList()

private fun buildAuthorLine(post: BlogPost): String {
    val author = post.author?.name ?: return relativeTime(post.publishedAt ?: post.createdAt)
    return "by $author · ${relativeTime(post.publishedAt ?: post.createdAt)}"
}

private fun relativeTime(iso: String): String {
    return try {
        val then = Instant.parse(iso)
        val now = Clock.System.now()
        val totalSeconds = (now.epochSeconds - then.epochSeconds)
        val minutes = totalSeconds / 60
        val hours = totalSeconds / 3600
        val days = totalSeconds / 86400
        when {
            days >= 365 -> "${days / 365} year${if (days / 365 > 1L) "s" else ""} ago"
            days >= 30 -> "${days / 30} month${if (days / 30 > 1L) "s" else ""} ago"
            days >= 1 -> "$days day${if (days > 1L) "s" else ""} ago"
            hours >= 1 -> "$hours hour${if (hours > 1L) "s" else ""} ago"
            minutes >= 1 -> "$minutes minute${if (minutes > 1L) "s" else ""} ago"
            else -> "just now"
        }
    } catch (_: Exception) {
        iso.take(10)
    }
}
