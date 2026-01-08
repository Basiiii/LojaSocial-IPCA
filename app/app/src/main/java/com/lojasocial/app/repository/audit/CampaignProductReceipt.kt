package com.lojasocial.app.repository.audit

import com.lojasocial.app.domain.product.Product
import java.util.Date

/**
 * Data class representing a product receipt for a campaign.
 * Contains audit log information and product details.
 */
data class CampaignProductReceipt(
    val itemId: String,
    val quantity: Int,
    val barcode: String,
    val timestamp: Date?,
    val userId: String?,
    val userName: String? = null,
    val product: Product? = null // Product information loaded separately
)
