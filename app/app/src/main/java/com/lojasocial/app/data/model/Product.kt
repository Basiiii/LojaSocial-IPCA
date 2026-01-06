package com.lojasocial.app.data.model

data class Product(
    val id: String = "",
    val name: String = "",
    val brand: String = "",
    val category: Int = 1, // ProductCategory.id
    val imageUrl: String = ""
) {
    fun getCategoryDisplayName(): String {
        return ProductCategory.fromId(category)?.displayName ?: "Unknown"
    }
}
