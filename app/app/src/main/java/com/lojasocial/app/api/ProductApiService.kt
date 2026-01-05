package com.lojasocial.app.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

data class BarcodeApiResponse(
    val products: List<BarcodeProduct>
)

data class BarcodeProduct(
    val barcode_number: String,
    val barcode_formats: String,
    val mpn: String? = null,
    val model: String? = null,
    val asin: String? = null,
    val title: String,
    val category: String? = null,
    val manufacturer: String? = null,
    val brand: String? = null,
    val contributors: List<String>? = null,
    val age_group: String? = null,
    val ingredients: String? = null,
    val nutrition_facts: String? = null,
    val energy_efficiency_class: String? = null,
    val color: String? = null,
    val gender: String? = null,
    val material: String? = null,
    val pattern: String? = null,
    val format: String? = null,
    val multipack: String? = null,
    val size: String? = null,
    val length: String? = null,
    val width: String? = null,
    val height: String? = null,
    val weight: String? = null,
    val release_date: String? = null,
    val description: String? = null,
    val features: List<String>? = null,
    val images: List<String>? = null,
    val last_update: String? = null,
    val stores: List<Store>? = null,
    val reviews: List<Any>? = null
) {
    // Helper property to get first image or null if no images
    val imageUrl: String? get() = images?.firstOrNull()
}

data class Store(
    val name: String,
    val country: String,
    val currency: String,
    val currency_symbol: String,
    val price: String,
    val sale_price: String? = null,
    val tax: List<String>? = null,
    val link: String,
    val item_group_id: String? = null,
    val availability: String,
    val condition: String? = null,
    val shipping: List<String>? = null,
    val last_update: String? = null
)

interface ProductApiService {
    @GET("api/barcode")
    suspend fun getProductByBarcode(@Query("barcode") barcode: String): Response<BarcodeApiResponse>
}
