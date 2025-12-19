package com.lojasocial.app.domain

data class PendingRequest(
    val id: Int,
    val name: String,
    val time: String,
    val category: String,
    val categoryIcon: Int
)