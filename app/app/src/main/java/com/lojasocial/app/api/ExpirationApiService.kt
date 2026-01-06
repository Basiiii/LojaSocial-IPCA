package com.lojasocial.app.api

import retrofit2.Response
import retrofit2.http.POST

/**
 * Response data class for expiration check API.
 */
data class ExpirationCheckResponse(
    val success: Boolean,
    val message: String,
    val itemCount: Int,
    val notificationsSent: Int,
    val timestamp: String
)

/**
 * API service interface for expiration-related endpoints.
 * 
 * This service handles manual expiration checks for stock items.
 */
interface ExpirationApiService {
    /**
     * Triggers a manual check for expiring items and sends notifications.
     * 
     * This endpoint checks stock items for expiry dates and sends out
     * notifications for items that are expiring soon.
     * 
     * @return Response containing check results with item count and notifications sent
     */
    @POST("api/expiration/check-expiring")
    suspend fun checkExpiringItems(): Response<ExpirationCheckResponse>
}
