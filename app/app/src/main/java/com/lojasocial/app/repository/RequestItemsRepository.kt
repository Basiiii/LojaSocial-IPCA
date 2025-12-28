package com.lojasocial.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.domain.RequestItem
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class RequestItemsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    open suspend fun getProducts(): List<RequestItem> {
        return try {
            firestore.collection("request_items")
                .get()
                .await()
                .toObjects(RequestItem::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
