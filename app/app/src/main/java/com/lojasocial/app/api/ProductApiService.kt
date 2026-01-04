package com.lojasocial.app.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class ProductResponse(
    val title: String, // API returns "title" not "name"
    val alias: String? = null,
    val description: String? = null,
    val brand: String? = null,
    val manufacturer: String? = null,
    val msrp: String? = null, // Manufacturer suggested retail price
    val ASIN: String? = null,
    val category: String? = null,
    val categories: String? = null,
    val stores: List<String>? = null,
    val barcode: String? = null,
    val success: Boolean? = null,
    val timestamp: Long? = null,
    val images: List<String>? = null,
    val metadata: Map<String, Any>? = null,
    val metanutrition: Map<String, Any>? = null
) {
    // Helper property to maintain compatibility with existing code
    val name: String get() = title
}

data class ApiResponse<T>(
    val data: T?,
    val message: String?,
    val success: Boolean
)

interface ProductApiService {
    @GET("{barcode}")
    suspend fun getProductByBarcode(@Path("barcode") barcode: String): Response<ProductResponse>
    
    // Optional: Add more endpoints
    @GET("search/{query}")
    suspend fun searchProducts(@Path("query") query: String): Response<List<ProductResponse>>
    
    @POST("products")
    suspend fun createProduct(@Body product: ProductResponse): Response<ProductResponse>
}
