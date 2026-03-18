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

enum class NotificationType {
    EVENT_REMINDER,
    TICKET_PURCHASED,
    CHECK_IN_SUCCESS,
    EVENT_CANCELLED,
    EVENT_UPDATED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    GENERAL
}

object NotificationChannels {
    const val EVENT_REMINDERS = "event_reminders"
    const val TICKETS = "tickets"
    const val CHECK_IN = "check_in"
    const val PAYMENTS = "payments"
    const val GENERAL = "general"
}

object NotificationHelper {

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(
                    NotificationChannels.EVENT_REMINDERS,
                    "Event Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Reminders for upcoming events you have tickets for" },

                NotificationChannel(
                    NotificationChannels.TICKETS,
                    "Ticket Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Ticket purchase confirmations and updates" },

                NotificationChannel(
                    NotificationChannels.CHECK_IN,
                    "Check-In Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Real-time check-in status alerts for scanners" },

                NotificationChannel(
                    NotificationChannels.PAYMENTS,
                    "Payment Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Payment confirmations and failures" },

                NotificationChannel(
                    NotificationChannels.GENERAL,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "General app notifications and announcements" }
            )
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        type: NotificationType = NotificationType.GENERAL,
        eventId: String? = null
    ) {
        val channelId = when (type) {
            NotificationType.EVENT_REMINDER -> NotificationChannels.EVENT_REMINDERS
            NotificationType.TICKET_PURCHASED -> NotificationChannels.TICKETS
            NotificationType.CHECK_IN_SUCCESS -> NotificationChannels.CHECK_IN
            NotificationType.PAYMENT_SUCCESS, NotificationType.PAYMENT_FAILED -> NotificationChannels.PAYMENTS
            else -> NotificationChannels.GENERAL
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            eventId?.let { putExtra("eventId", it) }
            putExtra("notificationType", type.name)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                when (type) {
                    NotificationType.EVENT_REMINDER,
                    NotificationType.PAYMENT_FAILED,
                    NotificationType.CHECK_IN_SUCCESS -> NotificationCompat.PRIORITY_HIGH
                    else -> NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
        }
    }

    fun showEventReminderNotification(
        context: Context,
        eventName: String,
        eventDate: String,
        eventId: String
    ) {
        showNotification(
            context = context,
            title = "⏰ Event Reminder: $eventName",
            body = "Your event starts on $eventDate. Don't forget to bring your QR ticket!",
            type = NotificationType.EVENT_REMINDER,
            eventId = eventId
        )
    }

    fun showTicketPurchasedNotification(
        context: Context,
        eventName: String,
        ticketId: String,
        eventId: String
    ) {
        showNotification(
            context = context,
            title = "🎟️ Ticket Confirmed!",
            body = "Your ticket for $eventName has been confirmed. Ticket ID: ${ticketId.take(8).uppercase()}",
            type = NotificationType.TICKET_PURCHASED,
            eventId = eventId
        )
    }

    fun showPaymentSuccessNotification(
        context: Context,
        amount: String,
        eventName: String,
        eventId: String
    ) {
        showNotification(
            context = context,
            title = "✅ Payment Successful",
            body = "Payment of $amount for $eventName was processed successfully.",
            type = NotificationType.PAYMENT_SUCCESS,
            eventId = eventId
        )
    }

    fun showPaymentFailedNotification(
        context: Context,
        amount: String,
        reason: String
    ) {
        showNotification(
            context = context,
            title = "❌ Payment Failed",
            body = "Payment of $amount failed: $reason. Please try again.",
            type = NotificationType.PAYMENT_FAILED
        )
    }
}
