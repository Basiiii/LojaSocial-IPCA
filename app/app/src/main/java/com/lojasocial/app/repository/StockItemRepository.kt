package com.lojasocial.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.data.model.StockItem
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
}
