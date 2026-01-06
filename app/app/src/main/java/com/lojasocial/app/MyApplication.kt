package com.lojasocial.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.lojasocial.app.utils.FcmTokenService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.exitProcess
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application() {
    
    @Inject
    lateinit var fcmTokenService: FcmTokenService

    companion object {
        const val CHANNEL_ID = "stock_warnings"
        const val CHANNEL_NAME = "Avisos de Stock"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Create notification channel
        createNotificationChannel()
        
        // Set global exception handler to catch silent crashes
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("GlobalExceptionHandler", "Uncaught exception in thread: ${thread.name}", exception)
            Log.e("GlobalExceptionHandler", "Exception message: ${exception.message}")
            Log.e("GlobalExceptionHandler", "Exception stack trace: ${exception.stackTraceToString()}")
            
            // Exit the app gracefully
            exitProcess(1)
        }

        // Initialize FCM token for logged-in users
        initializeFcmToken()
    }

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

    private fun initializeFcmToken() {
        // Check if user is logged in and get/store FCM token
        // Note: Hilt injection happens after onCreate, so we'll initialize token in MainActivity instead
        // This is kept here as a fallback but MainActivity handles the main initialization
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Use a delayed initialization to ensure Hilt is ready
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        fcmTokenService.getAndStoreToken()
                    }
                } catch (e: Exception) {
                    Log.e("MyApplication", "Error initializing FCM token", e)
                }
            }, 1000) // Delay 1 second to ensure Hilt is initialized
        }
    }
}
