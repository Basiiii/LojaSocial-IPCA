package com.lojasocial.app.data.model

data class Product(
    val name: String = "",
    val brand: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val quantity: Int = 0,
    val campaignId: String? = null,
    val stockBatches: Map<String, Int> = emptyMap() // expiryDate -> quantity
)
