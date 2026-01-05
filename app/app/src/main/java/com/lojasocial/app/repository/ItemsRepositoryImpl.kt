package com.lojasocial.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lojasocial.app.domain.RequestItem
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ItemsRepository {

    override suspend fun getProducts(pageSize: Int, lastVisibleId: String?): List<RequestItem> {
        return try {
            var query: Query = firestore.collection("items")
                .whereGreaterThan("quantity", 0)
                .orderBy("quantity")
                .limit(pageSize.toLong())

            if (lastVisibleId != null) {
                val lastVisible = firestore.collection("items").document(lastVisibleId).get().await()
                query = query.startAfter(lastVisible)
            }

            val snapshot = query.get().await()
            snapshot.documents.mapNotNull { document ->
                document.toObject(RequestItem::class.java)?.apply {
                    docId = document.id
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
