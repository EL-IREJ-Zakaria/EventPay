package com.example.eventpay.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.eventpay.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EventPayMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            NotificationRepository.saveFcmToken(applicationContext, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "EventPay"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val type = message.data["type"] ?: NotificationType.GENERAL.name
        val eventId = message.data["eventId"]

        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            type = NotificationType.valueOf(type.uppercase().let {
                runCatching { NotificationType.valueOf(it) }.getOrDefault(NotificationType.GENERAL).name
            }),
            eventId = eventId
        )
    }
}
