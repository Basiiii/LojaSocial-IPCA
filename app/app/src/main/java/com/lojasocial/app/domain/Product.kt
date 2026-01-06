package com.lojasocial.app.domain

import com.google.firebase.firestore.PropertyName

data class Product(
    val id: String = "",
    val name: String = "",
    val brand: String = "",
    val category: Int = 0,
    @get:PropertyName("categoryDisplayName")
    @set:PropertyName("categoryDisplayName")
    var categoryDisplayName: String = "",
    @get:PropertyName("imageUrl")
    @set:PropertyName("imageUrl")
    var imageUrl: String = ""
)
