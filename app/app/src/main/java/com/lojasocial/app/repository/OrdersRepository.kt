package com.lojasocial.app.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface OrdersRepository {
    suspend fun submitOrder(selectedItems: Map<String, Int>): Result<Unit>
}

@Singleton
class OrdersRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : OrdersRepository {

    override suspend fun submitOrder(selectedItems: Map<String, Int>): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            val requestsData = hashMapOf(
                "userId" to currentUser.uid,
                "status" to 0,
                "submissionDate" to FieldValue.serverTimestamp(),
                "totalItems" to selectedItems.values.sum()
            )

            val documentRef = firestore.collection("requests")
                .add(requestsData)
                .await()


            val itemsCollection = documentRef.collection("items")
            val batch = firestore.batch()

            selectedItems.forEach { (productDocId, quantity) ->
                val itemData = hashMapOf(
                    "productDocId" to productDocId,
                    "quantity" to quantity,
                    "addedAt" to FieldValue.serverTimestamp()
                )
                val itemRef = itemsCollection.document()
                batch.set(itemRef, itemData)
            }

            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}