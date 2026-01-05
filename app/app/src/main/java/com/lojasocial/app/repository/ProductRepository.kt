package com.lojasocial.app.repository

import android.util.Log
import com.lojasocial.app.api.BarcodeApiResponse
import com.lojasocial.app.api.BarcodeProduct
import com.lojasocial.app.api.ProductApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
data class ProductRepository @Inject constructor(
    private val apiService: ProductApiService
) {
    suspend fun getProductByBarcode(barcode: String): Result<BarcodeProduct> {
        return try {
            Log.d("ProductRepository", "Making API call for barcode: $barcode")
            val response = apiService.getProductByBarcode(barcode)
            Log.d("ProductRepository", "API response received")
            Log.d("ProductRepository", "Response code: ${response.code()}")
            Log.d("ProductRepository", "Response message: ${response.message()}")
            Log.d("ProductRepository", "Response successful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                response.body()?.let { barcodeApiResponse ->
                    Log.d("ProductRepository", "SUCCESS: Barcode API data received")
                    val firstProduct = barcodeApiResponse.products.firstOrNull()
                    if (firstProduct != null) {
                        Log.d("ProductRepository", "Product title: ${firstProduct.title}")
                        Log.d("ProductRepository", "Product brand: ${firstProduct.brand}")
                        Log.d("ProductRepository", "Product category: ${firstProduct.category}")
                        Log.d("ProductRepository", "Product imageUrl: ${firstProduct.imageUrl}")
                        Result.success(firstProduct)
                    } else {
                        Log.e("ProductRepository", "ERROR: No products found in response")
                        Result.failure(Exception("No products found for barcode: $barcode"))
                    }
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
    
    // Optional: Cache products locally
    suspend fun getCachedProduct(barcode: String): Flow<BarcodeProduct?> = flow {
        // Implement local caching if needed
        emit(null)
    }
}
