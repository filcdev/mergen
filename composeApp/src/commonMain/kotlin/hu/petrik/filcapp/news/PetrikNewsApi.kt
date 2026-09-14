package hu.petrik.filcapp.news

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class PetrikNewsItem(
    val id: Int,
    val date: String,
    val link: String,
    val title: String,
    val excerpt: String,
)

@Serializable
private data class WordPressRendered(
    val rendered: String = "",
)

@Serializable
private data class WordPressPost(
    val id: Int,
    val date: String = "",
    val link: String = "",
    val title: WordPressRendered = WordPressRendered(),
    val excerpt: WordPressRendered = WordPressRendered(),
)

object PetrikNewsApi {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val client =
        HttpClient {
            expectSuccess = true
            install(ContentNegotiation) {
                json(PetrikNewsApi.json)
            }
        }

    suspend fun latest(limit: Int = 3): List<PetrikNewsItem> =
        client
            .get(POSTS_URL) {
                parameter("per_page", limit)
                parameter("orderby", "date")
                parameter("order", "desc")
                parameter("_fields", "id,date,link,title,excerpt")
            }.body<List<WordPressPost>>()
            .map { post ->
                PetrikNewsItem(
                    id = post.id,
                    date = post.date,
                    link = post.link,
                    title = sanitizeHtml(post.title.rendered),
                    excerpt = sanitizeHtml(post.excerpt.rendered).shortened(180),
                )
            }

    private const val POSTS_URL = "https://petrik.hu/wp-json/wp/v2/posts"
}

private fun sanitizeHtml(value: String): String {
    val withoutTags = HTML_TAG.replace(value, " ")
    val decoded =
        withoutTags
            .replace("&nbsp;", " ")
            .replace("&#160;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&hellip;", "…")
            .replace("&ndash;", "–")
            .replace("&mdash;", "—")

    val numericDecoded =
        NUMERIC_ENTITY.replace(decoded) { match ->
            val raw = match.groupValues[1]
            val code =
                if (raw.startsWith("x", ignoreCase = true)) {
                    raw.drop(1).toIntOrNull(16)
                } else {
                    raw.toIntOrNull()
                }

            if (code != null && code in 0..0xFFFF) {
                code.toChar().toString()
            } else {
                match.value
            }
        }

    return WHITESPACE.replace(numericDecoded, " ").trim()
}

private fun String.shortened(maxLength: Int): String {
    if (length <= maxLength) {
        return this
    }

    return take(maxLength).trimEnd().trimEnd('.', ',', ';', ':') + "…"
}

private val HTML_TAG = Regex("<[^>]+>")
private val NUMERIC_ENTITY = Regex("&#(x?[0-9A-Fa-f]+);")
private val WHITESPACE = Regex("\\s+")
