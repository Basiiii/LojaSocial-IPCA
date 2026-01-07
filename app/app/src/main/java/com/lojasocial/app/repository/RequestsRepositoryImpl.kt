package com.lojasocial.app.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.domain.Request
import com.lojasocial.app.domain.RequestItemDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val productRepository: com.lojasocial.app.repository.ProductRepository
) : RequestsRepository {

    /**
     * Helper function to process an item and fetch its product name.
     */
    private suspend fun processItem(productDocId: String, quantity: Int, items: MutableList<com.lojasocial.app.domain.RequestItemDetail>) {
        Log.d("RequestsRepository", "Processing item: productDocId=$productDocId, quantity=$quantity")
        
        // First, get the item document to extract productId/barcode
        val productName = try {
            Log.d("RequestsRepository", "Fetching item document: $productDocId")
            val itemDoc = firestore.collection("items").document(productDocId).get().await()
            Log.d("RequestsRepository", "Item document exists: ${itemDoc.exists()}")
            
            if (itemDoc.exists()) {
                val itemData = itemDoc.data
                Log.d("RequestsRepository", "Item data: $itemData")
                
                // Get barcode or productId from the item
                val barcode = (itemData?.get("barcode") as? String) 
                    ?: (itemData?.get("productId") as? String)
                    ?: itemData?.get("productId")?.toString()
                
                if (barcode != null) {
                    Log.d("RequestsRepository", "Fetching product from products collection using barcode: $barcode")
                    // Fetch product name from products collection using barcode
                    val product = productRepository.getProductByBarcodeId(barcode)
                    if (product != null) {
                        Log.d("RequestsRepository", "Product found: ${product.name}")
                        product.name
                    } else {
                        Log.w("RequestsRepository", "Product not found in products collection for barcode: $barcode")
                        "Produto não encontrado"
                    }
                } else {
                    Log.w("RequestsRepository", "No barcode/productId found in item document")
                    "Produto sem código"
                }
            } else {
                Log.w("RequestsRepository", "Item document not found: $productDocId")
                "Item não encontrado"
            }
        } catch (e: Exception) {
            Log.e("RequestsRepository", "Error fetching product for item $productDocId: ${e.message}", e)
            "Erro ao carregar produto"
        }
        
        Log.d("RequestsRepository", "Adding item: $quantity x $productName")
        items.add(
            com.lojasocial.app.domain.RequestItemDetail(
                productDocId = productDocId,
                productName = productName,
                quantity = quantity
            )
        )
    }

    /**
     * Helper function to convert Firestore Timestamp to Date.
     */
    private fun convertToDate(value: Any?): Date? {
        return when {
            value is Date -> value
            value != null -> {
                val className = value.javaClass.name
                if (className == "com.google.firebase.Timestamp" || 
                    className == "com.google.firebase.firestore.Timestamp") {
                    try {
                        val toDateMethod = value.javaClass.getMethod("toDate")
                        toDateMethod.invoke(value) as? Date
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            }
            else -> null
        }
    }

    override fun getAllRequests(): Flow<List<Request>> = flow {
        try {
            // Try with orderBy first, fallback to unordered if index doesn't exist
            val snapshot = try {
                firestore.collection("requests")
                    .orderBy("submissionDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
            } catch (e: Exception) {
                // If orderBy fails (index missing), get without ordering
                Log.w("RequestsRepository", "OrderBy failed, fetching without order: ${e.message}")
                firestore.collection("requests")
                    .get()
                    .await()
            }

            val requests = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val userId = data["userId"] as? String ?: return@mapNotNull null
                    val status = (data["status"] as? Long)?.toInt() ?: 0
                    val submissionDate = convertToDate(data["submissionDate"])
                    val totalItems = (data["totalItems"] as? Long)?.toInt() ?: 0
                    val scheduledPickupDate = convertToDate(data["scheduledPickupDate"])
                    val rejectionReason = data["rejectionReason"] as? String

                    Request(
                        id = doc.id,
                        userId = userId,
                        status = status,
                        submissionDate = submissionDate,
                        totalItems = totalItems,
                        scheduledPickupDate = scheduledPickupDate,
                        rejectionReason = rejectionReason,
                        items = emptyList() // Items loaded separately when needed
                    )
                } catch (e: Exception) {
                    Log.e("RequestsRepository", "Error parsing document ${doc.id}: ${e.message}")
                    null
                }
            }

            // Sort manually if we couldn't use orderBy
            val sortedRequests = if (snapshot.documents.isEmpty() || 
                snapshot.documents.firstOrNull()?.data?.get("submissionDate") != null) {
                requests.sortedByDescending { it.submissionDate ?: Date(0) }
            } else {
                requests
            }

            Log.d("RequestsRepository", "Fetched ${sortedRequests.size} requests")
            emit(sortedRequests)
        } catch (e: Exception) {
            Log.e("RequestsRepository", "Error fetching requests: ${e.message}", e)
            emit(emptyList())
        }
    }

    override suspend fun getRequestById(requestId: String): Result<Request> {
        return try {
            Log.d("RequestsRepository", "Fetching request by ID: $requestId")
            val doc = firestore.collection("requests").document(requestId).get().await()
            val data = doc.data ?: return Result.failure(Exception("Request not found"))
            Log.d("RequestsRepository", "Request document fetched successfully")

            val userId = data["userId"] as? String ?: return Result.failure(Exception("Invalid request data"))
            val status = (data["status"] as? Long)?.toInt() ?: 0
            val submissionDate = convertToDate(data["submissionDate"])
            val totalItems = (data["totalItems"] as? Long)?.toInt() ?: 0
            val scheduledPickupDate = convertToDate(data["scheduledPickupDate"])
            val rejectionReason = data["rejectionReason"] as? String

            // Load items from subcollection
            Log.d("RequestsRepository", "Fetching items subcollection for request: $requestId")
            val items = mutableListOf<RequestItemDetail>()
            
            // Try to get items from subcollection first
            try {
                val itemsSnapshot = doc.reference.collection("items").get().await()
                Log.d("RequestsRepository", "Items subcollection fetched, count: ${itemsSnapshot.documents.size}")
                
                if (itemsSnapshot.documents.isEmpty()) {
                    // Check if items are stored as an array in the request document itself
                    val itemsArray = data["items"] as? List<Map<String, Any>>
                    if (itemsArray != null && itemsArray.isNotEmpty()) {
                        Log.d("RequestsRepository", "Found items array in request document, count: ${itemsArray.size}")
                        for (itemData in itemsArray) {
                            val productDocId = itemData["productDocId"] as? String ?: continue
                            val quantity = (itemData["quantity"] as? Long)?.toInt() ?: 0
                            processItem(productDocId, quantity, items)
                        }
                    } else {
                        Log.w("RequestsRepository", "No items found in subcollection or document array")
                    }
                } else {
                    // Process items from subcollection
                    for (itemDoc in itemsSnapshot.documents) {
                        val itemData = itemDoc.data ?: continue
                        val productDocId = itemData["productDocId"] as? String ?: continue
                        val quantity = (itemData["quantity"] as? Long)?.toInt() ?: 0
                        processItem(productDocId, quantity, items)
                    }
                }
            } catch (e: Exception) {
                Log.e("RequestsRepository", "Error fetching items subcollection: ${e.message}", e)
                // Try fallback: check if items are in document as array
                val itemsArray = data["items"] as? List<Map<String, Any>>
                if (itemsArray != null && itemsArray.isNotEmpty()) {
                    Log.d("RequestsRepository", "Using fallback: items array in document, count: ${itemsArray.size}")
                    for (itemData in itemsArray) {
                        val productDocId = itemData["productDocId"] as? String ?: continue
                        val quantity = (itemData["quantity"] as? Long)?.toInt() ?: 0
                        processItem(productDocId, quantity, items)
                    }
                }
            }

            val request = Request(
                id = doc.id,
                userId = userId,
                status = status,
                submissionDate = submissionDate,
                totalItems = totalItems,
                scheduledPickupDate = scheduledPickupDate,
                rejectionReason = rejectionReason,
                items = items
            )

            Log.d("RequestsRepository", "Request loaded successfully with ${items.size} items")
            Result.success(request)
        } catch (e: Exception) {
            Log.e("RequestsRepository", "Error fetching request by ID: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun acceptRequest(requestId: String, scheduledPickupDate: Date): Result<Unit> {
        return try {
            val timestamp = Timestamp(scheduledPickupDate)
            firestore.collection("requests").document(requestId)
                .update(
                    mapOf(
                        "status" to 1, // PENDENTE_LEVANTAMENTO
                        "scheduledPickupDate" to timestamp
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectRequest(requestId: String, reason: String?): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to 4 // REJEITADO
            )
            if (reason != null) {
                updates["rejectionReason"] = reason
            }
            firestore.collection("requests").document(requestId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserFcmToken(userId: String): Result<String?> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val token = userDoc.data?.get("fcmToken") as? String
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(userId: String): Result<com.lojasocial.app.repository.UserProfileData> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val data = userDoc.data ?: return Result.failure(Exception("User not found"))
            val name = data["name"] as? String ?: ""
            val email = data["email"] as? String ?: ""
            Result.success(com.lojasocial.app.repository.UserProfileData(name = name, email = email))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

