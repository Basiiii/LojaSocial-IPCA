package com.lojasocial.app.repository

import com.lojasocial.app.domain.RequestItem

const val PAGINATION = 5

interface ItemsRepository {
    suspend fun getProducts(pageSize: Int = PAGINATION, lastVisibleId: String? = null): List<RequestItem>
    suspend fun getProductById(productId: String): RequestItem?
    suspend fun updateProductQuantity(productId: String, newQuantity: Int): Boolean
}