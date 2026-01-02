package com.lojasocial.app.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.domain.RequestItem
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface RequestItemsRepository {
    suspend fun getProducts(): List<RequestItem>
    suspend fun submitOrder(selectedItems: Map<String, Int>): Result<Unit>
}

@Singleton
class RequestItemsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : RequestItemsRepository {

    override suspend fun getProducts(): List<RequestItem> {
        return try {
            val snapshot = firestore.collection("items")
                .whereGreaterThan("quantity", 0)
                .get()
                .await()
            snapshot.toObjects(RequestItem::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun submitOrder(selectedItems: Map<String, Int>): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            
            val applicationData = hashMapOf(
                "userId" to currentUser.uid,
                "status" to 0,
                "submissionDate" to FieldValue.serverTimestamp(),
                "totalItems" to selectedItems.values.sum()
            )
            
            val documentRef = firestore.collection("applications")
                .add(applicationData)
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