package hu.petrik.filcapp.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hu.petrik.filcapp.api.client.NotificationsApi
import hu.petrik.filcapp.api.infrastructure.ApiClient
import hu.petrik.filcapp.api.infrastructure.HttpResponse
import hu.petrik.filcapp.api.model.PatchNotificationsSettingsRequest
import hu.petrik.filcapp.api.model.PatchNotificationsSettingsRequestNotificationPreferences
import hu.petrik.filcapp.api.model.PostNotificationsFcmTokensRequest
import hu.petrik.filcapp.auth.Auth

/** A single authenticated API client; no separate tokens or credentials are stored. */
object NotificationRepository {
    private val api by lazy { NotificationsApi(ApiClient.BASE_URL, Auth.httpClient) }

    private suspend fun <T : Any> HttpResponse<T>.requireBody(): T {
        check(success) { "Notification API error: $status" }
        return body()
    }

    suspend fun list(): List<NotificationEntry> =
        api.getNotifications(limit = 50.0).requireBody().items.mapNotNull { row ->
            val id = row.id ?: return@mapNotNull null
            NotificationEntry(
                id = id,
                title = row.title.orEmpty(),
                content = row.content.orEmpty(),
                type = row.type.orEmpty(),
                createdAt = row.createdAt.orEmpty(),
                read = row.read,
            )
        }

    suspend fun unreadCount(): Int =
        api.getNotificationsUnreadCount().requireBody().count?.toInt() ?: 0

    suspend fun markRead(id: String) {
        api.postNotificationsByIdRead(id).requireBody()
        NotificationEvents.changed()
    }

    suspend fun markAllRead() {
        api.postNotificationsReadAll().requireBody()
        NotificationEvents.changed()
    }

    suspend fun settings(): NotificationPreferences {
        val prefs = api.getNotificationsSettings().requireBody().notificationPreferences
        return NotificationPreferences(
            channelsEnabled = prefs.channelsEnabled,
            substitution = prefs.substitution,
            movedLesson = prefs.movedLesson,
            announcement = prefs.announcement,
            systemMessage = prefs.systemMessage,
            blogPost = prefs.blogPost,
            doorlockCardUsed = prefs.doorlockCardUsed,
        )
    }

    suspend fun saveSettings(prefs: NotificationPreferences) {
        api.patchNotificationsSettings(
            PatchNotificationsSettingsRequest(
                notificationPreferences = PatchNotificationsSettingsRequestNotificationPreferences(
                    channelsEnabled = prefs.channelsEnabled,
                    substitution = prefs.substitution,
                    movedLesson = prefs.movedLesson,
                    announcement = prefs.announcement,
                    systemMessage = prefs.systemMessage,
                    blogPost = prefs.blogPost,
                    doorlockCardUsed = prefs.doorlockCardUsed,
                ),
            ),
        ).requireBody()
        NotificationEvents.changed()
    }

    suspend fun registerToken(token: String) {
        api.postNotificationsFcmTokens(
            PostNotificationsFcmTokensRequest(
                token = token,
                deviceInfo = "Mergen Android",
            ),
        ).requireBody()
    }

    suspend fun unregisterToken(token: String) {
        api.deleteNotificationsFcmTokensByToken(token).requireBody()
    }
}

data class NotificationEntry(
    val id: String,
    val title: String,
    val content: String,
    val type: String,
    val createdAt: String,
    val read: Boolean,
)

data class NotificationPreferences(
    val channelsEnabled: Boolean = true,
    val substitution: Boolean = true,
    val movedLesson: Boolean = true,
    val announcement: Boolean = true,
    val systemMessage: Boolean = true,
    val blogPost: Boolean = false,
    val doorlockCardUsed: Boolean = false,
)

/** Platform push handlers and inbox actions invalidate the unread count. */
object NotificationEvents {
    var version by mutableIntStateOf(0)
        private set

    fun changed() {
        version++
    }
}

/** Navigation survives the login screen and also handles taps on running activities. */
object NotificationNavigation {
    var request by mutableStateOf<NotificationOpenRequest?>(null)
        private set

    private var nextId = 0

    fun open(
        type: String?,
        notificationId: String? = null,
    ) {
        nextId++
        request = NotificationOpenRequest(nextId, type.orEmpty(), notificationId)
        NotificationEvents.changed()
    }

    fun consume(serial: Int) {
        if (request?.serial == serial) {
            request = null
        }
    }
}

data class NotificationOpenRequest(
    val serial: Int,
    val type: String,
    val notificationId: String?,
)

internal fun opensSubstitutions(type: String): Boolean =
    type == "substitution" || type == "substitution_teacher" || type == "moved_lesson"
