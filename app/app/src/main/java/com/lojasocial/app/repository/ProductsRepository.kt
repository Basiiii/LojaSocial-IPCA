package com.lojasocial.app.repository

import com.lojasocial.app.domain.Product

open class ProductsRepository {
    open fun getProducts(): List<Product> {
        return listOf(
            Product(1, "Arroz Agulha", "Alimentar", 20),
            Product(2, "Massa Esparguete", "Alimentar", 15),
            Product(3, "Leite UHT", "Alimentar", 0),
            Product(4, "Atum em Lata", "Alimentar", 30),
            Product(5, "Lixívia", "Limpeza", 10),
            Product(6, "Detergente Loiça", "Limpeza", 8),
            Product(7, "Sabonete Líquido", "Higiene", 25),
            Product(8, "Papel Higiénico", "Higiene", 40),
        )
    }
}