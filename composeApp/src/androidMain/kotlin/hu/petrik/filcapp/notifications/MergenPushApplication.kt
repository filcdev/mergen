package hu.petrik.filcapp.notifications

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import hu.petrik.filcapp.R
import hu.petrik.filcapp.auth.AndroidAuthContext

class MergenPushApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAuthContext.initialize(this)
        initializeFirebase()
        createChannel()
    }

    private fun initializeFirebase() {
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            return
        }
        val appId = getString(R.string.mergen_fcm_app_id)
        val senderId = getString(R.string.mergen_fcm_sender_id)
        val apiKey = getString(R.string.mergen_fcm_api_key)
        val projectId = getString(R.string.mergen_fcm_project_id)
        if (listOf(appId, senderId, apiKey, projectId).any { it.isBlank() }) {
            return
        }

        val options =
            FirebaseOptions.Builder()
                .setApplicationId(appId)
                .setGcmSenderId(senderId)
                .setApiKey(apiKey)
                .setProjectId(projectId)
                .build()
        FirebaseApp.initializeApp(this, options)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = getSystemService(NotificationManager::class.java)
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "FILC értesítések",
                NotificationManager.IMPORTANCE_HIGH,
            )
        channel.description = "Helyettesítések és iskolai értesítések"
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "filc_notifications"

        fun isConfigured(context: android.content.Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()
    }
}
