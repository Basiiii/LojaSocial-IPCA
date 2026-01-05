package com.lojasocial.app.repository

interface OrdersRepository {
    suspend fun submitOrder(selectedItems: Map<String, Int>): Result<Unit>
}