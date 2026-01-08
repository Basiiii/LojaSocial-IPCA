package com.lojasocial.app.domain.stock

import java.util.Date

data class StockItem(
    val id: String = "",
    val barcode: String = "",
    val campaignId: String? = null,
    val createdAt: Date = Date(),
    val expirationDate: Date? = null,
    val quantity: Int = 0,
    val productId: String = "" // Reference to Product collection
)
