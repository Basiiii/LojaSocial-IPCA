package com.lojasocial.app.repository

import com.lojasocial.app.domain.RequestItem

const val PAGINATION = 5

interface ItemsRepository {
    suspend fun getProducts(pageSize: Int = PAGINATION, lastVisibleId: String? = null): List<RequestItem>
}