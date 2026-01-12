package com.lojasocial.app.repository.request

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lojasocial.app.domain.product.Product
import com.lojasocial.app.domain.product.ProductCategory
import com.lojasocial.app.domain.request.RequestItem
import com.lojasocial.app.repository.product.ProductRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ItemsRepositoryImpl"

// Internal data class to hold item information before grouping
private data class ItemData(
    val docId: String,
    val productId: String,
    val product: Product,
    val category: String,
    val quantity: Int,
    val availableQuantity: Int, // quantity - reservedQuantity
    val expiryDate: Timestamp?
)

@Singleton
class ItemsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val productRepository: ProductRepository
) : ItemsRepository {

    override suspend fun getProducts(pageSize: Int, lastVisibleId: String?): List<RequestItem> {
        return try {
            // Note: We can't filter by availableQuantity directly in Firestore since it's a calculated field
            // So we fetch items with quantity > 0 and filter out items with no available stock after fetching
            var query: Query = firestore.collection("items")
                .whereGreaterThan("quantity", 0)
                .orderBy("quantity")
                .limit(pageSize.toLong())

            if (lastVisibleId != null) {
                try {
                    val lastVisible = firestore.collection("items").document(lastVisibleId).get().await()
                    if (lastVisible.exists()) {
                        query = query.startAfter(lastVisible)
                        Log.d(TAG, "Using lastVisibleId as item document ID for pagination: $lastVisibleId")
                    } else {
                        Log.w(TAG, "lastVisibleId $lastVisibleId is not a valid item document ID, skipping pagination")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error using lastVisibleId for pagination: ${e.message}, skipping pagination")
                }
            }

            val snapshot = query.get().await()
            
            Log.d(TAG, "Fetched ${snapshot.documents.size} items from Firestore")
            
            // First, collect all items with their product data
            val itemsWithProducts = snapshot.documents.mapNotNull { document ->
                val data = document.data
                if (data == null) return@mapNotNull null
                
                // Get barcode from the item document (prefer barcode field, fallback to productId)
                val barcode = (data["barcode"] as? String)
                    ?: (data["productId"] as? String)
                    ?: return@mapNotNull null
                
                // Use barcode as productId for product lookup
                val productId = barcode
                
                // Fetch product information from products collection
                val product = productRepository.getProductByBarcodeId(productId)
                
                if (product == null) {
                    Log.w(TAG, "Product not found for productId: $productId")
                    return@mapNotNull null
                }
                
                // Convert Product.category (Int) to category string
                val categoryString = when (ProductCategory.fromId(product.category)) {
                    ProductCategory.ALIMENTAR -> "Alimentar"
                    ProductCategory.HIGIENE_PESSOAL -> "Higiene"
                    ProductCategory.CASA -> "Limpeza"
                    null -> "Geral"
                }
                
                // Handle expiration date - check both field names (expiryDate or expirationDate)
                val expiryDate = (data["expiryDate"] as? Timestamp) 
                    ?: (data["expirationDate"] as? Timestamp)
                
                val quantity = (data["quantity"] as? Long)?.toInt() ?: (data["quantity"] as? Int) ?: 0
                val reservedQuantity = (data["reservedQuantity"] as? Long)?.toInt() 
                    ?: (data["reservedQuantity"] as? Int) ?: 0
                val availableQuantity = quantity - reservedQuantity
                
                Log.d(TAG, "Item ${document.id}: quantity=$quantity, reservedQuantity=$reservedQuantity, availableQuantity=$availableQuantity")
                
                // Only include items with available stock > 0
                if (availableQuantity <= 0) {
                    Log.d(TAG, "Skipping item ${document.id} - no available stock")
                    return@mapNotNull null
                }
                
                // Return item data with barcode for grouping
                ItemData(
                    docId = document.id,
                    productId = barcode, // Use barcode as productId for grouping
                    product = product,
                    category = categoryString,
                    quantity = quantity,
                    availableQuantity = availableQuantity,
                    expiryDate = expiryDate
                )
            }
            
            // Store the last item document ID for pagination (before grouping)
            // This is the actual Firestore document ID, not the productId
            val lastItemDocId = snapshot.documents.lastOrNull()?.id
            
            Log.d(TAG, "Last item document ID for pagination: $lastItemDocId")
            
            // Group items by productId
            val groupedByProductId = itemsWithProducts.groupBy { it.productId }
            
            Log.d(TAG, "Grouped ${itemsWithProducts.size} items into ${groupedByProductId.size} products")
            
            // For each productId group, create a single RequestItem with aggregated data
            val requestItems = groupedByProductId.map { (productId, items) ->
                // Sort items by expiry date: nearest first, then items without expiry date
                val sortedItems = items.sortedWith(compareBy<ItemData> { item ->
                    // Items with expiry date come first, sorted by date (earliest first)
                    item.expiryDate?.toDate()?.time ?: Long.MAX_VALUE
                })
                
                // Calculate total available quantity (not total physical quantity)
                val totalAvailableQuantity = items.sumOf { it.availableQuantity }
                val totalQuantity = items.sumOf { it.quantity }
                val totalReserved = items.sumOf { it.quantity - it.availableQuantity }
                
                Log.d(TAG, "Product $productId (${items.first().product.name}): totalQuantity=$totalQuantity, totalReserved=$totalReserved, totalAvailableQuantity=$totalAvailableQuantity (from ${items.size} item documents)")
                
                // Get the nearest expiry date (first item in sorted list)
                val nearestExpiryDate = sortedItems.firstOrNull()?.expiryDate
                
                // Use productId as docId (this will be handled specially in RequestsRepository)
                // Store lastItemDocId in a special format: "productId|lastDocId" for pagination
                // We'll extract it in the ViewModel
                val docIdWithPagination = if (lastItemDocId != null && productId == groupedByProductId.keys.lastOrNull()) {
                    "$productId|$lastItemDocId" // Only store pagination info in the last item
                } else {
                    productId
                }
                
                RequestItem(
                    docId = docIdWithPagination,
                    id = 0,
                    name = items.first().product.name,
                    category = items.first().category,
                    quantity = totalAvailableQuantity, // This represents available stock
                    expiryDate = nearestExpiryDate,
                    barcode = productId // Store barcode for merging in ViewModel
                )
            }
            
            requestItems
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching products: ${e.message}", e)
            emptyList()
        }
    }
}
