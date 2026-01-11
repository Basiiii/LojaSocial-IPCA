package com.lojasocial.app.repository.product

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lojasocial.app.domain.stock.StockItem
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockItemRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val itemsCollection = firestore.collection("items")

    suspend fun getStockItemByBarcode(barcode: String): StockItem? {
        return try {
            val snapshot = itemsCollection
                .whereEqualTo("barcode", barcode)
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.let { doc ->
                doc.toObject(StockItem::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveStockItem(stockItem: StockItem): String {
        return try {
            // Convert to map and exclude id field
            val stockItemMap = mutableMapOf<String, Any>(
                "barcode" to stockItem.barcode,
                "createdAt" to stockItem.createdAt,
                "quantity" to stockItem.quantity,
                "reservedQuantity" to stockItem.reservedQuantity,
                "productId" to stockItem.productId
            )
            stockItem.campaignId?.let { stockItemMap["campaignId"] = it }
            stockItem.expirationDate?.let { stockItemMap["expirationDate"] = it }
            
            val docRef = itemsCollection.add(stockItemMap).await()
            docRef.id
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateStockItem(stockItemId: String, updates: Map<String, Any>): Boolean {
        return try {
            itemsCollection.document(stockItemId).update(updates).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Deletes a stock item from the items collection
     * @param stockItemId The ID of the stock item to delete
     * @return Boolean indicating success or failure
     */
    suspend fun deleteStockItem(stockItemId: String): Boolean {
        return try {
            itemsCollection.document(stockItemId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get items expiring within the specified number of days
     * @param daysThreshold Number of days to check ahead (default: 3)
     * @return List of StockItems expiring within the threshold
     */
    suspend fun getExpiringItems(daysThreshold: Int = 3): List<StockItem> {
        return try {
            val now = java.util.Date()
            val calendar = java.util.Calendar.getInstance()
            calendar.time = now
            calendar.add(java.util.Calendar.DAY_OF_MONTH, daysThreshold)
            val thresholdDate = calendar.time

            // Query items with quantity > 0 and expirationDate <= thresholdDate
            val snapshot = itemsCollection
                .whereGreaterThan("quantity", 0)
                .whereLessThanOrEqualTo("expirationDate", thresholdDate)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val item = doc.toObject(StockItem::class.java)?.copy(id = doc.id)
                // Additional filter: ensure expirationDate exists and is in the future
                if (item != null && item.expirationDate != null) {
                    val expDate = item.expirationDate
                    if (expDate.after(now) && expDate.before(thresholdDate) || expDate == thresholdDate) {
                        item
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get items expiring within the specified number of days with pagination support
     * 
     * @param daysThreshold Number of days to check ahead (default: 3)
     * @param limit Maximum number of items to return
     * @param lastExpirationDate Optional last expiration date from previous batch for pagination
     * @return Pair of List of StockItems expiring within the threshold and Boolean indicating if more items exist
     */
    suspend fun getExpiringItemsPaginated(
        daysThreshold: Int = 3, 
        limit: Int = 5, 
        lastExpirationDate: java.util.Date? = null
    ): Pair<List<StockItem>, Boolean> {
        return try {
            val now = java.util.Date()
            val calendar = java.util.Calendar.getInstance()
            calendar.time = now
            calendar.add(java.util.Calendar.DAY_OF_MONTH, daysThreshold)
            val thresholdDate = calendar.time

            Log.d("StockItemRepository", "Query params: limit=$limit, lastDate=$lastExpirationDate, threshold=$thresholdDate")

            // Simplified query - get items by expiration date only, filter client-side
            var query = itemsCollection
                .orderBy("expirationDate")
                .limit(limit.toLong())

            // For pagination, start after the last loaded expiration date
            lastExpirationDate?.let { lastDate ->
                query = query.startAfter(lastDate)
                Log.d("StockItemRepository", "Starting after date: $lastDate")
            }

            val snapshot = query.get().await()
            Log.d("StockItemRepository", "Query returned ${snapshot.documents.size} documents")

            val items = snapshot.documents.mapNotNull { doc ->
                val item = doc.toObject(StockItem::class.java)?.copy(id = doc.id)
                // Client-side filtering: ensure item meets all criteria
                if (item != null && 
                    item.expirationDate != null && 
                    item.quantity > 0 &&
                    (item.expirationDate.before(thresholdDate) || item.expirationDate == thresholdDate)) {
                    Log.d("StockItemRepository", "Including item: ${item.id}, exp: ${item.expirationDate}, qty: ${item.quantity}")
                    item
                } else {
                    if (item != null) {
                        Log.d("StockItemRepository", "Filtering out item: ${item.id}, exp: ${item.expirationDate}, qty: ${item.quantity}")
                    }
                    null
                }
            }

            Log.d("StockItemRepository", "Final items count: ${items.size}")
            
            // Check if there might be more items
            // Only return hasMore = true if we got the full limit AND there might be more
            val hasMore = items.size == limit && items.isNotEmpty()
            Log.d("StockItemRepository", "Has more items: $hasMore (items.size=${items.size}, limit=$limit)")

            Pair(items, hasMore)
        } catch (e: Exception) {
            Log.e("StockItemRepository", "Error in getExpiringItemsPaginated", e)
            Pair(emptyList(), false)
        }
    }

    /**
     * Get all stock items for a specific barcode
     * @param barcode The barcode to filter by
     * @return List of StockItems with the given barcode
     */
    suspend fun getStockItemsByBarcode(barcode: String): List<StockItem> {
        return try {
            // Query by barcode only (to avoid index requirement), then filter by quantity client-side
            val snapshot = itemsCollection
                .whereEqualTo("barcode", barcode)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                val item = doc.toObject(StockItem::class.java)?.copy(id = doc.id)
                // Filter by quantity > 0 client-side
                if (item != null && item.quantity > 0) item else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get all stock items (for calculating total stock per product)
     * @return List of all StockItems with quantity > 0
     */
    suspend fun getAllStockItems(): List<StockItem> {
        return try {
            val snapshot = itemsCollection
                .whereGreaterThan("quantity", 0)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(StockItem::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
