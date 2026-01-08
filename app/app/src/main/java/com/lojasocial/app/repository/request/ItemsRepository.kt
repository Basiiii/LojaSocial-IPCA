package com.lojasocial.app.repository.request

import com.lojasocial.app.domain.request.RequestItem

const val PAGINATION = 5

interface ItemsRepository {
    suspend fun getProducts(pageSize: Int = PAGINATION, lastVisibleId: String? = null): List<RequestItem>
    suspend fun getProductById(productId: String): RequestItem?
    suspend fun updateProductQuantity(productId: String, newQuantity: Int): Boolean
}
