package com.example.eventpay.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_prefs")

object NotificationRepository {

    private val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")

    suspend fun saveFcmToken(context: Context, token: String) {
        context.dataStore.edit { prefs -> prefs[FCM_TOKEN_KEY] = token }
    }

    suspend fun getSavedToken(context: Context): String? {
        return context.dataStore.data.firstOrNull()?.get(FCM_TOKEN_KEY)
    }

    suspend fun getFreshFcmToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun registerTokenForUser(userId: String, context: Context) {
        val token = getFreshFcmToken() ?: return
        saveFcmToken(context, token)
        try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .await()
        } catch (_: Exception) {
        }
    }

    suspend fun subscribeToEventTopic(eventId: String) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("event_$eventId").await()
        } catch (_: Exception) {
        }
    }

    suspend fun unsubscribeFromEventTopic(eventId: String) {
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("event_$eventId").await()
        } catch (_: Exception) {
        }
    }

    suspend fun subscribeToAdminTopic() {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("admin_broadcast").await()
        } catch (_: Exception) {
        }
    }
}
