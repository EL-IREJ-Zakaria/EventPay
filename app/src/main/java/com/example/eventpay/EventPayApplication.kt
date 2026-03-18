package com.example.eventpay

import android.app.Application
import com.example.eventpay.notification.NotificationHelper

class EventPayApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }
}
