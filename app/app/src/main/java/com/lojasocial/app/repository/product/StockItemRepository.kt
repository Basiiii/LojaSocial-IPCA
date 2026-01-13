package com.lojasocial.app.repository.product

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
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
     * Get items expiring within the specified number of days (including already expired items)
     * @param daysThreshold Number of days to check ahead (default: 3)
     * @return List of StockItems that have expired or are expiring within the threshold
     */
    suspend fun getExpiringItems(daysThreshold: Int = 3): List<StockItem> {
        return try {
            val now = java.util.Date()
            val calendar = java.util.Calendar.getInstance()
            calendar.time = now
            calendar.add(java.util.Calendar.DAY_OF_MONTH, daysThreshold)
            // Set time to end of threshold day (23:59:59.999)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
            calendar.set(java.util.Calendar.MINUTE, 59)
            calendar.set(java.util.Calendar.SECOND, 59)
            calendar.set(java.util.Calendar.MILLISECOND, 999)
            val thresholdDate = calendar.time

            android.util.Log.d("StockItemRepository", "Getting expiring items: now=$now, thresholdDate=$thresholdDate (including expired items)")

            // Query items with quantity > 0 and expirationDate <= thresholdDate
            // This includes both expired items and items expiring within the threshold
            val snapshot = itemsCollection
                .whereGreaterThan("quantity", 0)
                .whereLessThanOrEqualTo("expirationDate", thresholdDate)
                .get()
                .await()

            android.util.Log.d("StockItemRepository", "Query returned ${snapshot.documents.size} documents")

            snapshot.documents.mapNotNull { doc ->
                val item = doc.toObject(StockItem::class.java)?.copy(id = doc.id)
                // Filter: ensure expirationDate exists and is within threshold (includes expired items)
                if (item != null && item.expirationDate != null) {
                    val expDate = item.expirationDate
                    // Include items that have expired or are expiring up to threshold date
                    val isWithinThreshold = expDate.before(thresholdDate) || expDate.time <= thresholdDate.time
                    
                    if (isWithinThreshold) {
                        android.util.Log.d("StockItemRepository", "Including item: ${item.id}, expirationDate=$expDate")
                        item
                    } else {
                        android.util.Log.d("StockItemRepository", "Excluding item: ${item.id}, expirationDate=$expDate, isWithinThreshold=$isWithinThreshold")
                        null
                    }
                } else {
                    null
                }
            }.also { filteredItems ->
                android.util.Log.d("StockItemRepository", "Final filtered items count: ${filteredItems.size}")
            }
        } catch (e: Exception) {
            android.util.Log.e("StockItemRepository", "Error getting expiring items", e)
            emptyList()
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

    /**
     * Delete a stock item by ID
     * @param stockItemId The ID of the stock item to delete
     * @return true if deletion was successful, false otherwise
     */
    suspend fun deleteStockItem(stockItemId: String): Boolean {
        return try {
            Log.d("StockItemRepository", "Attempting to delete stock item: $stockItemId")
            
            // Verify item exists before deletion
            val docSnapshot = itemsCollection.document(stockItemId).get().await()
            Log.d("StockItemRepository", "Document exists: ${docSnapshot.exists()}")
            
            if (!docSnapshot.exists()) {
                Log.w("StockItemRepository", "Stock item not found: $stockItemId")
                return false
            }
            
            // Delete the item
            itemsCollection.document(stockItemId).delete().await()
            
            // Verify deletion
            val verifySnapshot = itemsCollection.document(stockItemId).get().await()
            Log.d("StockItemRepository", "Document still exists after deletion: ${verifySnapshot.exists()}")
            
            if (!verifySnapshot.exists()) {
                Log.d("StockItemRepository", "Successfully deleted stock item: $stockItemId")
                true
            } else {
                Log.e("StockItemRepository", "Failed to delete stock item: $stockItemId - document still exists")
                false
            }
        } catch (e: Exception) {
            Log.e("StockItemRepository", "Error deleting stock item: $stockItemId", e)
            false
        }
    }
}
