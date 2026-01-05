package com.lojasocial.app.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.api.BarcodeApiResponse
import com.lojasocial.app.api.BarcodeProduct
import com.lojasocial.app.api.ProductApiService
import com.lojasocial.app.data.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
data class ProductRepository @Inject constructor(
    private val apiService: ProductApiService,
    private val firestore: FirebaseFirestore
) {
    // Firestore operations
suspend fun getProductFromFirestore(barcode: String): Product? {
    return try {
        Log.d("ProductRepository", "Checking Firestore for barcode: $barcode")
        val document = firestore.collection("items")
            .document(barcode)
            .get()
            .await()
        
        if (document.exists()) {
            Log.d("ProductRepository", "Product found in Firestore: $barcode")
            Log.d("ProductRepository", "Firestore document data: ${document.data}")
            val product = document.toObject(Product::class.java)
            Log.d("ProductRepository", "Mapped Product object: $product")
            product
        } else {
            Log.d("ProductRepository", "Product not found in Firestore: $barcode")
            null
        }
    } catch (e: Exception) {
        Log.e("ProductRepository", "Error fetching product from Firestore", e)
        null
    }
}
    
    suspend fun saveProductToFirestore(barcode: String, product: Product) {
        try {
            Log.d("ProductRepository", "Saving product to Firestore: $barcode")
            firestore.collection("items")
                .document(barcode)
                .set(product)
                .await()
            Log.d("ProductRepository", "Product saved successfully: $barcode")
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error saving product to Firestore", e)
            throw e
        }
    }
    
    // External API operations
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
