package com.lojasocial.app.repository

import android.util.Log
import com.lojasocial.app.api.ProductApiService
import com.lojasocial.app.api.ProductResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
data class ProductRepository @Inject constructor(
    private val apiService: ProductApiService
) {
    suspend fun getProductByBarcode(barcode: String): Result<ProductResponse> {
        return try {
            Log.d("ProductRepository", "Making API call for barcode: $barcode")
            val response = apiService.getProductByBarcode(barcode)
            Log.d("ProductRepository", "API response received")
            Log.d("ProductRepository", "Response code: ${response.code()}")
            Log.d("ProductRepository", "Response message: ${response.message()}")
            Log.d("ProductRepository", "Response successful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                response.body()?.let { product ->
                    Log.d("ProductRepository", "SUCCESS: Product data received")
                    Log.d("ProductRepository", "Product title: ${product.title}")
                    Result.success(product)
                } ?: run {
                    Log.e("ProductRepository", "ERROR: Empty response body")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Log.e("ProductRepository", "ERROR: API call failed")
                Log.e("ProductRepository", "Error code: ${response.code()}")
                Log.e("ProductRepository", "Error message: ${response.message()}")
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("ProductRepository", "EXCEPTION: API call failed", e)
            Log.e("ProductRepository", "Exception message: ${e.message}")
            Log.e("ProductRepository", "Exception type: ${e::class.java.simpleName}")
            Result.failure(e)
        }
    }
    
    suspend fun searchProducts(query: String): Result<List<ProductResponse>> {
        return try {
            val response = apiService.searchProducts(query)
            if (response.isSuccessful) {
                response.body()?.let { products ->
                    Result.success(products)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Optional: Cache products locally
    suspend fun getCachedProduct(barcode: String): Flow<ProductResponse?> = flow {
        // Implement local caching if needed
        emit(null)
    }
}
