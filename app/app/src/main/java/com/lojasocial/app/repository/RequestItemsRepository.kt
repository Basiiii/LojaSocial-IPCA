package com.lojasocial.app.repository

import com.lojasocial.app.domain.RequestItem

open class ProductsRepository {
    open fun getProducts(): List<RequestItem> {
        return listOf(
            RequestItem(1, "Arroz Agulha", "Alimentar", 20),
            RequestItem(2, "Massa Esparguete", "Alimentar", 15),
            RequestItem(3, "Leite UHT", "Alimentar", 0),
            RequestItem(4, "Atum em Lata", "Alimentar", 30),
            RequestItem(5, "Lixívia", "Limpeza", 10),
            RequestItem(6, "Detergente Loiça", "Limpeza", 8),
            RequestItem(7, "Sabonete Líquido", "Higiene", 25),
            RequestItem(8, "Papel Higiénico", "Higiene", 40),
        )
    }
}