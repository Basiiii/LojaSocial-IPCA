package com.lojasocial.app.repository.audit

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.api.AuditApiService
import com.lojasocial.app.domain.audit.AuditLogEntry
import com.lojasocial.app.domain.audit.AuditLogRequest
import com.lojasocial.app.domain.product.Product
import com.lojasocial.app.repository.auth.AuthRepository
import kotlinx.coroutines.tasks.await
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
    
    override suspend fun logCampaignProductReceipt(
        campaignId: String,
        itemId: String,
        quantity: Int,
        barcode: String,
        userId: String?
    ) {
        try {
            // Use provided userId or get current user ID
            val finalUserId = userId ?: authRepository.getCurrentUser()?.uid
            
            // Create audit log document in Firestore
            val auditLogData = hashMapOf<String, Any>(
                "action" to "campaign_receive_product",
                "campaignId" to campaignId,
                "itemId" to itemId,
                "quantity" to quantity,
                "barcode" to barcode,
                "timestamp" to FieldValue.serverTimestamp(),
                "userId" to (finalUserId ?: "")
            )
            
            firestore.collection("audit_logs")
                .add(auditLogData)
                .await()
            
            Log.d(TAG, "Campaign product receipt logged: campaignId=$campaignId, itemId=$itemId, quantity=$quantity")
        } catch (e: Exception) {
            // Silently handle errors to avoid impacting main operations
            Log.e(TAG, "Error logging campaign product receipt: ${e.message}", e)
        }
    }
    
    override suspend fun getCampaignProducts(campaignId: String): Result<List<CampaignProductReceipt>> {
        return try {
            Log.d(TAG, "Fetching campaign products for campaignId: $campaignId")
            
            val response = auditApiService.getCampaignProducts(
                authorization = BEARER_TOKEN,
                campaignId = campaignId
            )

            if (response.isSuccessful) {
                val apiResponse = response.body()
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
                                imageUrl = apiProduct.imageUrl
                            )
                        }
                        
                        CampaignProductReceipt(
                            itemId = apiReceipt.itemId,
                            quantity = apiReceipt.quantity,
                            barcode = apiReceipt.barcode,
                            timestamp = timestamp,
                            userId = apiReceipt.userId,
                            product = product
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing campaign product receipt: ${e.message}", e)
                        null
                    }
                } ?: emptyList()
                
                Log.d(TAG, "Successfully loaded ${receipts.size} campaign products")
                Result.success(receipts)
            } else {
                val errorMsg = "Failed to fetch campaign products: ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching campaign products: ${e.message}", e)
            Result.failure(e)
        }
    }
}
