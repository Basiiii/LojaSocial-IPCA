package com.lojasocial.app

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlin.system.exitProcess

@HiltAndroidApp
class MyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Set global exception handler to catch silent crashes
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("GlobalExceptionHandler", "Uncaught exception in thread: ${thread.name}", exception)
            Log.e("GlobalExceptionHandler", "Exception message: ${exception.message}")
            Log.e("GlobalExceptionHandler", "Exception stack trace: ${exception.stackTraceToString()}")
            
            // Exit the app gracefully
            exitProcess(1)
        }
    }
}
