package com.lojasocial.app.repository.request

import com.lojasocial.app.domain.request.RequestItem

const val PAGINATION = 5

interface ItemsRepository {
    suspend fun getProducts(pageSize: Int = PAGINATION, lastVisibleId: String? = null): List<RequestItem>
}
