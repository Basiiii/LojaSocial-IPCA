package com.lojasocial.app.repository

import android.util.Log
import com.lojasocial.app.api.ExpirationApiService
import com.lojasocial.app.api.ExpirationCheckResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling expiration-related API calls.
 * 
 * This repository provides methods to manually trigger expiration checks
 * for stock items, which will check expiry dates and send notifications.
 */
@Singleton
class ExpirationRepository @Inject constructor(
    private val apiService: ExpirationApiService
) {
    /**
     * Triggers a manual check for expiring items.
     * 
     * This method calls the backend API to check stock items for expiry dates
     * and send out notifications for items that are expiring soon.
     * 
     * @return Result containing the response data with item count and notifications sent
     */
    suspend fun checkExpiringItems(): Result<ExpirationCheckResponse> {
        return try {
            Log.d("ExpirationRepository", "Calling check-expiring API endpoint")
            val response = apiService.checkExpiringItems()
            
            Log.d("ExpirationRepository", "Response code: ${response.code()}")
            Log.d("ExpirationRepository", "Response message: ${response.message()}")
            Log.d("ExpirationRepository", "Response isSuccessful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    Log.d("ExpirationRepository", "Expiration check completed successfully: ${responseBody.itemCount} items, ${responseBody.notificationsSent} notifications sent")
                    Result.success(responseBody)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ExpirationRepository", "Expiration check response body is null. Error body: $errorBody")
                    Result.failure(Exception("Response body is null. Error: $errorBody"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ExpirationRepository", "Expiration check failed with code: ${response.code()}. Error body: $errorBody")
                Result.failure(Exception("API call failed with code: ${response.code()}. Error: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("ExpirationRepository", "Error calling expiration check API", e)
            Log.e("ExpirationRepository", "Exception type: ${e.javaClass.simpleName}")
            Log.e("ExpirationRepository", "Exception message: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
