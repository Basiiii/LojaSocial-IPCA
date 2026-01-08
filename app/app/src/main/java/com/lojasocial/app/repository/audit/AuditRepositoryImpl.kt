package com.lojasocial.app.repository.audit

import android.util.Log
import com.lojasocial.app.api.AuditApiService
import com.lojasocial.app.domain.audit.AuditLogEntry
import com.lojasocial.app.domain.audit.AuditLogRequest
import com.lojasocial.app.repository.auth.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditRepositoryImpl @Inject constructor(
    private val auditApiService: AuditApiService,
    private val authRepository: AuthRepository
) : AuditRepository {

    companion object {
        private const val TAG = "AuditRepository"
        private const val BEARER_TOKEN = "Bearer lojasocial2025"
    }

    override suspend fun logAction(
        action: String,
        userId: String?,
        details: Map<String, Any>?
    ) {
        try {
            // Use provided userId or get current user ID
            val finalUserId = userId ?: authRepository.getCurrentUser()?.uid

            val request = AuditLogRequest(
                action = action,
                userId = finalUserId,
                details = details
            )

            val response = auditApiService.logAction(
                authorization = BEARER_TOKEN,
                contentType = "application/json",
                request = request
            )

            if (!response.isSuccessful) {
                Log.w(TAG, "Failed to log action: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            // Silently handle errors to avoid impacting main operations
            Log.e(TAG, "Error logging action: ${e.message}", e)
        }
    }

    override suspend fun getLogs(
        startDate: String,
        endDate: String
    ): Result<List<AuditLogEntry>> {
        return try {
            val response = auditApiService.getLogs(
                authorization = BEARER_TOKEN,
                startDate = startDate,
                endDate = endDate
            )

            if (response.isSuccessful) {
                val auditLogResponse = response.body()
                val entries = auditLogResponse?.logs?.map { apiEntry ->
                    AuditLogEntry(
                        action = apiEntry.action,
                        timestamp = apiEntry.timestamp,
                        userId = apiEntry.userId,
                        details = apiEntry.details
                    )
                } ?: emptyList()
                Result.success(entries)
            } else {
                Result.failure(Exception("Failed to fetch logs: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching audit logs: ${e.message}", e)
            Result.failure(e)
        }
    }
}
