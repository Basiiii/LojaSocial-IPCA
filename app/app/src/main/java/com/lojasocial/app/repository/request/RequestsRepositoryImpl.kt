package com.lojasocial.app.repository.request

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.domain.request.Request
import com.lojasocial.app.domain.request.RequestItemDetail
import com.lojasocial.app.repository.audit.AuditRepository
import com.lojasocial.app.repository.auth.AuthRepository
import com.lojasocial.app.repository.product.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val productRepository: ProductRepository,
    private val auditRepository: AuditRepository,
    private val authRepository: AuthRepository
) : RequestsRepository {

    /**
     * Helper function to process an item and fetch its product name, brand, expiryDate, and category.
     */
    private suspend fun processItem(productDocId: String, quantity: Int, items: MutableList<RequestItemDetail>) {
        Log.d("RequestsRepository", "Processing item: productDocId=$productDocId, quantity=$quantity")
        
        // First, get the item document to extract productId/barcode and expiryDate
        val result = try {
            Log.d("RequestsRepository", "Fetching item document: $productDocId")
            val itemDoc = firestore.collection("items").document(productDocId).get().await()
            Log.d("RequestsRepository", "Item document exists: ${itemDoc.exists()}")
            
            if (itemDoc.exists()) {
                val itemData = itemDoc.data
                Log.d("RequestsRepository", "Item data: $itemData")
                
                // Get expiryDate from item document (check both field names)
                val expiryTimestamp = (itemData?.get("expiryDate") as? Timestamp)
                    ?: (itemData?.get("expirationDate") as? Timestamp)
                val expiryDateValue = expiryTimestamp?.toDate()
                
                // Get barcode or productId from the item
                val barcode = (itemData?.get("barcode") as? String) 
                    ?: (itemData?.get("productId") as? String)
                    ?: itemData?.get("productId")?.toString()
                
                if (barcode != null) {
                    Log.d("RequestsRepository", "Fetching product from products collection using barcode: $barcode")
                    // Fetch product information from products collection using barcode
                    val product = productRepository.getProductByBarcodeId(barcode)
                    if (product != null) {
                        Log.d("RequestsRepository", "Product found: ${product.name}, brand: ${product.brand}, category: ${product.category}")
                        Triple(product.name, product.brand, product.category) to expiryDateValue
                    } else {
                        Log.w("RequestsRepository", "Product not found in products collection for barcode: $barcode")
                        Triple("Produto não encontrado", "", 1) to null
                    }
                } else {
                    Log.w("RequestsRepository", "No barcode/productId found in item document")
                    Triple("Produto sem código", "", 1) to null
                }
            } else {
                Log.w("RequestsRepository", "Item document not found: $productDocId")
                Triple("Item não encontrado", "", 1) to null
            }
        } catch (e: Exception) {
            Log.e("RequestsRepository", "Error fetching product for item $productDocId: ${e.message}", e)
            Triple("Erro ao carregar produto", "", 1) to null
        }
        
        val (productName, brand, category) = result.first
        val expiryDateValue = result.second
        
        Log.d("RequestsRepository", "Adding item: $quantity x $productName (brand: $brand, category: $category)")
        items.add(
            RequestItemDetail(
                productDocId = productDocId,
                productName = productName,
                quantity = quantity,
                brand = brand,
                expiryDate = expiryDateValue,
                category = category
            )
        )
    }

    /**
     * Helper function to get the category of items in a request (for list display).
     * Returns the category if all items have the same category, or null if there are multiple categories.
     * This is a lightweight operation that fetches categories for all items.
     */
    private suspend fun getRequestCategory(itemsMap: Map<*, *>?): Int? {
        if (itemsMap == null || itemsMap.isEmpty()) return null
        
        return try {
            val categories = mutableSetOf<Int>()
            
            // Get categories for all items
            for (itemDocIdObj in itemsMap.keys) {
                val itemDocId = itemDocIdObj as? String ?: continue
                val itemDoc = firestore.collection("items").document(itemDocId).get().await()
                
                if (!itemDoc.exists()) continue
                
                val itemData = itemDoc.data
                // Get barcode or productId from the item
                val barcode = (itemData?.get("barcode") as? String) 
                    ?: (itemData?.get("productId") as? String)
                    ?: itemData?.get("productId")?.toString()
                
                if (barcode != null) {
                    // Fetch product to get category
                    val product = productRepository.getProductByBarcodeId(barcode)
                    val category = product?.category ?: 1
                    categories.add(category)
                } else {
                    categories.add(1) // Default to ALIMENTAR
                }
            }
            
            // If all items have the same category, return it; otherwise return null (multiple categories)
            if (categories.size == 1) {
                categories.first()
            } else {
                null // Multiple categories - will be displayed as "Vários"
            }
        } catch (e: Exception) {
            Log.w("RequestsRepository", "Error fetching request category: ${e.message}")
            null // Return null on error to default to "Vários"
        }
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

                    // Get items map - category will be loaded asynchronously if needed
                    val itemsMap = data["items"] as? Map<*, *>
                    
                    // Create a minimal item list with default category (will be updated if items exist)
                    val minimalItems = if (itemsMap != null && itemsMap.isNotEmpty()) {
                        listOf(
                            com.lojasocial.app.domain.request.RequestItemDetail(
                                productDocId = "",
                                productName = "",
                                quantity = 0,
                                brand = "",
                                expiryDate = null,
                                category = 1 // Default, will be loaded asynchronously
                            )
                        )
                    } else {
                        emptyList()
                    }

                    Request(
                        id = doc.id,
                        userId = userId,
                        status = status,
                        submissionDate = submissionDate,
                        totalItems = totalItems,
                        scheduledPickupDate = scheduledPickupDate,
                        rejectionReason = rejectionReason,
                        items = minimalItems
                    )
                } catch (e: Exception) {
                    Log.e("RequestsRepository", "Error parsing document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            // Load categories for requests with items (in suspend context)
            val requestsWithCategories = mutableListOf<Request>()
            for (request in requests) {
                if (request.items.isNotEmpty()) {
                    val doc = snapshot.documents.find { it.id == request.id }
                    val itemsMap = doc?.data?.get("items") as? Map<*, *>
                    if (itemsMap != null && itemsMap.isNotEmpty()) {
                        val requestCategory = getRequestCategory(itemsMap)
                        // Use the category if all items have the same category, otherwise use 0 to indicate "Vários"
                        val categoryToUse = requestCategory ?: 0 // 0 means multiple categories
                        requestsWithCategories.add(
                            request.copy(
                                items = listOf(
                                    request.items.first().copy(category = categoryToUse)
                                )
                            )
                        )
                    } else {
                        requestsWithCategories.add(request)
                    }
                } else {
                    requestsWithCategories.add(request)
                }
            }

            // Sort manually if we couldn't use orderBy
            val sortedRequests = if (snapshot.documents.isEmpty() || 
                snapshot.documents.firstOrNull()?.data?.get("submissionDate") != null) {
                requestsWithCategories.sortedByDescending { it.submissionDate ?: Date(0) }
            } else {
                requestsWithCategories
            }

            Log.d("RequestsRepository", "Fetched ${sortedRequests.size} requests")
            emit(sortedRequests)
        } catch (e: Exception) {
            Log.e("RequestsRepository", "Error fetching requests: ${e.message}", e)
            emit(emptyList())
        }
    }

    override fun getRequests(): Flow<List<Request>> = flow {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.w("RequestsRepository", "User not authenticated, returning empty list")
                emit(emptyList())
                return@flow
            }
            
            // Try with orderBy first, fallback to unordered if index doesn't exist
            val snapshot = try {
                firestore.collection("requests")
                    .whereEqualTo("userId", userId)
                    .orderBy("submissionDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
            } catch (e: Exception) {
                // If orderBy fails (index missing), get without ordering
                Log.w("RequestsRepository", "OrderBy failed, fetching without order: ${e.message}")
                firestore.collection("requests")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
            }

            val requests = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val requestUserId = data["userId"] as? String ?: return@mapNotNull null
                    val status = (data["status"] as? Long)?.toInt() ?: 0
                    val submissionDate = convertToDate(data["submissionDate"])
                    val totalItems = (data["totalItems"] as? Long)?.toInt() ?: 0
                    val scheduledPickupDate = convertToDate(data["scheduledPickupDate"])
                    val rejectionReason = data["rejectionReason"] as? String

                    // Get items map - category will be loaded separately
                    val itemsMap = data["items"] as? Map<*, *>
                    
                    // Create a minimal item list with default category (will be updated)
                    val minimalItems = if (itemsMap != null && itemsMap.isNotEmpty()) {
                        listOf(
                            com.lojasocial.app.domain.request.RequestItemDetail(
                                productDocId = "",
                                productName = "",
                                quantity = 0,
                                brand = "",
                                expiryDate = null,
                                category = 1 // Default, will be updated
                            )
                        )
                    } else {
                        emptyList()
                    }

                    Request(
                        id = doc.id,
                        userId = requestUserId,
                        status = status,
                        submissionDate = submissionDate,
                        totalItems = totalItems,
                        scheduledPickupDate = scheduledPickupDate,
                        rejectionReason = rejectionReason,
                        items = minimalItems
                    )
                } catch (e: Exception) {
                    Log.e("RequestsRepository", "Error parsing document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            // Load categories for requests with items (in suspend context)
            val requestsWithCategories = mutableListOf<Request>()
            for (request in requests) {
                if (request.items.isNotEmpty()) {
                    val doc = snapshot.documents.find { it.id == request.id }
                    val itemsMap = doc?.data?.get("items") as? Map<*, *>
                    if (itemsMap != null && itemsMap.isNotEmpty()) {
                        val requestCategory = getRequestCategory(itemsMap)
                        // Use the category if all items have the same category, otherwise use 0 to indicate "Vários"
                        val categoryToUse = requestCategory ?: 0 // 0 means multiple categories
                        requestsWithCategories.add(
                            request.copy(
                                items = listOf(
                                    request.items.first().copy(category = categoryToUse)
                                )
                            )
                        )
                    } else {
                        requestsWithCategories.add(request)
                    }
                } else {
                    requestsWithCategories.add(request)
                }
            }

            // Sort manually if we couldn't use orderBy
            val sortedRequests = if (snapshot.documents.isEmpty() || 
                snapshot.documents.firstOrNull()?.data?.get("submissionDate") != null) {
                requestsWithCategories.sortedByDescending { it.submissionDate ?: Date(0) }
            } else {
                requestsWithCategories
            }

            Log.d("RequestsRepository", "Fetched ${sortedRequests.size} requests for user $userId")
            emit(sortedRequests)
        } catch (e: Exception) {
            Log.e("RequestsRepository", "Error fetching user requests: ${e.message}", e)
            emit(emptyList())
        }
    }

    override suspend fun getRequestsPaginated(
        limit: Int,
        lastSubmissionDate: Date?
    ): Pair<List<Request>, Boolean> {
        return try {
            var query = firestore.collection("requests")
                .orderBy("submissionDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            
            // If we have a lastSubmissionDate, start after it
            if (lastSubmissionDate != null) {
                query = query.startAfter(Timestamp(lastSubmissionDate))
            }
            
            val snapshot = try {
                query.limit((limit + 1).toLong()).get().await()
            } catch (e: Exception) {
                // If orderBy fails (index missing), fallback to simple pagination without ordering
                Log.w("RequestsRepository", "OrderBy pagination failed, using fallback: ${e.message}")
                // Just fetch limit + 1 documents without ordering - this is not ideal but better than fetching all
                firestore.collection("requests").limit((limit + 1).toLong()).get().await()
            }
            
            val hasMore = snapshot.documents.size > limit
            val documentsToProcess = if (hasMore) snapshot.documents.dropLast(1) else snapshot.documents
            
            val requests = documentsToProcess.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val userId = data["userId"] as? String ?: return@mapNotNull null
                    val status = (data["status"] as? Long)?.toInt() ?: 0
                    val submissionDate = convertToDate(data["submissionDate"])
                    val totalItems = (data["totalItems"] as? Long)?.toInt() ?: 0
                    val scheduledPickupDate = convertToDate(data["scheduledPickupDate"])
                    val rejectionReason = data["rejectionReason"] as? String
                    
                    // Get items map - category will be loaded separately
                    val itemsMap = data["items"] as? Map<*, *>
                    
                    // Create a minimal item list with default category (will be updated)
                    val minimalItems = if (itemsMap != null && itemsMap.isNotEmpty()) {
                        listOf(
                            com.lojasocial.app.domain.request.RequestItemDetail(
                                productDocId = "",
                                productName = "",
                                quantity = 0,
                                brand = "",
                                expiryDate = null,
                                category = 1 // Default, will be updated
                            )
                        )
                    } else {
                        emptyList()
                    }
                    
                    Request(
                        id = doc.id,
                        userId = userId,
                        status = status,
                        submissionDate = submissionDate,
                        totalItems = totalItems,
                        scheduledPickupDate = scheduledPickupDate,
                        rejectionReason = rejectionReason,
                        items = minimalItems
                    )
                } catch (e: Exception) {
                    Log.e("RequestsRepository", "Error parsing document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            // Load categories for requests with items (in suspend context)
            val requestsWithCategories = mutableListOf<Request>()
            for (request in requests) {
                if (request.items.isNotEmpty()) {
                    val doc = documentsToProcess.find { it.id == request.id }
                    val itemsMap = doc?.data?.get("items") as? Map<*, *>
                    if (itemsMap != null && itemsMap.isNotEmpty()) {
                        val requestCategory = getRequestCategory(itemsMap)
                        // Use the category if all items have the same category, otherwise use 0 to indicate "Vários"
                        val categoryToUse = requestCategory ?: 0 // 0 means multiple categories
                        requestsWithCategories.add(
                            request.copy(
                                items = listOf(
                                    request.items.first().copy(category = categoryToUse)
                                )
                            )
                        )
                    } else {
                        requestsWithCategories.add(request)
                    }
                } else {
                    requestsWithCategories.add(request)
                }
            }
            
            Pair(requestsWithCategories, hasMore)
        } catch (e: Exception) {
            Log.e("RequestsRepository", "Error fetching paginated requests: ${e.message}", e)
            Pair(emptyList(), false)
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

            // Load items from request document
            Log.d("RequestsRepository", "Fetching items for request: $requestId")
            val items = mutableListOf<RequestItemDetail>()
            
            try {
                // First, try to get items from the items map in the document
                val itemsMap = data["items"] as? Map<*, *>
                if (itemsMap != null && itemsMap.isNotEmpty()) {
                    Log.d("RequestsRepository", "Found items map in request document, count: ${itemsMap.size}")
                    // Items map: document ID -> quantity
                    itemsMap.forEach { (docId, quantity) ->
                        val documentId = docId as? String ?: return@forEach
                        val itemQuantity = when (quantity) {
                            is Long -> quantity.toInt()
                            is Int -> quantity
                            else -> 0
                        }
                        if (itemQuantity > 0) {
                            processItem(documentId, itemQuantity, items)
                        }
                    }
                } else {
                    // Fallback: Try to get items from subcollection (for backward compatibility)
                    Log.d("RequestsRepository", "No items map found, trying subcollection")
                    try {
                        val itemsSnapshot = doc.reference.collection("items").get().await()
                        Log.d("RequestsRepository", "Items subcollection fetched, count: ${itemsSnapshot.documents.size}")
                        
                        if (itemsSnapshot.documents.isNotEmpty()) {
                            // Process items from subcollection
                            for (itemDoc in itemsSnapshot.documents) {
                                val itemData = itemDoc.data ?: continue
                                val productDocId = itemData["productDocId"] as? String ?: continue
                                val quantity = (itemData["quantity"] as? Long)?.toInt() ?: 0
                                processItem(productDocId, quantity, items)
                            }
                        } else {
                            // Try legacy array format
                            val itemsArray = data["items"] as? List<Map<String, Any>>
                            if (itemsArray != null && itemsArray.isNotEmpty()) {
                                Log.d("RequestsRepository", "Found items array in request document, count: ${itemsArray.size}")
                                for (itemData in itemsArray) {
                                    val productDocId = itemData["productDocId"] as? String ?: continue
                                    val quantity = (itemData["quantity"] as? Long)?.toInt() ?: 0
                                    processItem(productDocId, quantity, items)
                                }
                            } else {
                                Log.w("RequestsRepository", "No items found in document map, subcollection, or array")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("RequestsRepository", "Error fetching items subcollection: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("RequestsRepository", "Error processing items: ${e.message}", e)
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
            
            // Log audit action
            val currentUser = authRepository.getCurrentUser()
            CoroutineScope(Dispatchers.IO).launch {
                auditRepository.logAction(
                    action = "accept_request",
                    userId = currentUser?.uid,
                    details = mapOf(
                        "requestId" to requestId,
                        "scheduledPickupDate" to scheduledPickupDate.toString()
                    )
                )
            }
            
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
            
            // Log audit action
            val currentUser = authRepository.getCurrentUser()
            CoroutineScope(Dispatchers.IO).launch {
                auditRepository.logAction(
                    action = "decline_request",
                    userId = currentUser?.uid,
                    details = mapOf(
                        "requestId" to requestId,
                        "reason" to (reason ?: "")
                    )
                )
            }
            
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

    override suspend fun getUserProfile(userId: String): Result<UserProfileData> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val data = userDoc.data ?: return Result.failure(Exception("User not found"))
            val name = data["name"] as? String ?: ""
            val email = data["email"] as? String ?: ""
            Result.success(UserProfileData(name = name, email = email))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun submitRequest(selectedItems: Map<String, Int>): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            val itemsMap = mutableMapOf<String, Int>()

            selectedItems.forEach { (productDocId, requestedQuantity) ->
                try {
                    // First check if it's a document ID
                    val itemDoc = firestore.collection("items").document(productDocId).get().await()
                    
                    if (itemDoc.exists()) {
                        val itemQuantity = ((itemDoc.data?.get("quantity") as? Long)?.toInt()
                            ?: (itemDoc.data?.get("quantity") as? Int) ?: 0)
                        if (itemQuantity > 0) {
                            itemsMap[productDocId] = minOf(requestedQuantity, itemQuantity)
                        }
                    } else {
                        // It's a productId/barcode, search for items by barcode or productId
                        // Try barcode first (most common case)
                        var itemsSnapshot = try {
                            firestore.collection("items")
                                .whereEqualTo("barcode", productDocId)
                                .get()
                                .await()
                        } catch (e: Exception) {
                            null
                        }
                        
                        // If not found by barcode, try productId
                        if (itemsSnapshot == null || itemsSnapshot.isEmpty) {
                            itemsSnapshot = try {
                                firestore.collection("items")
                                    .whereEqualTo("productId", productDocId)
                                    .get()
                                    .await()
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        if (itemsSnapshot == null || itemsSnapshot.isEmpty) {
                            return@forEach
                        }
                        
                        // Filter items with quantity > 0 and sort by expiry date
                        val availableItems = itemsSnapshot.documents.filter { doc ->
                            val itemQuantity = ((doc.data?.get("quantity") as? Long)?.toInt()
                                ?: (doc.data?.get("quantity") as? Int) ?: 0)
                            itemQuantity > 0
                        }
                        
                        if (availableItems.isEmpty()) {
                            return@forEach
                        }
                        
                        val sortedItems = availableItems.sortedWith(compareBy { doc ->
                            val expiryDate = (doc.data?.get("expiryDate") as? Timestamp)
                                ?: (doc.data?.get("expirationDate") as? Timestamp)
                            expiryDate?.toDate()?.time ?: Long.MAX_VALUE
                        })
                        
                        var remainingQuantity = requestedQuantity
                        for (itemDoc in sortedItems) {
                            if (remainingQuantity <= 0) break
                            
                            val itemQuantity = ((itemDoc.data?.get("quantity") as? Long)?.toInt()
                                ?: (itemDoc.data?.get("quantity") as? Int) ?: 0)
                            
                            val quantityToUse = minOf(remainingQuantity, itemQuantity)
                            itemsMap[itemDoc.id] = quantityToUse
                            remainingQuantity -= quantityToUse
                        }
                    }
                } catch (e: Exception) {
                    // Continue with other items
                }
            }

            if (itemsMap.isEmpty()) {
                return Result.failure(Exception("No items found to add to request. Selected items: ${selectedItems.keys.joinToString()}"))
            }

            val itemsMapForFirestore = HashMap<String, Any>().apply {
                itemsMap.forEach { (docId, quantity) ->
                    this[docId] = quantity.toLong()
                }
            }

            val requestsData = hashMapOf<String, Any>(
                "userId" to currentUser.uid,
                "status" to 0,
                "submissionDate" to FieldValue.serverTimestamp(),
                "totalItems" to selectedItems.values.sum(),
                "items" to itemsMapForFirestore
            )

            try {
                firestore.collection("requests")
                    .add(requestsData)
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("Failed to create request: ${e.message}", e))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error submitting request: ${e.message}", e))
        }
    }
}
