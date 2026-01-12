package com.lojasocial.app.repository.notification

import android.util.Log
import com.lojasocial.app.api.NotificationApiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for sending push notifications.
 * 
 * This repository handles sending notifications for various events:
 * - New applications
 * - Date proposals/acceptances
 * - New requests
 * - Pickup reminders
 * - Request acceptances
 * - Application acceptances
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val apiService: NotificationApiService
) {
    private val TAG = "NotificationRepository"

    /**
     * Sends notification to employees about a new application.
     */
    suspend fun notifyNewApplication(applicationId: String): Result<Unit> {
        return try {
            val response = apiService.notifyNewApplication(
                com.lojasocial.app.api.NewApplicationNotificationRequest(applicationId)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "New application notification sent successfully")
                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: "Unknown error"
                Log.e(TAG, "Failed to send new application notification: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending new application notification", e)
            Result.failure(e)
        }
    }

    /**
     * Sends notification when a date is proposed or accepted.
     */
    suspend fun notifyDateProposedOrAccepted(
        requestId: String,
        recipientUserId: String,
        isAccepted: Boolean = false
    ): Result<Unit> {
        return try {
            val response = apiService.notifyDateProposedOrAccepted(
                com.lojasocial.app.api.DateProposedOrAcceptedNotificationRequest(
                    requestId = requestId,
                    recipientUserId = recipientUserId,
                    isAccepted = isAccepted
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "Date proposed/accepted notification sent successfully")
                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: "Unknown error"
                Log.e(TAG, "Failed to send date proposed/accepted notification: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending date proposed/accepted notification", e)
            Result.failure(e)
        }
    }

    /**
     * Sends notification to employees about a new request.
     */
    suspend fun notifyNewRequest(requestId: String): Result<Unit> {
        return try {
            val response = apiService.notifyNewRequest(
                com.lojasocial.app.api.NewRequestNotificationRequest(requestId)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "New request notification sent successfully")
                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: "Unknown error"
                Log.e(TAG, "Failed to send new request notification: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending new request notification", e)
            Result.failure(e)
        }
    }

    /**
     * Sends pickup reminder notification to beneficiary.
     */
    suspend fun notifyPickupReminder(requestId: String, beneficiaryUserId: String): Result<Unit> {
        return try {
            val response = apiService.notifyPickupReminder(
                com.lojasocial.app.api.PickupReminderNotificationRequest(
                    requestId = requestId,
                    beneficiaryUserId = beneficiaryUserId
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "Pickup reminder notification sent successfully")
                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: "Unknown error"
                Log.e(TAG, "Failed to send pickup reminder notification: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending pickup reminder notification", e)
            Result.failure(e)
        }
    }

    /**
     * Sends notification when a request is accepted.
     */
    suspend fun notifyRequestAccepted(requestId: String, beneficiaryUserId: String): Result<Unit> {
        return try {
            val response = apiService.notifyRequestAccepted(
                com.lojasocial.app.api.RequestAcceptedNotificationRequest(
                    requestId = requestId,
                    beneficiaryUserId = beneficiaryUserId
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "Request accepted notification sent successfully")
                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: "Unknown error"
                Log.e(TAG, "Failed to send request accepted notification: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending request accepted notification", e)
            Result.failure(e)
        }
    }

    /**
     * Sends notification when an application is accepted.
     */
    suspend fun notifyApplicationAccepted(applicationId: String, applicantUserId: String): Result<Unit> {
        return try {
            val response = apiService.notifyApplicationAccepted(
                com.lojasocial.app.api.ApplicationAcceptedNotificationRequest(
                    applicationId = applicationId,
                    applicantUserId = applicantUserId
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "Application accepted notification sent successfully")
                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: "Unknown error"
                Log.e(TAG, "Failed to send application accepted notification: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending application accepted notification", e)
            Result.failure(e)
        }
    }

    /**
     * Sends notification when an application is rejected.
     */
    suspend fun notifyApplicationRejected(applicationId: String, applicantUserId: String): Result<Unit> {
        return try {
            val response = apiService.notifyApplicationRejected(
                com.lojasocial.app.api.ApplicationRejectedNotificationRequest(
                    applicationId = applicationId,
                    applicantUserId = applicantUserId
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "Application rejected notification sent successfully")
                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: "Unknown error"
                Log.e(TAG, "Failed to send application rejected notification: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending application rejected notification", e)
            Result.failure(e)
        }
    }

    /**
     * Sends notification when a request is rejected.
     */
    suspend fun notifyRequestRejected(requestId: String, beneficiaryUserId: String): Result<Unit> {
        return try {
            val response = apiService.notifyRequestRejected(
                com.lojasocial.app.api.RequestRejectedNotificationRequest(
                    requestId = requestId,
                    beneficiaryUserId = beneficiaryUserId
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "Request rejected notification sent successfully")
                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: "Unknown error"
                Log.e(TAG, "Failed to send request rejected notification: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending request rejected notification", e)
            Result.failure(e)
        }
    }
}
