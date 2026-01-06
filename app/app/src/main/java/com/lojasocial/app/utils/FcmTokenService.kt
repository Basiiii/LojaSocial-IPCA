package com.lojasocial.app.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val TAG = "FcmTokenService"

    /**
     * Get FCM token and store it in Firestore for the current user
     */
    suspend fun getAndStoreToken(): String? {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.d(TAG, "No user logged in, skipping token storage")
                return null
            }

            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM Token retrieved: ${token.take(20)}...")
            Log.d(TAG, "Full FCM Token: $token") // Full token for debugging

            // Store token in Firestore under users/{userId}/fcmToken
            firestore.collection("users")
                .document(currentUser.uid)
                .update("fcmToken", token)
                .await()

            Log.d(TAG, "FCM Token stored successfully for user: ${currentUser.uid}")
            Log.d(TAG, "Token stored in Firestore at: users/${currentUser.uid}/fcmToken")
            token
        } catch (e: Exception) {
            Log.e(TAG, "Error getting/storing FCM token", e)
            null
        }
    }

    /**
     * Delete FCM token from Firestore (e.g., on logout)
     */
    suspend fun deleteToken() {
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                firestore.collection("users")
                    .document(currentUser.uid)
                    .update("fcmToken", com.google.firebase.firestore.FieldValue.delete())
                    .await()
                Log.d(TAG, "FCM Token deleted for user: ${currentUser.uid}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting FCM token", e)
        }
    }
}
