package com.lojasocial.app.repository.request

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.AggregateSource
import com.lojasocial.app.domain.request.Request
import com.lojasocial.app.domain.request.RequestItemDetail
import com.lojasocial.app.repository.audit.AuditRepository
import com.lojasocial.app.repository.auth.AuthRepository
import com.lojasocial.app.repository.product.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
     * Optimized to batch fetch all item documents and products in parallel.
     */
    private suspend fun getRequestCategory(itemsMap: Map<*, *>?): Int? {
        if (itemsMap == null || itemsMap.isEmpty()) return null
        
        return try {
            val itemDocIds = itemsMap.keys.mapNotNull { it as? String }
            if (itemDocIds.isEmpty()) return null
            
            // Batch fetch all item documents in parallel using coroutines
            val itemDocs = coroutineScope {
                itemDocIds.map { itemDocId ->
                    async {
                        firestore.collection("items").document(itemDocId).get().await()
                    }
                }.awaitAll()
            }
            
            // Extract all barcodes/productIds
            val barcodes = itemDocs.mapNotNull { doc ->
                if (!doc.exists()) return@mapNotNull null
                val itemData = doc.data
                (itemData?.get("barcode") as? String) 
                    ?: (itemData?.get("productId") as? String)
                    ?: itemData?.get("productId")?.toString()
            }
            
            // Batch fetch all products in parallel (deduplicated)
            val uniqueBarcodes = barcodes.distinct()
            val products = coroutineScope {
                uniqueBarcodes.map { barcode ->
                    async {
                        productRepository.getProductByBarcodeId(barcode)
                    }
                }.awaitAll()
            }
            
            // Create a map for quick lookup
            val barcodeToCategory = uniqueBarcodes.zip(products).associate { (barcode, product) ->
                barcode to (product?.category ?: 1)
            }
            
            // Get categories for all items
            val categories = barcodes.mapNotNull { barcode ->
                barcodeToCategory[barcode] ?: 1
            }.toSet()
            
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
                    val proposedDeliveryDate = convertToDate(data["proposedDeliveryDate"])
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
                        proposedDeliveryDate = proposedDeliveryDate,
                        rejectionReason = rejectionReason,
                        items = minimalItems
                    )
                } catch (e: Exception) {
                    Log.e("RequestsRepository", "Error parsing document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            // Load categories for requests with items in parallel
            val requestsWithCategories = coroutineScope {
                requests.map { request ->
                    async {
                        if (request.items.isNotEmpty()) {
                            val doc = snapshot.documents.find { it.id == request.id }
                            val itemsMap = doc?.data?.get("items") as? Map<*, *>
                            if (itemsMap != null && itemsMap.isNotEmpty()) {
                                val requestCategory = getRequestCategory(itemsMap)
                                // Use the category if all items have the same category, otherwise use 0 to indicate "Vários"
                                val categoryToUse = requestCategory ?: 0 // 0 means multiple categories
                                request.copy(
                                    items = listOf(
                                        request.items.first().copy(category = categoryToUse)
                                    )
                                )
                            } else {
                                request
                            }
                        } else {
                            request
                        }
                    }
                }.awaitAll()
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
                    val proposedDeliveryDate = convertToDate(data["proposedDeliveryDate"])
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
                        proposedDeliveryDate = proposedDeliveryDate,
                        rejectionReason = rejectionReason,
                        items = minimalItems
                    )
                } catch (e: Exception) {
                    Log.e("RequestsRepository", "Error parsing document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            // Load categories for requests with items in parallel
            val requestsWithCategories = coroutineScope {
                requests.map { request ->
                    async {
                        if (request.items.isNotEmpty()) {
                            val doc = snapshot.documents.find { it.id == request.id }
                            val itemsMap = doc?.data?.get("items") as? Map<*, *>
                            if (itemsMap != null && itemsMap.isNotEmpty()) {
                                val requestCategory = getRequestCategory(itemsMap)
                                // Use the category if all items have the same category, otherwise use 0 to indicate "Vários"
                                val categoryToUse = requestCategory ?: 0 // 0 means multiple categories
                                request.copy(
                                    items = listOf(
                                        request.items.first().copy(category = categoryToUse)
                                    )
                                )
                            } else {
                                request
                            }
                        } else {
                            request
                        }
                    }
                }.awaitAll()
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

    override suspend fun getRequestsByUserId(userId: String): Result<List<Request>> {
        return try {
            // Filter at database level using whereEqualTo
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
                    val proposedDeliveryDate = convertToDate(data["proposedDeliveryDate"])
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
                        proposedDeliveryDate = proposedDeliveryDate,
                        rejectionReason = rejectionReason,
                        items = minimalItems
                    )
                } catch (e: Exception) {
                    Log.e("RequestsRepository", "Error parsing document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            // Load categories for requests with items in parallel
            val requestsWithCategories = coroutineScope {
                requests.map { request ->
                    async {
                        if (request.items.isNotEmpty()) {
                            val doc = snapshot.documents.find { it.id == request.id }
                            val itemsMap = doc?.data?.get("items") as? Map<*, *>
                            if (itemsMap != null && itemsMap.isNotEmpty()) {
                                val requestCategory = getRequestCategory(itemsMap)
                                // Use the category if all items have the same category, otherwise use 0 to indicate "Vários"
                                val categoryToUse = requestCategory ?: 0 // 0 means multiple categories
                                request.copy(
                                    items = listOf(
                                        request.items.first().copy(category = categoryToUse)
                                    )
                                )
                            } else {
                                request
                            }
                        } else {
                            request
                        }
                    }
                }.awaitAll()
            }

            // Sort manually if we couldn't use orderBy
            val sortedRequests = if (snapshot.documents.isEmpty() || 
                snapshot.documents.firstOrNull()?.data?.get("submissionDate") != null) {
                requestsWithCategories.sortedByDescending { it.submissionDate ?: Date(0) }
            } else {
                requestsWithCategories
            }

            Log.d("RequestsRepository", "Fetched ${sortedRequests.size} requests for user $userId")
            Result.success(sortedRequests)
        } catch (e: Exception) {
            Log.e("RequestsRepository", "Error fetching requests by userId: ${e.message}", e)
            Result.failure(e)
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
                    val proposedDeliveryDate = convertToDate(data["proposedDeliveryDate"])
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
                        proposedDeliveryDate = proposedDeliveryDate,
                        rejectionReason = rejectionReason,
                        items = minimalItems
                    )
                } catch (e: Exception) {
                    Log.e("RequestsRepository", "Error parsing document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            // Load categories for requests with items in parallel
            val requestsWithCategories = coroutineScope {
                requests.map { request ->
                    async {
                        if (request.items.isNotEmpty()) {
                            val doc = documentsToProcess.find { it.id == request.id }
                            val itemsMap = doc?.data?.get("items") as? Map<*, *>
                            if (itemsMap != null && itemsMap.isNotEmpty()) {
                                val requestCategory = getRequestCategory(itemsMap)
                                // Use the category if all items have the same category, otherwise use 0 to indicate "Vários"
                                val categoryToUse = requestCategory ?: 0 // 0 means multiple categories
                                request.copy(
                                    items = listOf(
                                        request.items.first().copy(category = categoryToUse)
                                    )
                                )
                            } else {
                                request
                            }
                        } else {
                            request
                        }
                    }
                }.awaitAll()
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
            val proposedDeliveryDate = convertToDate(data["proposedDeliveryDate"])
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
                proposedDeliveryDate = proposedDeliveryDate,
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
            // First, get the request to retrieve items
            val requestDoc = firestore.collection("requests").document(requestId).get().await()
            if (!requestDoc.exists()) {
                return Result.failure(Exception("Request not found"))
            }
            
            val requestData = requestDoc.data ?: return Result.failure(Exception("Request data not found"))
            val itemsMap = requestData["items"] as? Map<*, *>
            
            // Release reservations using transaction
            if (itemsMap != null && itemsMap.isNotEmpty()) {
                firestore.runTransaction { transaction ->
                    // PHASE 1: Read all item documents first
                    val itemData = itemsMap.mapNotNull { (itemDocIdObj, quantityObj) ->
                        val itemDocId = itemDocIdObj as? String ?: return@mapNotNull null
                        val reservedQty = when (quantityObj) {
                            is Long -> quantityObj.toInt()
                            is Int -> quantityObj
                            else -> 0
                        }
                        
                        if (reservedQty > 0) {
                            val itemRef = firestore.collection("items").document(itemDocId)
                            val itemDoc = transaction.get(itemRef)
                            
                            if (itemDoc.exists()) {
                                val currentReserved = (itemDoc.getLong("reservedQuantity")?.toInt()
                                    ?: (itemDoc.get("reservedQuantity") as? Int) ?: 0)
                                val newReserved = maxOf(0, currentReserved - reservedQty)
                                Triple(itemRef, newReserved, itemDocId)
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }
                    
                    // PHASE 2: Now do all writes
                    itemData.forEach { (itemRef, newReserved, _) ->
                        transaction.update(itemRef, "reservedQuantity", newReserved)
                    }
                }.await()
            }
            
            // Update request status
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
            Log.e("RequestsRepository", "Error rejecting request: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun proposeNewDate(requestId: String, proposedDate: Date): Result<Unit> {
        return try {
            val timestamp = Timestamp(proposedDate)
            firestore.collection("requests").document(requestId)
                .update("scheduledPickupDate", timestamp)
                .await()
            
            // Log audit action
            val currentUser = authRepository.getCurrentUser()
            CoroutineScope(Dispatchers.IO).launch {
                auditRepository.logAction(
                    action = "propose_new_date",
                    userId = currentUser?.uid,
                    details = mapOf(
                        "requestId" to requestId,
                        "proposedDate" to proposedDate.toString()
                    )
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun proposeNewDeliveryDate(requestId: String, proposedDate: Date): Result<Unit> {
        return try {
            val timestamp = Timestamp(proposedDate)
            // Update proposedDeliveryDate and clear scheduledPickupDate to reset negotiation
            firestore.collection("requests").document(requestId)
                .update(
                    mapOf(
                        "proposedDeliveryDate" to timestamp,
                        "scheduledPickupDate" to FieldValue.delete()
                    )
                )
                .await()
            
            // Log audit action
            val currentUser = authRepository.getCurrentUser()
            CoroutineScope(Dispatchers.IO).launch {
                auditRepository.logAction(
                    action = "propose_new_delivery_date",
                    userId = currentUser?.uid,
                    details = mapOf(
                        "requestId" to requestId,
                        "proposedDate" to proposedDate.toString()
                    )
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptEmployeeProposedDate(requestId: String): Result<Unit> {
        return try {
            // Get the request to retrieve scheduledPickupDate
            val requestDoc = firestore.collection("requests").document(requestId).get().await()
            if (!requestDoc.exists()) {
                return Result.failure(Exception("Request not found"))
            }
            
            val scheduledPickupDate = requestDoc.getTimestamp("scheduledPickupDate")
            if (scheduledPickupDate == null) {
                return Result.failure(Exception("No scheduled pickup date found"))
            }
            
            // Update status to PENDENTE_LEVANTAMENTO
            firestore.collection("requests").document(requestId)
                .update("status", 1) // PENDENTE_LEVANTAMENTO
                .await()
            
            // Log audit action
            val currentUser = authRepository.getCurrentUser()
            CoroutineScope(Dispatchers.IO).launch {
                auditRepository.logAction(
                    action = "accept_employee_proposed_date",
                    userId = currentUser?.uid,
                    details = mapOf(
                        "requestId" to requestId,
                        "scheduledPickupDate" to scheduledPickupDate.toDate().toString()
                    )
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPendingRequestsCount(): Result<Int> {
        return try {
            // Use a count aggregation query to efficiently get the total number of pending requests
            // This only counts documents without fetching their data
            val query = firestore.collection("requests")
                .whereEqualTo("status", 0) // SUBMETIDO
            
            val aggregateQuery = query.count()
            val snapshot = aggregateQuery.get(AggregateSource.SERVER).await()
            
            val count = snapshot.count.toInt()
            Result.success(count)
        } catch (e: Exception) {
            Log.e("RequestsRepository", "Error getting pending requests count: ${e.message}", e)
            // Fallback: if count aggregation is not available, use a lightweight query
            try {
                val snapshot = firestore.collection("requests")
                    .whereEqualTo("status", 0)
                    .limit(1000) // Reasonable limit for counting
                    .get()
                    .await()
                Result.success(snapshot.size())
            } catch (fallbackError: Exception) {
                Result.failure(fallbackError)
            }
        }
    }

    override suspend fun completeRequest(requestId: String): Result<Unit> {
        return try {
            // First, get the request to retrieve items
            val requestDoc = firestore.collection("requests").document(requestId).get().await()
            if (!requestDoc.exists()) {
                return Result.failure(Exception("Request not found"))
            }
            
            val requestData = requestDoc.data ?: return Result.failure(Exception("Request data not found"))
            val itemsMap = requestData["items"] as? Map<*, *>
            
            // Validate that request is in a state that can be completed
            val currentStatus = (requestData["status"] as? Long)?.toInt() ?: (requestData["status"] as? Int) ?: 0
            if (currentStatus != 1) { // Only PENDENTE_LEVANTAMENTO (1) can be completed
                return Result.failure(Exception("Request must be in PENDENTE_LEVANTAMENTO status to be completed"))
            }
            
            // Decrease quantity and reservedQuantity using transaction
            // The itemsMap contains the exact quantities reserved for THIS specific request
            if (itemsMap != null && itemsMap.isNotEmpty()) {
                firestore.runTransaction { transaction ->
                    // PHASE 1: Read all item documents first and validate
                    val itemData = itemsMap.mapNotNull { (itemDocIdObj, quantityObj) ->
                        val itemDocId = itemDocIdObj as? String ?: return@mapNotNull null
                        val quantityToDeduct = when (quantityObj) {
                            is Long -> quantityObj.toInt()
                            is Int -> quantityObj
                            else -> 0
                        }
                        
                        if (quantityToDeduct > 0) {
                            val itemRef = firestore.collection("items").document(itemDocId)
                            val itemDoc = transaction.get(itemRef)
                            
                            if (itemDoc.exists()) {
                                val currentQuantity = (itemDoc.getLong("quantity")?.toInt()
                                    ?: (itemDoc.get("quantity") as? Int) ?: 0)
                                val currentReserved = (itemDoc.getLong("reservedQuantity")?.toInt()
                                    ?: (itemDoc.get("reservedQuantity") as? Int) ?: 0)
                                
                                // Safety check: ensure we don't deduct more than what's reserved
                                // This protects against edge cases where reservedQuantity might be less than expected
                                val actualDeduction = minOf(quantityToDeduct, currentReserved)
                                
                                // Calculate new values
                                val newQuantity = maxOf(0, currentQuantity - quantityToDeduct)
                                // Only deduct what's actually reserved (protects other requests' reservations)
                                val newReserved = maxOf(0, currentReserved - actualDeduction)
                                
                                // Log warning if there's a mismatch (shouldn't happen in normal flow)
                                if (actualDeduction < quantityToDeduct) {
                                    Log.w("RequestsRepository", "Warning: Attempting to deduct $quantityToDeduct but only $currentReserved is reserved for item $itemDocId in request $requestId")
                                }
                                
                                Triple(itemRef, newQuantity, newReserved)
                            } else {
                                Log.w("RequestsRepository", "Item document $itemDocId not found when completing request $requestId")
                                null
                            }
                        } else {
                            null
                        }
                    }
                    
                    // PHASE 2: Now do all writes
                    itemData.forEach { (itemRef, newQuantity, newReserved) ->
                        transaction.update(itemRef, mapOf(
                            "quantity" to newQuantity,
                            "reservedQuantity" to newReserved
                        ))
                    }
                }.await()
            }
            
            // Update request status to CONCLUIDO (2)
            firestore.collection("requests").document(requestId)
                .update("status", 2) // CONCLUIDO
                .await()
            
            // Log audit action
            val currentUser = authRepository.getCurrentUser()
            CoroutineScope(Dispatchers.IO).launch {
                auditRepository.logAction(
                    action = "complete_request",
                    userId = currentUser?.uid,
                    details = mapOf(
                        "requestId" to requestId
                    )
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RequestsRepository", "Error completing request: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun cancelDelivery(requestId: String, beneficiaryAbsent: Boolean): Result<Unit> {
        return try {
            // First, get the request to retrieve items
            val requestDoc = firestore.collection("requests").document(requestId).get().await()
            if (!requestDoc.exists()) {
                return Result.failure(Exception("Request not found"))
            }
            
            val requestData = requestDoc.data ?: return Result.failure(Exception("Request data not found"))
            val itemsMap = requestData["items"] as? Map<*, *>
            val userId = requestData["userId"] as? String
            
            // Validate that request is in a state that can be cancelled
            val currentStatus = (requestData["status"] as? Long)?.toInt() ?: (requestData["status"] as? Int) ?: 0
            if (currentStatus != 1) { // Only PENDENTE_LEVANTAMENTO (1) can be cancelled
                return Result.failure(Exception("Request must be in PENDENTE_LEVANTAMENTO status to be cancelled"))
            }
            
            // Release reserved quantities (decrease reservedQuantity) but do NOT decrease actual quantity
            // The itemsMap contains the exact quantities reserved for THIS specific request
            if (itemsMap != null && itemsMap.isNotEmpty()) {
                firestore.runTransaction { transaction ->
                    // PHASE 1: Read all item documents first and validate
                    val itemData = itemsMap.mapNotNull { (itemDocIdObj, quantityObj) ->
                        val itemDocId = itemDocIdObj as? String ?: return@mapNotNull null
                        val quantityToRelease = when (quantityObj) {
                            is Long -> quantityObj.toInt()
                            is Int -> quantityObj
                            else -> 0
                        }
                        
                        if (quantityToRelease > 0) {
                            val itemRef = firestore.collection("items").document(itemDocId)
                            val itemDoc = transaction.get(itemRef)
                            
                            if (itemDoc.exists()) {
                                val currentReserved = (itemDoc.getLong("reservedQuantity")?.toInt()
                                    ?: (itemDoc.get("reservedQuantity") as? Int) ?: 0)
                                
                                // Safety check: ensure we don't release more than what's reserved
                                val actualRelease = minOf(quantityToRelease, currentReserved)
                                
                                // Only decrease reservedQuantity, NOT actual quantity
                                val newReserved = maxOf(0, currentReserved - actualRelease)
                                
                                // Log warning if there's a mismatch (shouldn't happen in normal flow)
                                if (actualRelease < quantityToRelease) {
                                    Log.w("RequestsRepository", "Warning: Attempting to release $quantityToRelease but only $currentReserved is reserved for item $itemDocId in request $requestId")
                                }
                                
                                Pair(itemRef, newReserved)
                            } else {
                                Log.w("RequestsRepository", "Item document $itemDocId not found when cancelling request $requestId")
                                null
                            }
                        } else {
                            null
                        }
                    }
                    
                    // PHASE 2: Now do all writes - only update reservedQuantity, NOT quantity
                    itemData.forEach { (itemRef, newReserved) ->
                        transaction.update(itemRef, "reservedQuantity", newReserved)
                    }
                }.await()
            }
            
            // Update request status to CANCELADO (3)
            firestore.collection("requests").document(requestId)
                .update("status", 3) // CANCELADO
                .await()
            
            // If beneficiary was absent, add the date to the absence array
            if (beneficiaryAbsent && userId != null) {
                try {
                    val userRef = firestore.collection("users").document(userId)
                    val userDoc = userRef.get().await()
                    
                    if (userDoc.exists()) {
                        // Get current absence array or create empty list
                        val currentAbsences = userDoc.get("absence") as? List<*>
                        val absenceList = mutableListOf<Any>()
                        
                        // Add existing absences to the list (preserve them, filter out nulls)
                        currentAbsences?.forEach { item ->
                            if (item != null) {
                                absenceList.add(item)
                            }
                        }
                        
                        // Add current date to the array
                        val currentDate = Timestamp.now()
                        absenceList.add(currentDate)
                        
                        // Update user document with the new absence array
                        userRef.update("absence", absenceList).await()
                        Log.d("RequestsRepository", "Added absence date for user $userId. Total absences: ${absenceList.size}")
                    } else {
                        Log.w("RequestsRepository", "User document $userId not found when adding absence")
                    }
                } catch (e: Exception) {
                    Log.e("RequestsRepository", "Error adding absence date: ${e.message}", e)
                    // Don't fail the entire operation if absence update fails
                }
            }
            
            // Log audit action
            val currentUser = authRepository.getCurrentUser()
            CoroutineScope(Dispatchers.IO).launch {
                auditRepository.logAction(
                    action = "cancel_delivery",
                    userId = currentUser?.uid,
                    details = mapOf(
                        "requestId" to requestId,
                        "beneficiaryAbsent" to beneficiaryAbsent
                    )
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RequestsRepository", "Error cancelling delivery: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun rescheduleDelivery(requestId: String, newDate: Date, isEmployeeRescheduling: Boolean): Result<Unit> {
        return try {
            // First, get the request to validate status
            val requestDoc = firestore.collection("requests").document(requestId).get().await()
            if (!requestDoc.exists()) {
                return Result.failure(Exception("Request not found"))
            }
            
            val requestData = requestDoc.data ?: return Result.failure(Exception("Request data not found"))
            
            // Validate that request is in PENDENTE_LEVANTAMENTO status
            val currentStatus = (requestData["status"] as? Long)?.toInt() ?: (requestData["status"] as? Int) ?: 0
            if (currentStatus != 1) { // Only PENDENTE_LEVANTAMENTO (1) can be rescheduled
                return Result.failure(Exception("Request must be in PENDENTE_LEVANTAMENTO status to be rescheduled"))
            }
            
            val timestamp = Timestamp(newDate)
            // Change status back to SUBMETIDO (0) and set the appropriate date field based on who is rescheduling
            val updateData = mutableMapOf<String, Any>(
                "status" to 0 // SUBMETIDO
            )
            
            if (isEmployeeRescheduling) {
                // Employee rescheduling: set scheduledPickupDate (employee proposal) and clear proposedDeliveryDate
                updateData["scheduledPickupDate"] = timestamp
                updateData["proposedDeliveryDate"] = FieldValue.delete()
            } else {
                // Beneficiary rescheduling: set proposedDeliveryDate (beneficiary proposal) and clear scheduledPickupDate
                updateData["proposedDeliveryDate"] = timestamp
                updateData["scheduledPickupDate"] = FieldValue.delete()
            }
            
            firestore.collection("requests").document(requestId)
                .update(updateData)
                .await()
            
            // Log audit action
            val currentUser = authRepository.getCurrentUser()
            CoroutineScope(Dispatchers.IO).launch {
                auditRepository.logAction(
                    action = "reschedule_delivery",
                    userId = currentUser?.uid,
                    details = mapOf(
                        "requestId" to requestId,
                        "newDate" to newDate.toString(),
                        "isEmployeeRescheduling" to isEmployeeRescheduling
                    )
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RequestsRepository", "Error rescheduling delivery: ${e.message}", e)
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

    override suspend fun submitRequest(selectedItems: Map<String, Int>, proposedDeliveryDate: Date?): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            // First, collect item document IDs and quantities to reserve
            val itemsToReserve = mutableMapOf<String, Int>()

            selectedItems.forEach { (productDocId, requestedQuantity) ->
                try {
                    // First check if it's a document ID
                    val itemDoc = firestore.collection("items").document(productDocId).get().await()
                    
                    if (itemDoc.exists()) {
                        val itemQuantity = ((itemDoc.data?.get("quantity") as? Long)?.toInt()
                            ?: (itemDoc.data?.get("quantity") as? Int) ?: 0)
                        val reservedQuantity = ((itemDoc.data?.get("reservedQuantity") as? Long)?.toInt()
                            ?: (itemDoc.data?.get("reservedQuantity") as? Int) ?: 0)
                        val available = itemQuantity - reservedQuantity
                        
                        if (available > 0) {
                            itemsToReserve[productDocId] = minOf(requestedQuantity, available)
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
                        
                        // Filter items with available stock > 0 and sort by expiry date
                        val availableItems = itemsSnapshot.documents.mapNotNull { doc ->
                            val itemQuantity = ((doc.data?.get("quantity") as? Long)?.toInt()
                                ?: (doc.data?.get("quantity") as? Int) ?: 0)
                            val reservedQuantity = ((doc.data?.get("reservedQuantity") as? Long)?.toInt()
                                ?: (doc.data?.get("reservedQuantity") as? Int) ?: 0)
                            val available = itemQuantity - reservedQuantity
                            
                            if (available > 0) {
                                val expiryDate = (doc.data?.get("expiryDate") as? Timestamp)
                                    ?: (doc.data?.get("expirationDate") as? Timestamp)
                                Triple(doc.id, available, expiryDate?.toDate()?.time ?: Long.MAX_VALUE)
                            } else {
                                null
                            }
                        }
                        
                        if (availableItems.isEmpty()) {
                            return@forEach
                        }
                        
                        // Sort by expiry date (earliest first)
                        val sortedItems = availableItems.sortedBy { it.third }
                        
                        var remainingQuantity = requestedQuantity
                        for ((docId, available, _) in sortedItems) {
                            if (remainingQuantity <= 0) break
                            
                            val quantityToUse = minOf(remainingQuantity, available)
                            itemsToReserve[docId] = quantityToUse
                            remainingQuantity -= quantityToUse
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RequestsRepository", "Error processing item $productDocId: ${e.message}", e)
                    // Continue with other items
                }
            }

            if (itemsToReserve.isEmpty()) {
                return Result.failure(Exception("No items available to add to request. Selected items: ${selectedItems.keys.joinToString()}"))
            }

            // Use Firestore transaction to atomically reserve quantities
            try {
                val verifiedItems = firestore.runTransaction { transaction ->
                    // PHASE 1: Read all item documents first
                    val itemRefs = itemsToReserve.keys.map { itemDocId ->
                        itemDocId to firestore.collection("items").document(itemDocId)
                    }
                    
                    val itemData = itemRefs.mapNotNull { (itemDocId, itemRef) ->
                        val itemDoc = transaction.get(itemRef)
                        
                        if (!itemDoc.exists()) {
                            throw Exception("Item document $itemDocId not found")
                        }
                        
                        val currentQuantity = (itemDoc.getLong("quantity")?.toInt()
                            ?: (itemDoc.get("quantity") as? Int) ?: 0)
                        val currentReserved = (itemDoc.getLong("reservedQuantity")?.toInt()
                            ?: (itemDoc.get("reservedQuantity") as? Int) ?: 0)
                        val requestedQty = itemsToReserve[itemDocId] ?: 0
                        val available = currentQuantity - currentReserved
                        
                        if (available < requestedQty) {
                            throw Exception("Insufficient stock for item $itemDocId. Available: $available, Requested: $requestedQty")
                        }
                        
                        Triple(itemDocId, itemRef, currentReserved + requestedQty)
                    }
                    
                    // PHASE 2: Now do all writes
                    val items = mutableMapOf<String, Int>()
                    itemData.forEach { (itemDocId, itemRef, newReservedQty) ->
                        transaction.update(itemRef, "reservedQuantity", newReservedQty)
                        items[itemDocId] = itemsToReserve[itemDocId] ?: 0
                    }
                    
                    items
                }.await()
                
                // Create request document after successful reservation
                val itemsMapForFirestore = HashMap<String, Any>().apply {
                    verifiedItems.forEach { (docId, quantity) ->
                        this[docId] = quantity.toLong()
                    }
                }
                
                val requestsData = hashMapOf<String, Any>(
                    "userId" to currentUser.uid,
                    "status" to 0,
                    "submissionDate" to FieldValue.serverTimestamp(),
                    "totalItems" to verifiedItems.values.sum(),
                    "items" to itemsMapForFirestore
                )
                
                // Add proposed delivery date if provided
                proposedDeliveryDate?.let { date ->
                    requestsData["proposedDeliveryDate"] = Timestamp(date)
                }
                
                firestore.collection("requests")
                    .add(requestsData)
                    .await()
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("RequestsRepository", "Transaction failed: ${e.message}", e)
                Result.failure(Exception("Failed to reserve items: ${e.message}", e))
            }
        } catch (e: Exception) {
            Log.e("RequestsRepository", "Error submitting request: ${e.message}", e)
            Result.failure(Exception("Error submitting request: ${e.message}", e))
        }
    }

    override suspend fun createUrgentRequest(userId: String, selectedItems: Map<String, Int>): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            if (selectedItems.isEmpty()) {
                return Result.failure(Exception("No items selected"))
            }

            // Validate items and calculate quantities to deduct
            // First, collect item document IDs and quantities
            val itemsToProcess = mutableMapOf<String, Int>()

            selectedItems.forEach { (productDocId, requestedQuantity) ->
                try {
                    // First check if it's a document ID
                    val itemDoc = firestore.collection("items").document(productDocId).get().await()
                    
                    if (itemDoc.exists()) {
                        val itemQuantity = ((itemDoc.data?.get("quantity") as? Long)?.toInt()
                            ?: (itemDoc.data?.get("quantity") as? Int) ?: 0)
                        val reservedQuantity = ((itemDoc.data?.get("reservedQuantity") as? Long)?.toInt()
                            ?: (itemDoc.data?.get("reservedQuantity") as? Int) ?: 0)
                        val available = itemQuantity - reservedQuantity
                        
                        if (available > 0) {
                            itemsToProcess[productDocId] = minOf(requestedQuantity, available)
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
                            Log.w("RequestsRepository", "Item $productDocId not found by document ID, barcode, or productId")
                            return@forEach
                        }
                        
                        // Filter items with available stock > 0 and sort by expiry date
                        val availableItems = itemsSnapshot.documents.mapNotNull { doc ->
                            val itemQuantity = ((doc.data?.get("quantity") as? Long)?.toInt()
                                ?: (doc.data?.get("quantity") as? Int) ?: 0)
                            val reservedQuantity = ((doc.data?.get("reservedQuantity") as? Long)?.toInt()
                                ?: (doc.data?.get("reservedQuantity") as? Int) ?: 0)
                            val available = itemQuantity - reservedQuantity
                            
                            if (available > 0) {
                                val expiryDate = (doc.data?.get("expiryDate") as? Timestamp)
                                    ?: (doc.data?.get("expirationDate") as? Timestamp)
                                Triple(doc.id, available, expiryDate?.toDate()?.time ?: Long.MAX_VALUE)
                            } else {
                                null
                            }
                        }
                        
                        if (availableItems.isEmpty()) {
                            Log.w("RequestsRepository", "No available stock for item $productDocId")
                            return@forEach
                        }
                        
                        // Sort by expiry date (earliest first)
                        val sortedItems = availableItems.sortedBy { it.third }
                        
                        var remainingQuantity = requestedQuantity
                        for ((docId, available, _) in sortedItems) {
                            if (remainingQuantity <= 0) break
                            
                            val quantityToUse = minOf(remainingQuantity, available)
                            itemsToProcess[docId] = quantityToUse
                            remainingQuantity -= quantityToUse
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RequestsRepository", "Error processing item $productDocId: ${e.message}", e)
                    // Continue with other items
                }
            }

            if (itemsToProcess.isEmpty()) {
                return Result.failure(Exception("No items available to process. Selected items: ${selectedItems.keys.joinToString()}"))
            }

            val verifiedItems = itemsToProcess

            if (verifiedItems.isEmpty()) {
                return Result.failure(Exception("No valid items to process"))
            }

            // Generate request ID before transaction
            val requestRef = firestore.collection("requests").document()
            val requestId = requestRef.id
            
            // Use transaction to decrease quantities and create request
            firestore.runTransaction { transaction ->
                // PHASE 1: Read all item documents and validate
                val itemData = verifiedItems.mapNotNull { (itemDocId, quantityToDeduct) ->
                    val itemRef = firestore.collection("items").document(itemDocId)
                    val itemDoc = transaction.get(itemRef)
                    
                    if (itemDoc.exists()) {
                        val data = itemDoc.data ?: return@mapNotNull null
                        val currentQuantity = (data["quantity"] as? Long)?.toInt()
                            ?: (data["quantity"] as? Int) ?: 0
                        val currentReserved = (data["reservedQuantity"] as? Long)?.toInt()
                            ?: (data["reservedQuantity"] as? Int) ?: 0
                        
                        if (currentQuantity < quantityToDeduct) {
                            throw Exception("Insufficient quantity for item ${data["name"] ?: itemDocId}")
                        }
                        
                        val newQuantity = maxOf(0, currentQuantity - quantityToDeduct)
                        val newReserved = maxOf(0, currentReserved - quantityToDeduct)
                        
                        Triple(itemRef, newQuantity, newReserved)
                    } else {
                        null
                    }
                }
                
                // PHASE 2: Update all items
                itemData.forEach { (itemRef, newQuantity, newReserved) ->
                    transaction.update(itemRef, mapOf(
                        "quantity" to newQuantity,
                        "reservedQuantity" to newReserved
                    ))
                }
                
                // PHASE 3: Create request document with CONCLUIDO status
                val itemsMapForFirestore = HashMap<String, Any>().apply {
                    verifiedItems.forEach { (docId, quantity) ->
                        this[docId] = quantity.toLong()
                    }
                }
                
                val submissionTimestamp = FieldValue.serverTimestamp()
                val requestsData = hashMapOf<String, Any>(
                    "userId" to userId,
                    "status" to 2, // CONCLUIDO
                    "submissionDate" to submissionTimestamp,
                    "scheduledPickupDate" to submissionTimestamp, // Data de recolha = data de submissão
                    "totalItems" to verifiedItems.values.sum(),
                    "items" to itemsMapForFirestore
                )
                
                transaction.set(requestRef, requestsData)
                requestId // Return the document ID
            }.await()
            
            // Log audit action
            CoroutineScope(Dispatchers.IO).launch {
                auditRepository.logAction(
                    action = "create_urgent_request",
                    userId = currentUser.uid,
                    details = mapOf(
                        "beneficiaryUserId" to userId,
                        "itemsCount" to verifiedItems.size,
                        "requestId" to requestId
                    )
                )
            }
            
            Result.success(requestId)
        } catch (e: Exception) {
            Log.e("RequestsRepository", "Error creating urgent request: ${e.message}", e)
            Result.failure(Exception("Error creating urgent request: ${e.message}", e))
        }
    }
}
