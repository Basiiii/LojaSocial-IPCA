package com.lojasocial.app.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Request data classes for notification API.
 */
data class NewApplicationNotificationRequest(
    val applicationId: String
)

data class DateProposedOrAcceptedNotificationRequest(
    val requestId: String,
    val recipientUserId: String,
    val isAccepted: Boolean = false
)

data class NewRequestNotificationRequest(
    val requestId: String
)

data class PickupReminderNotificationRequest(
    val requestId: String,
    val beneficiaryUserId: String
)

data class RequestAcceptedNotificationRequest(
    val requestId: String,
    val beneficiaryUserId: String
)

data class ApplicationAcceptedNotificationRequest(
    val applicationId: String,
    val applicantUserId: String
)

data class ApplicationRejectedNotificationRequest(
    val applicationId: String,
    val applicantUserId: String
)

data class RequestRejectedNotificationRequest(
    val requestId: String,
    val beneficiaryUserId: String
)

data class BeneficiaryDateProposalNotificationRequest(
    val requestId: String
)

/**
 * Response data class for notification API.
 */
data class NotificationResponse(
    val success: Boolean,
    val message: String?,
    val error: String?
)

/**
 * API service interface for notification endpoints.
 * 
 * This service handles sending push notifications for various events:
 * - New applications
 * - Date proposals/acceptances
 * - New requests
 * - Pickup reminders
 * - Request acceptances
 * - Application acceptances
 */
interface NotificationApiService {
    /**
     * Sends notification to employees about a new application.
     */
    @POST("api/notifications/new-application")
    suspend fun notifyNewApplication(@Body request: NewApplicationNotificationRequest): Response<NotificationResponse>

    /**
     * Sends notification when a date is proposed or accepted.
     */
    @POST("api/notifications/date-proposed-or-accepted")
    suspend fun notifyDateProposedOrAccepted(@Body request: DateProposedOrAcceptedNotificationRequest): Response<NotificationResponse>

    /**
     * Sends notification to employees about a new request.
     */
    @POST("api/notifications/new-request")
    suspend fun notifyNewRequest(@Body request: NewRequestNotificationRequest): Response<NotificationResponse>

    /**
     * Sends pickup reminder notification to beneficiary.
     */
    @POST("api/notifications/pickup-reminder")
    suspend fun notifyPickupReminder(@Body request: PickupReminderNotificationRequest): Response<NotificationResponse>

    /**
     * Sends notification when a request is accepted.
     */
    @POST("api/notifications/request-accepted")
    suspend fun notifyRequestAccepted(@Body request: RequestAcceptedNotificationRequest): Response<NotificationResponse>

    /**
     * Sends notification when an application is accepted.
     */
    @POST("api/notifications/application-accepted")
    suspend fun notifyApplicationAccepted(@Body request: ApplicationAcceptedNotificationRequest): Response<NotificationResponse>

    /**
     * Sends notification when an application is rejected.
     */
    @POST("api/notifications/application-rejected")
    suspend fun notifyApplicationRejected(@Body request: ApplicationRejectedNotificationRequest): Response<NotificationResponse>

    /**
     * Sends notification when a request is rejected.
     */
    @POST("api/notifications/request-rejected")
    suspend fun notifyRequestRejected(@Body request: RequestRejectedNotificationRequest): Response<NotificationResponse>

    /**
     * Sends notification to all employees when beneficiary proposes a new date.
     */
    @POST("api/notifications/beneficiary-date-proposal")
    suspend fun notifyBeneficiaryDateProposal(@Body request: BeneficiaryDateProposalNotificationRequest): Response<NotificationResponse>
}
