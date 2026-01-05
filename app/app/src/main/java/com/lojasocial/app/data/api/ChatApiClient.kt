package com.lojasocial.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton object that provides the Chat API service client.
 * 
 * This object configures and provides a Retrofit instance for making HTTP requests
 * to the Loja Social chat API. It handles:
 * - Base URL configuration
 * - HTTP client setup with timeouts
 * - Request/response logging for debugging
 * - JSON serialization/deserialization
 * 
 * The API client is configured to communicate with:
 * Base URL: https://lojasocial-ipca.onrender.com/
 * Endpoint: /api/chat
 * Authentication: Bearer token (lojasocial2025)
 */
object ChatApiClient {
    /**
     * The base URL for the Loja Social chat API.
     */
    private const val BASE_URL = "https://lojasocial-ipca.onrender.com/"
    
    /**
     * HTTP logging interceptor for debugging API requests and responses.
     * 
     * Logs the full request and response bodies at BODY level,
     * which is useful for development and troubleshooting.
     * Should be disabled or set to NONE in production builds.
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    /**
     * Configured OkHttpClient for making HTTP requests.
     * 
     * Features:
     * - Request/response logging for debugging
     * - 30-second connection timeout
     * - 30-second read timeout
     * - 30-second write timeout
     */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * Retrofit instance configured for the chat API.
     * 
     * Configuration:
     * - Base URL set to Loja Social API endpoint
     * - Custom OkHttpClient with logging and timeouts
     * - Gson converter for JSON serialization/deserialization
     */
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    /**
     * The Chat API service interface implementation.
     * 
     * This property provides access to the ChatApiService methods
     * for making API calls to the chat endpoint.
     * 
     * Usage:
     * ```kotlin
     * val response = ChatApiClient.apiService.sendMessage(
     *     authorization = "Bearer lojasocial2025",
     *     contentType = "application/json",
     *     request = chatRequest
     * )
     * ```
     */
    val apiService: ChatApiService = retrofit.create(ChatApiService::class.java)
}
