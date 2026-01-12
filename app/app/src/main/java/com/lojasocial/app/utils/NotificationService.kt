package com.lojasocial.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lojasocial.app.MainActivity
import com.lojasocial.app.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Service responsible for handling Firebase Cloud Messaging (FCM) notifications.
 * 
 * This service extends [FirebaseMessagingService] to receive and process push notifications
 * from Firebase. It handles notifications about items approaching their expiration date,
 * displays them to the user, and manages FCM token updates.
 * 
 * Key features:
 * - Receives and processes FCM messages when the app is in the foreground
 * - Creates and displays notifications with deep linking support
 * - Manages notification channels for Android O and above
 * - Handles FCM token refresh and updates user tokens in Firestore
 * 
 * @see FirebaseMessagingService
 */
@AndroidEntryPoint
class NotificationService : FirebaseMessagingService() {
    /** Tag used for logging purposes */
    private val TAG = "NotificationService"
    
    /** Unique identifier for the notification channel */
    private val CHANNEL_ID = "stock_warnings"
    
    /** Display name for the notification channel */
    private val CHANNEL_NAME = "Avisos de Stock"

    /**
     * Called when a message is received from Firebase Cloud Messaging.
     * 
     * This method is invoked when:
     * - The app is in the foreground (FCM doesn't automatically show notifications)
     * - A data-only message is received (no notification payload)
     * 
     * When the app is in the background, FCM automatically displays notifications
     * if the message contains a notification payload.
     * 
     * The method extracts the notification title and body from either the notification
     * payload or the data payload, with fallback values if neither is present.
     * 
     * @param remoteMessage The message received from FCM containing notification data
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "=== FCM MESSAGE RECEIVED ===")
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "Data: ${remoteMessage.data}")
        Log.d(TAG, "Notification: ${remoteMessage.notification}")

        // When app is in foreground, we handle the notification manually
        // When app is in background, FCM automatically shows notification if it has notification payload
        
        // Extract title: prefer notification payload, then data payload, then default
        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "Aviso de Validade"
        
        // Extract body: prefer notification payload, then data payload, then generate from itemCount, then default
        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: remoteMessage.data["itemCount"]?.let { count ->
                // Generate plural/singular message based on item count
                "$count ${if (count.toIntOrNull() == 1) "item está" else "itens estão"} próximo${if (count.toIntOrNull() == 1) "" else "s"} do prazo de validade"
            } 
            ?: "Itens próximos do prazo de validade"

        // Always show notification when message is received (app is in foreground)
        showNotification(title, body, remoteMessage.data)
    }

    /**
     * Creates and displays a notification to the user.
     * 
     * This method:
     * - Ensures the notification channel exists
     * - Creates an intent for deep linking to the appropriate screen
     * - Builds a high-priority notification with the provided title and body
     * - Checks if notifications are enabled before displaying
     * - Uses a unique notification ID based on the current timestamp
     * 
     * The notification includes deep linking data that allows the app to navigate
     * to a specific screen (e.g., expiring items screen) when tapped.
     * 
     * @param title The title text to display in the notification
     * @param body The body text to display in the notification
     * @param data Additional data from the FCM message, used for deep linking
     */
    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        Log.d(TAG, "Showing notification: title=$title, body=$body")
        createNotificationChannel()

        // Create intent for deep linking to MainActivity with screen navigation data
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add data to intent for deep linking
            val screen = data["screen"] ?: "expiringItems"
            putExtra("screen", screen)
            putExtra("type", data["type"] ?: "")
            
            Log.d(TAG, "Creating intent with screen: $screen, data: $data")
            
            // Add requestId or applicationId based on notification type
            when (screen) {
                "applicationDetail" -> {
                    val appId = data["applicationId"]
                    Log.d(TAG, "ApplicationDetail notification - applicationId: $appId")
                    appId?.let { 
                        putExtra("applicationId", it)
                        Log.d(TAG, "Added applicationId to intent: $it")
                    } ?: Log.w(TAG, "No applicationId found in notification data")
                }
                "requestDetails" -> {
                    val reqId = data["requestId"]
                    Log.d(TAG, "RequestDetails notification - requestId: $reqId")
                    reqId?.let { 
                        putExtra("requestId", it)
                        Log.d(TAG, "Added requestId to intent: $it")
                    } ?: Log.w(TAG, "No requestId found in notification data")
                }
                "expiringItems" -> {
                    data["itemCount"]?.let { putExtra("itemCount", it) }
                }
            }
        }

        // Create pending intent that will be triggered when notification is tapped
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build the notification with high priority and auto-cancel enabled
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using system icon
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)) // Allow expanded text view
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for important alerts
            .setContentIntent(pendingIntent) // Action when notification is tapped
            .setAutoCancel(true) // Automatically dismiss when tapped
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Check if notifications are enabled for this app
        if (!notificationManager.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications are disabled for this app")
            return
        }

        // Use timestamp as notification ID to ensure uniqueness
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "Notification displayed with ID: $notificationId")
    }

    /**
     * Creates a notification channel for Android O (API 26) and above.
     * 
     * Notification channels are required on Android O+ to categorize notifications
     * and allow users to control notification settings per channel. This channel
     * is configured with high importance to ensure notifications are displayed prominently.
     * 
     * This method is safe to call multiple times - creating a channel that already
     * exists will not cause an error.
     * 
     * On Android versions below O, this method does nothing as channels are not required.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações sobre itens próximos do prazo de validade"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Called when a new FCM registration token is generated.
     * 
     * FCM tokens can be refreshed in the following scenarios:
     * - App is restored on a new device
     * - App is uninstalled and reinstalled
     * - App data is cleared
     * - Token rotation (periodic refresh by FCM)
     * 
     * This method updates the user's FCM token in Firestore so that the server
     * can send push notifications to the correct device. The token is stored
     * in the user's document in the "users" collection.
     * 
     * If no user is currently authenticated, the token update is skipped.
     * 
     * @param token The new FCM registration token that should be used for sending messages
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed FCM token: ${token.take(20)}...")
        
        // Store the new token in Firestore for the currently authenticated user
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.let { user ->
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "New FCM token stored successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error storing new FCM token", e)
                }
        }
    }
}
