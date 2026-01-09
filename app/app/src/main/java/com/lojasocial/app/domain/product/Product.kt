package com.lojasocial.app.domain.product

data class Product(
    val id: String = "",
    val name: String = "",
    val brand: String = "",
    val category: Int = 1, // ProductCategory.id
    val imageUrl: String = "",
    val serializedImage: String? = null // Base64-encoded image for manually added products
) {
    fun getCategoryDisplayName(): String {
        return ProductCategory.fromId(category)?.displayName ?: "Unknown"
    }
}
