package com.lojasocial.app.domain.request

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.lojasocial.app.data.model.Product
import com.lojasocial.app.data.model.StockItem

data class RequestItem(
    @DocumentId
    val id: String = "",
    val product: Product = Product(),
    val stockItem: StockItem = StockItem(),
    val quantity: Int = 0
) {
    val name: String get() = product.name
    val category: String get() = product.getCategoryDisplayName()
    val stock: Int get() = stockItem.quantity
    val expiryDate: Timestamp? = stockItem.expirationDate?.let { Timestamp(it) }
}
