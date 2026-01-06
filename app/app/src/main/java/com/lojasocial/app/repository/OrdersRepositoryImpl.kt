package com.lojasocial.app.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrdersRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : OrdersRepository {

    override suspend fun submitOrder(selectedItems: Map<String, Int>): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            val itemsList = selectedItems.map { (productDocId, quantity) ->
                mapOf(
                    "productDocId" to productDocId,
                    "quantity" to quantity
                )
            }

            val requestsData = mapOf(
                "userId" to currentUser.uid,
                "status" to 0,
                "submissionDate" to FieldValue.serverTimestamp(),
                "totalItems" to selectedItems.values.sum(),
                "items" to itemsList
            )

            firestore.collection("requests")
                .add(requestsData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
