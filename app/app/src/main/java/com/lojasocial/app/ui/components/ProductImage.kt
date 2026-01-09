package com.lojasocial.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.lojasocial.app.domain.product.Product
import com.lojasocial.app.domain.product.ProductCategory
import com.lojasocial.app.utils.AppConstants
import com.lojasocial.app.utils.FileUtils

/**
 * Composable that displays a product image, handling both imageUrl and serializedImage.
 * 
 * Priority (optimized for performance):
 * 1. If preDecodedBitmap is provided, use it (for performance when same image is used multiple times)
 * 2. If imageUrl exists and is not empty, load it via AsyncImage (faster than Base64 decoding)
 * 3. If serializedImage (Base64) exists, decode and display it (only if no imageUrl)
 * 4. Fallback to default product image
 * 
 * @param product The product to display image for
 * @param preDecodedBitmap Optional pre-decoded bitmap (for performance optimization)
 * @param modifier Modifier for the image container
 * @param contentScale How to scale the image
 * @param contentDescription Description for accessibility
 */
@Composable
fun ProductImage(
    product: Product?,
    preDecodedBitmap: androidx.compose.ui.graphics.ImageBitmap? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    
    val imageUrl = product?.imageUrl?.takeIf { it.isNotEmpty() }
    val hasImageUrl = !imageUrl.isNullOrEmpty()
    val hasSerializedImage = !product?.serializedImage.isNullOrBlank()
    
    android.util.Log.d("ProductImage", "Product: ${product?.name}, hasImageUrl: $hasImageUrl, hasSerializedImage: $hasSerializedImage, serializedImage length: ${product?.serializedImage?.length ?: 0}")
    
    // Only decode Base64 if there's no imageUrl (imageUrl is faster)
    var imageBitmap by remember(product?.serializedImage, hasImageUrl) { 
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) 
    }
    
    LaunchedEffect(product?.serializedImage, hasImageUrl) {
        android.util.Log.d("ProductImage", "LaunchedEffect triggered - hasImageUrl: $hasImageUrl, serializedImage: ${if (product?.serializedImage != null) "present (${product.serializedImage.length} chars)" else "null"}")
        // Skip Base64 decoding if we have imageUrl or pre-decoded bitmap
        if (preDecodedBitmap == null && !hasImageUrl) {
            imageBitmap = null // Reset when product changes
            product?.serializedImage?.let { base64 ->
                if (base64.isNotBlank()) {
                    android.util.Log.d("ProductImage", "Starting Base64 decode...")
                    // Decode on background thread
                    imageBitmap = withContext(Dispatchers.IO) {
                        try {
                            val bytes = FileUtils.convertBase64ToFile(base64).getOrNull()
                            android.util.Log.d("ProductImage", "Base64 decoded to ${bytes?.size ?: 0} bytes")
                            bytes?.let {
                                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
                                android.util.Log.d("ProductImage", "Bitmap decoded: ${if (bitmap != null) "success" else "failed"}")
                                bitmap
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ProductImage", "Error decoding Base64 image: ${e.message}", e)
                            null
                        }
                    }
                    android.util.Log.d("ProductImage", "Base64 decode completed, imageBitmap: ${if (imageBitmap != null) "set" else "null"}")
                } else {
                    android.util.Log.d("ProductImage", "serializedImage is blank")
                }
            } ?: run {
                android.util.Log.d("ProductImage", "No serializedImage in product")
            }
        } else {
            // Clear decoded image if imageUrl is available
            android.util.Log.d("ProductImage", "Skipping Base64 decode - preDecodedBitmap: ${preDecodedBitmap != null}, hasImageUrl: $hasImageUrl")
            imageBitmap = null
        }
    }
    
    val category = product?.let { ProductCategory.fromId(it.category) }
    val categoryIcon = when (category) {
        ProductCategory.ALIMENTAR -> Icons.Default.Restaurant
        ProductCategory.HIGIENE_PESSOAL -> Icons.Default.Spa
        ProductCategory.CASA -> Icons.Default.Home
        null -> Icons.Default.ShoppingCart
    }
    
    Box(modifier = modifier) {
        // Use pre-decoded bitmap if available, otherwise use decoded bitmap
        val currentBitmap = preDecodedBitmap ?: imageBitmap
        when {
            // Priority 1: Pre-decoded or serialized image (Base64) - only if no imageUrl
            currentBitmap != null && !hasImageUrl -> {
                Image(
                    bitmap = currentBitmap,
                    contentDescription = contentDescription ?: product?.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
            // Priority 2: Image URL (faster than Base64 decoding)
            hasImageUrl -> {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = contentDescription ?: product?.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    error = rememberVectorPainter(categoryIcon)
                )
            }
            // Priority 3: Base64 decoded image (fallback if no imageUrl)
            currentBitmap != null -> {
                Image(
                    bitmap = currentBitmap,
                    contentDescription = contentDescription ?: product?.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
            // Priority 4: Default image
            else -> {
                AsyncImage(
                    model = AppConstants.DEFAULT_PRODUCT_IMAGE_URL,
                    contentDescription = contentDescription ?: product?.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
        }
    }
}
