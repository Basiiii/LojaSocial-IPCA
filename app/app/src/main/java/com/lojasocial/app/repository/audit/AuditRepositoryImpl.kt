package com.lojasocial.app.repository.audit

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.api.AuditApiService
import com.lojasocial.app.domain.audit.AuditLogEntry
import com.lojasocial.app.domain.audit.AuditLogRequest
import com.lojasocial.app.domain.product.Product
import com.lojasocial.app.repository.auth.AuthRepository
import com.lojasocial.app.repository.user.UserRepository
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditRepositoryImpl @Inject constructor(
    private val auditApiService: AuditApiService,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore
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
            
            val (logUserId, logUserName) = if (finalUserId != null) {
                try {
                    val userProfile = userRepository.getUserProfile(finalUserId).firstOrNull()
                    Pair(finalUserId, userProfile?.name)
                } catch (e: Exception) {
                    Log.w(TAG, "Error fetching user name for audit log: ${e.message}")
                    // If we can't get the name, still log with userId
                    Pair(finalUserId, null)
                }
            } else {
                // If no userId, both should be null
                Pair(null, null)
            }

            val request = AuditLogRequest(
                action = action,
                userId = logUserId,
                userName = logUserName,
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
                        userName = apiEntry.userName,
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
    
    override suspend fun getCampaignProducts(campaignId: String): Result<List<CampaignProductReceipt>> {
        return try {
            // Validate campaign ID before making API call
            if (campaignId.isBlank()) {
                Log.e(TAG, "Campaign ID is empty or blank")
                return Result.failure(Exception("ID da campanha inválido"))
            }
            
            // Validate campaign ID format
            val trimmedCampaignId = campaignId.trim()
            if (trimmedCampaignId.contains(" ") || trimmedCampaignId.contains("\n") || trimmedCampaignId.contains("\r")) {
                Log.e(TAG, "Campaign ID contains invalid characters: '$trimmedCampaignId'")
                return Result.failure(Exception("ID da campanha contém caracteres inválidos"))
            }
            
            Log.d(TAG, "Fetching campaign products for campaignId: '$trimmedCampaignId' (length: ${trimmedCampaignId.length})")
            
            val response = auditApiService.getCampaignProducts(
                campaignId = trimmedCampaignId
            )

            Log.d(TAG, "API response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
            
            if (!response.isSuccessful) {
                val errorBody = try {
                    response.errorBody()?.string() ?: "No error body"
                } catch (e: Exception) {
                    "Error reading error body: ${e.message}"
                }
                Log.e(TAG, "API request failed. URL would be: api/audit/campaign/$trimmedCampaignId/products")
                Log.e(TAG, "Error body: $errorBody")
            }

            if (response.isSuccessful) {
                val apiResponse = response.body()
                Log.d(TAG, "API response body: count=${apiResponse?.count}, products size=${apiResponse?.products?.size}")
                val receipts = apiResponse?.products?.mapNotNull { apiReceipt ->
                    try {
                        // Parse timestamp from ISO 8601 string
                        val timestamp = apiReceipt.timestamp?.let { timestampStr ->
                            try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                                sdf.timeZone = TimeZone.getTimeZone("UTC")
                                sdf.parse(timestampStr)
                            } catch (e: Exception) {
                                try {
                                    // Try alternative format without milliseconds
                                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                                    sdf.parse(timestampStr)
                                } catch (e2: Exception) {
                                    Log.e(TAG, "Error parsing timestamp: $timestampStr", e2)
                                    null
                                }
                            }
                        }
                        
                        // Convert API product model to domain Product
                        val product = apiReceipt.product?.let { apiProduct ->
                            Product(
                                id = apiProduct.id,
                                name = apiProduct.name,
                                brand = apiProduct.brand,
                                category = apiProduct.category,
                                imageUrl = apiProduct.imageUrl,
                                serializedImage = apiProduct.serializedImage
                            )
                        }
                        
                        CampaignProductReceipt(
                            itemId = apiReceipt.itemId,
                            quantity = apiReceipt.quantity,
                            barcode = apiReceipt.barcode,
                            timestamp = timestamp,
                            userId = apiReceipt.userId,
                            userName = apiReceipt.userName,
                            product = product
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing campaign product receipt: ${e.message}", e)
                        null
                    }
                } ?: emptyList()
                
                Log.d(TAG, "Successfully loaded ${receipts.size} campaign products")
                if (receipts.isEmpty()) {
                    Log.w(TAG, "No campaign products found for campaignId: $campaignId. Check backend logs for details.")
                }
                Result.success(receipts)
            } else {
                val errorMsg = try {
                    val errorBody = response.errorBody()?.string()
                    "Failed to fetch campaign products: ${response.code()} - ${response.message()}. Error body: $errorBody"
                } catch (e: Exception) {
                    "Failed to fetch campaign products: ${response.code()} - ${response.message()}"
                }
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching campaign products: ${e.message}", e)
            Result.failure(e)
        }
    }
}
