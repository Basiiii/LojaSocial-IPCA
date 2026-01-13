package com.lojasocial.app.api

import com.lojasocial.app.domain.audit.AuditLogRequest
import com.lojasocial.app.domain.audit.AuditLogResponse
import com.lojasocial.app.domain.audit.CampaignProductsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Audit API service.
 * 
 * This interface defines the HTTP endpoints for communicating with the
 * Loja Social audit API hosted at https://lojasocial-ipca.onrender.com/
 * 
 * The API uses POST requests for logging actions and GET requests for
 * retrieving logs, with Bearer token authentication.
 */
interface AuditApiService {
    /**
     * Logs an action to the audit system.
     * 
     * @param authorization The Bearer token for API authentication (format: "Bearer lojasocial2025")
     * @param contentType The content type of the request (should be "application/json")
     * @param request The audit log request containing action, userId, and details
     * @return Response indicating success or error
     */
    @POST("api/audit/log")
    suspend fun logAction(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String,
        @Body request: AuditLogRequest
    ): Response<Unit>

    /**
     * Retrieves audit logs between specified dates.
     * 
     * @param authorization The Bearer token for API authentication (format: "Bearer lojasocial2025")
     * @param startDate ISO 8601 formatted start date (e.g., "2025-01-15T00:00:00Z")
     * @param endDate ISO 8601 formatted end date (e.g., "2025-01-20T23:59:59Z")
     * @return Response containing list of audit log entries
     */
    @GET("api/audit/logs")
    suspend fun getLogs(
        @Header("Authorization") authorization: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<AuditLogResponse>

    /**
     * Retrieves all products received for a specific campaign.
     * 
     * Authorization header is added automatically by the interceptor.
     * 
     * @param campaignId The ID of the campaign
     * @return Response containing list of campaign product receipts with product information
     */
    @GET("api/audit/campaign/{campaignId}/products")
    suspend fun getCampaignProducts(
        @Path(value = "campaignId", encoded = false) campaignId: String
    ): Response<CampaignProductsResponse>
}
