package com.lojasocial.app.domain.audit

/**
 * Data class representing an audit log request to the backend API.
 * 
 * @param action The action type (e.g., "add_item", "remove_item", "accept_request", etc.)
 * @param userId Optional user ID who performed the action
 * @param details Optional map of additional details about the action
 */
data class AuditLogRequest(
    val action: String,
    val userId: String? = null,
    val details: Map<String, Any>? = null
)

/**
 * Data class representing a single audit log entry.
 * 
 * @param action The action type
 * @param timestamp ISO 8601 formatted timestamp
 * @param userId Optional user ID who performed the action
 * @param details Optional map of additional details
 */
data class AuditLogEntry(
    val action: String,
    val timestamp: String,
    val userId: String? = null,
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
