package com.lojasocial.app.domain.product

enum class ProductCategory(val id: Int, val displayName: String) {
    ALIMENTAR(1, "Alimentar"),
    CASA(2, "Casa"),
    HIGIENE_PESSOAL(3, "Higiene Pessoal");
    
    companion object {
        fun fromId(id: Int): ProductCategory? {
            return values().find { it.id == id }
        }
        
        fun getAllCategories(): List<ProductCategory> {
            return values().toList()
        }
    }
}
