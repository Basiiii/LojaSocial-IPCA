package com.lojasocial.app.domain.audit

import com.lojasocial.app.domain.product.Product

/**
 * Data class representing an audit log request to the backend API.
 * 
 * @param action The action type (e.g., "add_item", "remove_item", "accept_request", etc.)
 * @param userId Optional user ID who performed the action
 * @param userName Optional user name who performed the action
 * @param details Optional map of additional details about the action
 */
data class AuditLogRequest(
    val action: String,
    val userId: String? = null,
    val userName: String? = null,
    val details: Map<String, Any>? = null
)

/**
 * Data class representing a single audit log entry.
 * 
 * @param action The action type
 * @param timestamp ISO 8601 formatted timestamp
 * @param userId Optional user ID who performed the action
 * @param userName Optional user name who performed the action
 * @param details Optional map of additional details
 */
data class AuditLogEntry(
    val action: String,
    val timestamp: String,
    val userId: String? = null,
    val userName: String? = null,
    val details: Map<String, Any>? = null
)

/**
 * Data class representing the response from the audit logs API.
 * 
 * @param logs List of audit log entries
 */
data class AuditLogResponse(
    val logs: List<AuditLogEntry>
)

/**
 * Data class representing a product in the campaign products API response.
 */
data class CampaignProductApiModel(
    val id: String = "",
    val name: String = "",
    val brand: String = "",
    val category: Int = 1,
    val imageUrl: String = ""
)

/**
 * Data class representing a campaign product receipt in the API response.
 */
data class CampaignProductReceiptApiModel(
    val itemId: String,
    val quantity: Int,
    val barcode: String,
    val timestamp: String?,
    val userId: String?,
    val userName: String?,
    val product: CampaignProductApiModel?
)

/**
 * Data class representing the response from the campaign products API.
 * 
 * @param success Whether the request was successful
 * @param count Number of products returned
 * @param products List of campaign product receipts
 */
data class CampaignProductsResponse(
    val success: Boolean,
    val count: Int,
    val products: List<CampaignProductReceiptApiModel>
)