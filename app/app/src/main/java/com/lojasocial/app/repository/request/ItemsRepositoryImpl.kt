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

// Internal data class to hold item information before grouping
private data class ItemData(
    val docId: String,
    val productId: String,
    val product: Product,
    val category: String,
    val quantity: Int,
    val expiryDate: Timestamp?
)

@Singleton
class ItemsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val productRepository: ProductRepository
) : ItemsRepository {

    override suspend fun getProducts(pageSize: Int, lastVisibleId: String?): List<RequestItem> {
        return try {
            var query: Query = firestore.collection("items")
                .whereGreaterThan("quantity", 0)
                .orderBy("quantity")
                .limit(pageSize.toLong())

            if (lastVisibleId != null) {
                val lastVisible = firestore.collection("items").document(lastVisibleId).get().await()
                query = query.startAfter(lastVisible)
            }

            val snapshot = query.get().await()
            
            // First, collect all items with their product data
            val itemsWithProducts = snapshot.documents.mapNotNull { document ->
                val data = document.data
                if (data == null) return@mapNotNull null
                
                // Get productId from the item document
                val productId = (data["productId"] as? String) 
                    ?: (data["barcode"] as? String)
                    ?: return@mapNotNull null
                
                // Fetch product information from products collection
                val product = productRepository.getProductByBarcodeId(productId)
                
                if (product == null) {
                    Log.w("ItemsRepositoryImpl", "Product not found for productId: $productId")
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
                
                // Return item data with productId for grouping
                ItemData(
                    docId = document.id,
                    productId = productId,
                    product = product,
                    category = categoryString,
                    quantity = quantity,
                    expiryDate = expiryDate
                )
            }
            
            // Group items by productId
            val groupedByProductId = itemsWithProducts.groupBy { it.productId }
            
            // For each productId group, create a single RequestItem with aggregated data
            groupedByProductId.map { (productId, items) ->
                // Sort items by expiry date: nearest first, then items without expiry date
                val sortedItems = items.sortedWith(compareBy<ItemData> { item ->
                    // Items with expiry date come first, sorted by date (earliest first)
                    item.expiryDate?.toDate()?.time ?: Long.MAX_VALUE
                })
                
                // Calculate total quantity
                val totalQuantity = items.sumOf { it.quantity }
                
                // Get the nearest expiry date (first item in sorted list)
                val nearestExpiryDate = sortedItems.firstOrNull()?.expiryDate
                
                // Use productId as docId (this will be handled specially in RequestsRepository)
                RequestItem(
                    docId = productId, // Use productId instead of document.id for grouped items
                    id = 0,
                    name = items.first().product.name,
                    category = items.first().category,
                    quantity = totalQuantity,
                    expiryDate = nearestExpiryDate
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
