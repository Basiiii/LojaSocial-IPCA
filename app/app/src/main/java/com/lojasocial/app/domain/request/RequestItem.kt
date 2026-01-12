package com.lojasocial.app.domain.request

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class RequestItem(
    @DocumentId
    var docId: String = "",
    val id: Int = 0,
    val name: String = "",
    val category: String = "Geral",
    val quantity: Int = 0,
    val expiryDate: Timestamp? = null,
    val barcode: String = ""
) {
    val stock: Int get() = quantity
}
