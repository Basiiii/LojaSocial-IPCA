package com.lojasocial.app.ui.campaigns.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lojasocial.app.repository.audit.CampaignProductReceipt
import com.lojasocial.app.ui.theme.*
import com.lojasocial.app.utils.AppConstants

/**
 * Card component displaying a product received in a campaign.
 * Similar to ExpiringItemCard but without expiry date information.
 */
@Composable
fun CampaignProductCard(receipt: CampaignProductReceipt) {
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
            // Product Image
            AsyncImage(
                model = receipt.product?.imageUrl ?: AppConstants.DEFAULT_PRODUCT_IMAGE_URL,
                contentDescription = receipt.product?.name ?: AppConstants.DEFAULT_PRODUCT_CONTENT_DESCRIPTION,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BorderColor),
                contentScale = ContentScale.Crop
            )

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
            }
        }
    }
}
