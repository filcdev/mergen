package hu.petrik.filcapp.admin

import hu.petrik.filcapp.api.infrastructure.ApiClient
import hu.petrik.filcapp.auth.Auth
import hu.petrik.filcapp.data.CohortDto
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Chronos is authoritative for permission checks, targeting and notifications. */
enum class NewsAdminType(val endpoint: String) {
    ANNOUNCEMENT("news/announcements"),
    SYSTEM_MESSAGE("news/system-messages"),
}

@Serializable
data class NewsAdminItem(
    val id: String,
    val title: String? = null,
    val content: JsonElement = JsonNull,
    val validFrom: String,
    val validUntil: String,
    val cohortIds: List<String> = emptyList(),
    val kioskIds: List<String> = emptyList(),
    val highlighted: Boolean = false,
    val kioskOnly: Boolean = false,
    val imageKey: String? = null,
    val imageUpdatedAt: String? = null,
)

@Serializable
private data class NewsAdminPage(val items: List<NewsAdminItem> = emptyList())

@Serializable
data class NewsAdminKiosk(val id: String, val name: String, val kind: String)

@Serializable
private data class NewsAdminKioskPage(val items: List<NewsAdminKiosk> = emptyList())

/** Keep legacy JSON or multi-block content intact unless the administrator edits it. */
fun plainNewsText(content: JsonElement): String =
    when (content) {
        is JsonPrimitive -> content.contentOrNull.orEmpty()
        is JsonArray ->
            content.mapNotNull { block ->
                (block as? JsonObject)?.get("content")?.let { value ->
                    (value as? JsonPrimitive)?.contentOrNull
                }
            }.joinToString("\n")
        else -> ""
    }

fun plainNewsBlock(text: String): JsonElement =
    buildJsonArray {
        add(
            buildJsonObject {
                put("type", "text")
                put("content", text)
            },
        )
    }

/** The web frontend uses the school's local day boundaries. */
@OptIn(ExperimentalTime::class)
fun newsDateBoundary(
    day: String,
    endOfDay: Boolean,
): String {
    val parsed = LocalDate.parse(day)
    require(parsed.toString() == day) { "Expected YYYY-MM-DD" }
    val zone = TimeZone.of("Europe/Budapest")
    return if (endOfDay) {
        LocalDate.fromEpochDays(parsed.toEpochDays() + 1).atStartOfDayIn(zone).minus(1.milliseconds).toString()
    } else {
        parsed.atStartOfDayIn(zone).toString()
    }
}

@OptIn(ExperimentalTime::class)
fun newsLocalDay(iso: String): String =
    runCatching {
        Instant.parse(iso).toLocalDateTime(TimeZone.of("Europe/Budapest")).date.toString()
    }.getOrElse { iso.take(10) }

/** File content check avoids accidentally uploading HEIC or spoofed extensions. */
fun supportedNewsImageMime(bytes: ByteArray): String? =
    when {
        bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte() -> "image/jpeg"
        bytes.size >= 8 &&
            bytes.sliceArray(0..7).contentEquals(
                byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a),
            )
        -> "image/png"
        bytes.size >= 12 && bytes.decodeToString(0, 4) == "RIFF" && bytes.decodeToString(8, 12) == "WEBP" -> "image/webp"
        else -> null
    }

object NewsAdminRepository {
    private val http get() = Auth.httpClient
    private val url get() = ApiClient.BASE_URL.trimEnd('/')
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private suspend fun HttpResponse.checkedText(): String {
        val result = bodyAsText()
        if (!status.isSuccess()) throw IllegalStateException("HTTP ${status.value}: ${result.take(350)}")
        return result
    }

