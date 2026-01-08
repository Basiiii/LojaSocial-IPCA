package com.lojasocial.app.domain.request

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Represents a pickup request in the system.
 * 
 * @property id Document ID from Firestore
 * @property userId ID of the user who made the request
 * @property status Request status: 0=SUBMETIDO, 1=PENDENTE_LEVANTAMENTO, 2=CONCLUIDO, 3=REJEITADO
 * @property submissionDate Date when the request was submitted
 * @property totalItems Total number of items in the request
 * @property scheduledPickupDate Optional scheduled pickup date (set when request is accepted)
 * @property rejectionReason Optional reason for rejection
 * @property items List of items in the request (loaded separately from subcollection)
 */
data class Request(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val status: Int = 0, // 0=SUBMETIDO, 1=PENDENTE_LEVANTAMENTO, 2=CONCLUIDO, 3=REJEITADO
    val submissionDate: Date? = null,
    val totalItems: Int = 0,
    val scheduledPickupDate: Date? = null,
    val rejectionReason: String? = null,
    val items: List<RequestItemDetail> = emptyList()
)

/**
 * Represents an item within a request.
 */
data class RequestItemDetail(
    val productDocId: String = "",
    val productName: String = "", // Loaded from items collection
    val quantity: Int = 0,
    val brand: String = "", // Product brand
    val expiryDate: Date? = null, // Expiry date from items subcollection
    val category: Int = 1 // ProductCategory.id (1=ALIMENTAR, 2=CASA, 3=HIGIENE_PESSOAL)
)
