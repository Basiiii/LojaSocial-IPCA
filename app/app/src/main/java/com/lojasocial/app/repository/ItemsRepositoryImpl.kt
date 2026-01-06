package com.lojasocial.app.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lojasocial.app.data.model.Product
import com.lojasocial.app.data.model.StockItem
import com.lojasocial.app.domain.RequestItem
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ItemsRepository {

    private val productsCollection = firestore.collection("products")
    private val stockItemsCollection = firestore.collection("items")

    override suspend fun getProducts(pageSize: Int, lastVisibleId: String?): List<RequestItem> {
        return try {
            var stockQuery: Query = stockItemsCollection
                .whereGreaterThan("quantity", 0)
                .orderBy("quantity")
                .limit(pageSize.toLong())

            if (lastVisibleId != null) {
                val lastVisible = stockItemsCollection.document(lastVisibleId).get().await()
                stockQuery = stockQuery.startAfter(lastVisible)
            }

            val stockSnapshot = stockQuery.get().await()

            stockSnapshot.documents.mapNotNull { stockDoc ->
                val stockItem = stockDoc.toObject(StockItem::class.java) ?: return@mapNotNull null
                
                if (stockItem.productId.isNullOrEmpty()) {
                    return@mapNotNull null
                }

                val productDoc = productsCollection.document(stockItem.productId).get().await()
                val product = productDoc.toObject(Product::class.java)?.copy(id = productDoc.id)

                if (product != null) {
                    RequestItem(
                        id = stockDoc.id,
                        product = product,
                        stockItem = stockItem,
                        quantity = 0
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getProductById(productId: String): RequestItem? {
        return try {
            val productDoc = productsCollection.document(productId).get().await()
            val product = productDoc.toObject(Product::class.java)?.copy(id = productDoc.id)
                ?: return null

            val stockQuery = stockItemsCollection
                .whereEqualTo("productId", productId)
                .limit(1)
                .get()
                .await()

            val stockDoc = stockQuery.documents.firstOrNull() ?: return null
            val stockItem = stockDoc.toObject(StockItem::class.java) ?: return null

            RequestItem(
                id = stockDoc.id,
                product = product,
                stockItem = stockItem,
                quantity = 0
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun updateProductQuantity(productId: String, newQuantity: Int): Boolean {
        return try {
            stockItemsCollection.document(productId)
                .update("quantity", newQuantity)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}