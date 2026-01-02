package com.lojasocial.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.domain.RequestItem
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface ItemsRepository {
    suspend fun getProducts(): List<RequestItem>
}

@Singleton
class ItemsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ItemsRepository {

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
}