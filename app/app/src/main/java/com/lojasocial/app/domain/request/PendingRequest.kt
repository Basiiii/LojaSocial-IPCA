package com.lojasocial.app.domain.request

data class PendingRequest(
    val id: Int,
    val name: String,
    val time: String,
    val category: String,
    val categoryIcon: Int
)
