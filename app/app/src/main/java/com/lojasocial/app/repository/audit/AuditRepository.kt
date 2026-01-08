package com.lojasocial.app.repository.audit

import com.lojasocial.app.domain.audit.AuditLogEntry

/**
 * Repository interface for audit logging operations.
 * 
 * This interface provides methods for logging actions and retrieving audit logs
 * from the backend API.
 */
interface AuditRepository {
    /**
     * Logs an action to the audit system.
     * 
     * This is a fire-and-forget operation that should not block the main operation.
     * Errors are handled silently to avoid impacting the main flow.
     * 
     * @param action The action type (e.g., "add_item", "remove_item", "accept_request", etc.)
     * @param userId Optional user ID who performed the action. If null, current user ID will be used.
     * @param details Optional map of additional details about the action
     */
    suspend fun logAction(
        action: String,
        userId: String? = null,
        details: Map<String, Any>? = null
    )

    /**
     * Retrieves audit logs between specified dates.
     * 
     * @param startDate ISO 8601 formatted start date (e.g., "2025-01-15T00:00:00Z")
     * @param endDate ISO 8601 formatted end date (e.g., "2025-01-20T23:59:59Z")
     * @return Result containing list of audit log entries or error
     */
    suspend fun getLogs(
        startDate: String,
        endDate: String
    ): Result<List<AuditLogEntry>>
    
    /**
     * Logs a campaign product receipt directly to Firestore audit_logs collection.
     * 
     * This method is specifically designed for tracking products received in campaigns.
     * It stores: action, campaignId, itemId, quantity, barcode, timestamp, and userId.
     * 
     * @param campaignId The ID of the campaign receiving the product
     * @param itemId The document ID from the items collection
     * @param quantity The quantity of products received
     * @param barcode The barcode of the product (for product info lookup)
     * @param userId Optional user ID who received the product. If null, current user ID will be used.
     */
    suspend fun logCampaignProductReceipt(
        campaignId: String,
        itemId: String,
        quantity: Int,
        barcode: String,
        userId: String? = null
    )
    
    /**
     * Retrieves all products received for a specific campaign from audit_logs.
     * 
     * @param campaignId The ID of the campaign
     * @return Result containing list of campaign product receipts with product information
     */
    suspend fun getCampaignProducts(campaignId: String): Result<List<CampaignProductReceipt>>
}
