package com.lojasocial.app.ui.models

data class ProductUI(
    val id: Int,
    val name: String,
    val category: String,
    val stock: Int,
    var quantity: Int
)