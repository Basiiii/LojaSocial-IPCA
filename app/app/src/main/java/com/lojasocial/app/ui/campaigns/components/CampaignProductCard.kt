package com.lojasocial.app.ui.campaigns.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lojasocial.app.domain.product.ProductCategory
import com.lojasocial.app.repository.audit.CampaignProductReceipt
import com.lojasocial.app.ui.theme.*
import com.lojasocial.app.utils.AppConstants
import com.lojasocial.app.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Card component displaying a product received in a campaign.
 * Similar to ExpiringItemCard but without expiry date information.
 */
@Composable
fun CampaignProductCard(receipt: CampaignProductReceipt) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "PT")) }
    val formattedDate = receipt.timestamp?.let { dateFormatter.format(it) }
    
    // Determine fallback icon based on product category
    val productCategory = receipt.product?.let { ProductCategory.fromId(it.category) }
    val fallbackIcon: ImageVector = when (productCategory) {
        ProductCategory.ALIMENTAR -> Icons.Default.Restaurant
        ProductCategory.HIGIENE_PESSOAL -> Icons.Default.Spa
        ProductCategory.CASA -> Icons.Default.CleaningServices
        null -> Icons.Default.Work
    }
    
    // Handle image loading: prioritize imageUrl, then serializedImage (base64), then default
    val imageUrl = receipt.product?.imageUrl?.takeIf { it.isNotBlank() }
    val hasImageUrl = !imageUrl.isNullOrEmpty()
    val hasSerializedImage = !receipt.product?.serializedImage.isNullOrBlank()
    
    // Decode Base64 image if no imageUrl is available
    var imageBitmap by remember(receipt.product?.serializedImage, hasImageUrl) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    
    LaunchedEffect(receipt.product?.serializedImage, hasImageUrl) {
        // Only decode Base64 if there's no imageUrl
        if (!hasImageUrl) {
            imageBitmap = null // Reset when product changes
            receipt.product?.serializedImage?.let { base64 ->
                if (base64.isNotBlank()) {
                    // Decode on background thread
                    imageBitmap = withContext(Dispatchers.IO) {
                        try {
                            val bytes = FileUtils.convertBase64ToFile(base64).getOrNull()
                            bytes?.let {
                                BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }
        } else {
            // Clear decoded image if imageUrl is available
            imageBitmap = null
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LojaSocialSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Product Image with support for URL, Base64, and fallback
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BorderColor)
            ) {
                when {
                    // Priority 1: Base64 decoded image (only if no imageUrl)
                    imageBitmap != null && !hasImageUrl -> {
                        Image(
                            bitmap = imageBitmap!!,
                            contentDescription = receipt.product?.name ?: AppConstants.DEFAULT_PRODUCT_CONTENT_DESCRIPTION,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Priority 2: Image URL
                    hasImageUrl -> {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = receipt.product?.name ?: AppConstants.DEFAULT_PRODUCT_CONTENT_DESCRIPTION,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(fallbackIcon),
                            placeholder = rememberVectorPainter(fallbackIcon)
                        )
                    }
                    // Priority 3: Base64 decoded image (fallback if no imageUrl)
                    imageBitmap != null -> {
                        Image(
                            bitmap = imageBitmap!!,
                            contentDescription = receipt.product?.name ?: AppConstants.DEFAULT_PRODUCT_CONTENT_DESCRIPTION,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Priority 4: Default image
                    else -> {
                        AsyncImage(
                            model = AppConstants.DEFAULT_PRODUCT_IMAGE_URL,
                            contentDescription = receipt.product?.name ?: AppConstants.DEFAULT_PRODUCT_CONTENT_DESCRIPTION,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(fallbackIcon),
                            placeholder = rememberVectorPainter(fallbackIcon)
                        )
                    }
                }
            }

            // Product Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = receipt.product?.name ?: "Produto desconhecido",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (receipt.product?.brand?.isNotEmpty() == true) {
                    Text(
                        text = receipt.product.brand,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Quantidade: ${receipt.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
                
                if (formattedDate != null) {
                    Text(
                        text = "Adicionado em: $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }
        }
    }
}
