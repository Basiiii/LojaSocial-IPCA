package com.lojasocial.app.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.lojasocial.app.api.BarcodeApiResponse
import com.lojasocial.app.api.BarcodeProduct
import com.lojasocial.app.api.ProductApiService
import com.lojasocial.app.data.model.Product
import com.lojasocial.app.data.model.StockItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import retrofit2.Response
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class ProductRepository @Inject constructor(
    private val apiService: ProductApiService,
    private val firestore: FirebaseFirestore
) {
    private val productsCollection = firestore.collection("products")
    private val itemsCollection = firestore.collection("items")
    
    // Rate limiting: track last API call time
    private var lastApiCallTime = 0L
    private val minApiCallInterval = 1200L // 1.2 seconds between calls (slightly more than 1 sec to be safe)

    // Get product by ID from products collection
    suspend fun getProductById(productId: String): Product? {
        return try {
            Log.d("ProductRepository", "Getting product by ID: $productId")
            val document = productsCollection.document(productId).get().await()
            document.toObject(Product::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error getting product by ID", e)
            null
        }
    }

    // Get product by barcode (using barcode as document ID)
    suspend fun getProductByBarcodeId(barcode: String): Product? {
        return try {
            Log.d("ProductRepository", "Getting product by barcode ID: $barcode")
            Log.d("ProductRepository", "Collection path: ${productsCollection.path}")
            Log.d("ProductRepository", "Document path: ${productsCollection.document(barcode).path}")
            
            val document = productsCollection.document(barcode).get().await()
            Log.d("ProductRepository", "Document exists: ${document.exists()}")
            Log.d("ProductRepository", "Document data: ${document.data}")
            
            val product = document.toObject(Product::class.java)?.copy(id = document.id)
            if (product != null) {
                Log.d("ProductRepository", "Product found: ${product.name}")
            } else {
                Log.d("ProductRepository", "No product found for barcode: $barcode")
                Log.d("ProductRepository", "Document.toObject returned null")
            }
            product
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error getting product by barcode ID", e)
            null
        }
    }

    // Get stock item by barcode and expiration date
    suspend fun getStockItemByBarcodeAndExpiry(barcode: String, expirationDate: Date): StockItem? {
        return try {
            Log.d("ProductRepository", "Getting stock item by barcode: $barcode and expiry: $expirationDate")
            val snapshot = itemsCollection
                .whereEqualTo("barcode", barcode)
                .whereEqualTo("expirationDate", expirationDate)
                .get()
                .await()
            
            val stockItem = snapshot.documents.firstOrNull()?.let { doc ->
                doc.toObject(StockItem::class.java)?.copy(id = doc.id)
            }
            if (stockItem != null) {
                Log.d("ProductRepository", "Existing stock item found with quantity: ${stockItem.quantity}")
            } else {
                Log.d("ProductRepository", "No existing stock item found for barcode: $barcode and expiry: $expirationDate")
            }
            stockItem
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error getting stock item by barcode and expiry", e)
            null
        }
    }

    // Update stock item quantity
    suspend fun updateStockItemQuantity(stockItemId: String, newQuantity: Int) {
        try {
            Log.d("ProductRepository", "Updating stock item $stockItemId quantity to: $newQuantity")
            itemsCollection.document(stockItemId)
                .update("quantity", newQuantity)
                .await()
            Log.d("ProductRepository", "Stock item quantity updated successfully")
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error updating stock item quantity", e)
            throw e
        }
    }

    // Save or update product (using barcode as document ID)
    suspend fun saveOrUpdateProduct(product: Product, barcode: String) {
        try {
            Log.d("ProductRepository", "Saving/updating product with barcode: $barcode")
            productsCollection.document(barcode).set(product).await()
            Log.d("ProductRepository", "Product saved/updated successfully with ID: $barcode")
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error saving/updating product", e)
            throw e
        }
    }

    // Save stock item to items collection
    suspend fun saveStockItem(stockItem: StockItem): String {
        return try {
            Log.d("ProductRepository", "Saving stock item for barcode: ${stockItem.barcode}")
            val docRef = itemsCollection.add(stockItem).await()
            Log.d("ProductRepository", "Stock item saved with ID: ${docRef.id}")
            docRef.id
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error saving stock item", e)
            throw e
        }
    }

    // Search products by name
    suspend fun searchProductsByName(query: String): List<Product> {
        return try {
            Log.d("ProductRepository", "Searching products with query: $query")
            val snapshot = productsCollection
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .limit(10)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Product::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error searching products", e)
            emptyList()
        }
    }
    
    // External API operations with retry logic and rate limiting
    suspend fun getProductByBarcode(barcode: String): Result<BarcodeProduct> {
        val maxRetries = 3
        var lastException: Exception? = null
        
        for (attempt in 1..maxRetries) {
            try {
                // Rate limiting: ensure minimum interval between API calls
                val currentTime = System.currentTimeMillis()
                val timeSinceLastCall = currentTime - lastApiCallTime
                if (timeSinceLastCall < minApiCallInterval) {
                    val waitTime = minApiCallInterval - timeSinceLastCall
                    Log.d("ProductRepository", "Rate limiting: waiting ${waitTime}ms before API call")
                    delay(waitTime)
                }
                lastApiCallTime = System.currentTimeMillis()
                
                Log.d("ProductRepository", "Making API call for barcode: $barcode (attempt $attempt/$maxRetries)")
                val response = apiService.getProductByBarcode(barcode)
                Log.d("ProductRepository", "API response received")
                Log.d("ProductRepository", "Response code: ${response.code()}")
                Log.d("ProductRepository", "Response message: ${response.message()}")
                Log.d("ProductRepository", "Response successful: ${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    response.body()?.let { barcodeApiResponse ->
                        Log.d("ProductRepository", "SUCCESS: Barcode API data received")
                        Log.d("ProductRepository", "Products list: ${barcodeApiResponse.products}")
                        
                        // Check if products list is null or empty
                        if (barcodeApiResponse.products.isNullOrEmpty()) {
                            Log.e("ProductRepository", "ERROR: Products list is null or empty")
                            return Result.failure(Exception("No products found for barcode: $barcode"))
                        }
                        
                        val firstProduct = barcodeApiResponse.products.firstOrNull()
                        if (firstProduct != null) {
                            Log.d("ProductRepository", "Product title: ${firstProduct.title}")
                            Log.d("ProductRepository", "Product brand: ${firstProduct.brand}")
                            Log.d("ProductRepository", "Product category: ${firstProduct.category}")
                            Log.d("ProductRepository", "Product imageUrl: ${firstProduct.imageUrl}")
                            return Result.success(firstProduct)
                        } else {
                            Log.e("ProductRepository", "ERROR: No products found in response")
                            return Result.failure(Exception("No products found for barcode: $barcode"))
                        }
                    } ?: run {
                        Log.e("ProductRepository", "ERROR: Empty response body")
                        return Result.failure(Exception("Empty response body"))
                    }
                } else {
                    Log.e("ProductRepository", "ERROR: API call failed")
                    Log.e("ProductRepository", "Error code: ${response.code()}")
                    Log.e("ProductRepository", "Error message: ${response.message()}")
                    
                    // Handle specific error codes
                    when (response.code()) {
                        429 -> {
                            if (attempt < maxRetries) {
                                val delayMs = 2000L * attempt // 2s, 4s, 6s to be more conservative
                                Log.w("ProductRepository", "Rate limit hit, retrying in ${delayMs}ms...")
                                delay(delayMs)
                                continue
                            } else {
                                lastException = Exception("API rate limit exceeded. Please try again in a few moments.")
                            }
                        }
                        404 -> {
                            return Result.failure(Exception("Product not found in database"))
                        }
                        500 -> {
                            if (attempt < maxRetries) {
                                val delayMs = 2000L * attempt
                                Log.w("ProductRepository", "Server error, retrying in ${delayMs}ms...")
                                delay(delayMs)
                                continue
                            } else {
                                lastException = Exception("Server error. Please try again later")
                            }
                        }
                        else -> {
                            lastException = Exception("API Error: ${response.code()} - ${response.message()}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ProductRepository", "EXCEPTION: API call failed (attempt $attempt/$maxRetries)", e)
                lastException = e
                
                if (attempt < maxRetries && e is java.net.SocketTimeoutException || e is java.net.UnknownHostException) {
                    val delayMs = 1000L * attempt
                    Log.w("ProductRepository", "Network error, retrying in ${delayMs}ms...")
                    delay(delayMs)
                    continue
                }
            }
        }
        
        return Result.failure(lastException ?: Exception("Unknown error occurred"))
    }
    
    // Optional: Cache products locally
    suspend fun getCachedProduct(barcode: String): Flow<BarcodeProduct?> = flow {
        // Implement local caching if needed
        emit(null)
    }
}
