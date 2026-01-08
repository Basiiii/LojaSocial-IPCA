package com.lojasocial.app.repository.request

interface OrdersRepository {
    suspend fun submitOrder(selectedItems: Map<String, Int>): Result<Unit>
}
