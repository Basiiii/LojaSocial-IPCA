package com.lojasocial.app.repository.product

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
