package hu.petrik.filcapp.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import hu.petrik.filcapp.MainActivity
import hu.petrik.filcapp.R

/** Foreground FCM messages need a local notification; background messages use FCM's system tray. */
class MergenFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        MobilePushRegistration.onTokenRefreshed(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        NotificationEvents.changed()
        val title = message.notification?.title ?: message.data["title"] ?: "FILC értesítés"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val id = message.messageId?.hashCode() ?: System.currentTimeMillis().toInt()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("type", "notification")
            putExtra("notificationType", message.data["notificationType"].orEmpty())
            putExtra("notificationId", message.data["notificationId"].orEmpty())
        }
        val pending = PendingIntent.getActivity(
            this,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification =
            NotificationCompat.Builder(this, MergenPushApplication.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_filc_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        NotificationEvents.changed()
    }
}