    suspend fun list(kind: NewsAdminType): List<NewsAdminItem> {
        val response =
            http.get("$url/${kind.endpoint}") {
                url {
                    parameters.append("limit", "100")
                    parameters.append("offset", "0")
                    parameters.append("includeExpired", "true")
                    if (kind == NewsAdminType.ANNOUNCEMENT) {
                        parameters.append("includeAll", "true")
                        parameters.append("includeKioskOnly", "true")
                    }
                }
            }
        return json.decodeFromString<NewsAdminPage>(response.checkedText()).items
    }

    suspend fun cohorts(): List<CohortDto> {
        val response = http.get("$url/cohort")
        val raw = json.parseToJsonElement(response.checkedText()) as JsonObject
        val items = raw["items"] as? JsonArray ?: return emptyList()
        return items.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = (obj["id"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            CohortDto(
                id,
                (obj["name"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                (obj["short"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            )
        }.sortedBy { it.name }
    }

    suspend fun tvKiosks(): List<NewsAdminKiosk> {
        val response = http.get("$url/kiosk")
        return json.decodeFromString<NewsAdminKioskPage>(response.checkedText()).items.filter { it.kind == "tv" }
    }

    suspend fun save(
        kind: NewsAdminType,
        existingId: String?,
        title: String,
        content: JsonElement,
        validFrom: String,
        validUntil: String,
        cohortIds: List<String>,
        highlighted: Boolean = false,
        kioskOnly: Boolean = false,
        kioskIds: List<String> = emptyList(),
    ): NewsAdminItem {
        require(kind != NewsAdminType.SYSTEM_MESSAGE || title.isNotBlank()) { "A rendszerüzenet címe kötelező." }
        require(!highlighted || title.isNotBlank()) { "Kiemelt közleményhez cím szükséges." }
        require(validUntil >= validFrom) { "A záródátum nem lehet korábbi." }
        val payload =
            buildJsonObject {
                put("validFrom", newsDateBoundary(validFrom, false))
                put("validUntil", newsDateBoundary(validUntil, true))
                put("content", content)
                put("cohortIds", buildJsonArray { cohortIds.distinct().forEach { add(JsonPrimitive(it)) } })
                if (title.isNotBlank() || kind == NewsAdminType.SYSTEM_MESSAGE) put("title", title.trim())
                if (kind == NewsAdminType.ANNOUNCEMENT) {
                    put("highlighted", highlighted)
                    put("kioskOnly", kioskOnly)
                    put("kioskIds", buildJsonArray { kioskIds.distinct().forEach { add(JsonPrimitive(it)) } })
                }
            }
        val response =
            if (existingId == null) {
                http.post("$url/${kind.endpoint}") {
                    contentType(ContentType.Application.Json)
                    setBody(payload.toString())
                }
            } else {
                http.patch("$url/${kind.endpoint}/$existingId") {
                    contentType(ContentType.Application.Json)
                    setBody(payload.toString())
                }
            }
        return json.decodeFromString<NewsAdminItem>(response.checkedText())
    }

    suspend fun delete(
        kind: NewsAdminType,
        id: String,
    ) {
        http.delete("$url/${kind.endpoint}/$id").checkedText()
    }

    /** Upload is a separate operation after creation, exactly as in FILC web. */
    suspend fun uploadImage(
        id: String,
        filename: String,
        bytes: ByteArray,
    ) {
        require(bytes.size <= 8 * 1024 * 1024) { "A kép maximális mérete 8 MiB." }
        val mime = supportedNewsImageMime(bytes) ?: error("Csak JPG, PNG vagy WebP kép tölthető fel.")
        val safeName = filename.filter { it.isLetterOrDigit() || it in ".-_" }.ifBlank { "announcement-image" }
        val multipart =
            MultiPartFormDataContent(
                formData {
                    append(
                        "file",
                        bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, mime)
                            append(HttpHeaders.ContentDisposition, "filename=\"$safeName\"")
                        },
                    )
                },
            )
        http.post("$url/news/announcements/$id/image") { setBody(multipart) }.checkedText()
    }

    suspend fun deleteImage(id: String) {
        http.delete("$url/news/announcements/$id/image").checkedText()
    }
}
