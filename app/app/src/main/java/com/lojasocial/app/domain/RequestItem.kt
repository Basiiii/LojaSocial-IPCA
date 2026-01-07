package com.lojasocial.app.domain

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class RequestItem(
    @DocumentId
    var docId: String = "",
    val id: Int = 0,
    val name: String = "",
    val category: String = "Geral",
    val quantity: Int = 0,
    val expiryDate: Timestamp? = null
) {
    val stock: Int get() = quantity
}
