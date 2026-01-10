package com.lojasocial.app.ui.expiringitems.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lojasocial.app.domain.product.Product
import com.lojasocial.app.domain.stock.StockItem
import com.lojasocial.app.domain.stock.ExpiringItemWithProduct
import com.lojasocial.app.ui.theme.*
import com.lojasocial.app.ui.theme.LojaSocialTheme
import com.lojasocial.app.utils.AppConstants
import java.text.SimpleDateFormat
import java.util.*

/**
 * Card component displaying an expiring item with comprehensive product information.
 * 
 * This component presents a single expiring stock item in a visually appealing card format.
 * It displays all relevant information administrators need to make decisions about
 * expiring inventory:
 * 
 * - **Product Image**: Visual representation of product (with fallback to default image)
 * - **Product Name**: Full name of product
 * - **Brand**: Product brand name (if available)
 * - **Expiration Badge**: Color-coded urgency indicator showing days until expiration
 *   - Red: Expires today (0 days)
 *   - Orange: Expires tomorrow (1 day)
 *   - Orange: Expires in 2-3 days
 * - **Quantity**: Current stock quantity
 * - **Expiration Date**: Formatted expiration date (DD/MM/YYYY)
 * - **Quantity Selection**: Interactive controls to select quantities for action
 * 
 * The urgency color provides immediate visual feedback about how critical expiration is,
 * helping administrators prioritize which items need attention first.
 * 
 * @param item The expiring item with product information to display. Contains stock item data,
 *             product details, and calculated days until expiration.
 * @param selectedQuantity Currently selected quantity for this item
 * @param onQuantityIncrease Callback when quantity is increased
 * @param onQuantityDecrease Callback when quantity is decreased
 * @param maxQuantity Maximum quantity that can be selected (defaults to available stock)
 * 
 * @see ExpiringItemWithProduct The domain model for expiring items
 * @see ExpirationBadge The badge component showing expiration urgency
 */
@Composable
fun ExpiringItemCard(
    item: ExpiringItemWithProduct,
    selectedQuantity: Int = 0,
    onQuantityIncrease: () -> Unit = {},
    onQuantityDecrease: () -> Unit = {},
    maxQuantity: Int = item.stockItem.quantity
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val expirationDateText = item.stockItem.expirationDate?.let { dateFormat.format(it) } ?: "Sem data"
    
    val urgencyColor = when {
        item.daysUntilExpiration == 0 -> ScanRed
        item.daysUntilExpiration == 1 -> Color(0xFFFF9800) // Orange
        else -> BrandOrange
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LojaSocialSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Product Image
                AsyncImage(
                    model = item.product?.imageUrl ?: AppConstants.DEFAULT_PRODUCT_IMAGE_URL,
                    contentDescription = item.product?.name ?: AppConstants.DEFAULT_PRODUCT_CONTENT_DESCRIPTION,
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
                        text = item.product?.name ?: "Produto desconhecido",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (item.product?.brand?.isNotEmpty() == true) {
                        Text(
                            text = item.product.brand,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpirationBadge(
                            daysUntilExpiration = item.daysUntilExpiration,
                            urgencyColor = urgencyColor
                        )

                        Text(
                            text = "•",
                            color = TextGray
                        )

                        Text(
                            text = "Qtd: ${item.stockItem.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }

                    Text(
                        text = "Validade: $expirationDateText",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }

            // Quantity Selection Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selecionar quantidade:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuantityButton(
                        icon = Icons.Default.Remove,
                        onClick = onQuantityDecrease,
                        enabled = selectedQuantity > 0,
                        isRemoveButton = true
                    )
                    Text(
                        text = "$selectedQuantity",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.width(30.dp),
                        textAlign = TextAlign.Center
                    )
                    QuantityButton(
                        icon = Icons.Default.Add,
                        onClick = onQuantityIncrease,
                        enabled = selectedQuantity < maxQuantity,
                        isRemoveButton = false
                    )
                }
            }
        }
    }
}

/**
 * Badge component showing the expiration urgency status.
 * 
 * Displays a color-coded badge indicating how many days remain until the item expires.
 * The text changes based on urgency:
 * - "Expira hoje!" for items expiring today (0 days)
 * - "Expira amanhã" for items expiring tomorrow (1 day)
 * - "{n} dias" for items expiring in 2+ days
 * 
 * The badge uses a semi-transparent background of the urgency color for visual emphasis
 * while maintaining readability.
 * 
 * @param daysUntilExpiration Number of days until expiration (0 = today, 1 = tomorrow, etc.)
 * @param urgencyColor Color to use for the badge based on urgency level
 */
@Composable
private fun ExpirationBadge(
    daysUntilExpiration: Int,
    urgencyColor: Color
) {
    Surface(
        color = urgencyColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = when {
                daysUntilExpiration == 0 -> "Expira hoje!"
                daysUntilExpiration == 1 -> "Expira amanhã"
                else -> "$daysUntilExpiration dias"
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = urgencyColor
        )
    }
}

/**
 * Quantity selection button component.
 * 
 * Provides a circular button for increasing or decreasing item quantities with
 * consistent styling and proper disabled states.
 * 
 * @param icon The icon to display (Add or Remove)
 * @param onClick Callback when button is clicked
 * @param enabled Whether the button is enabled
 * @param isRemoveButton Whether this is a remove button (affects disabled styling)
 */
@Composable
private fun QuantityButton(
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    isRemoveButton: Boolean
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = LojaSocialPrimary,
            contentColor = Color.White,
            disabledContainerColor = if (isRemoveButton) LojaSocialPrimary else InactiveFilterBackground,
            disabledContentColor = if (isRemoveButton) Color.White else TextGray
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

/**
 * Preview composable for ExpiringItemCard component with item expiring today.
 */
@Preview(showBackground = true)
@Composable
fun ExpiringItemCardTodayPreview() {
    LojaSocialTheme {
        ExpiringItemCard(
            item = ExpiringItemWithProduct(
                stockItem = StockItem(
                    id = "1",
                    barcode = "123456789",
                    quantity = 5,
                    expirationDate = Date(System.currentTimeMillis())
                ),
                product = Product(
                    id = "123456789",
                    name = "Leite UHT",
                    brand = "Marca Z",
                    category = 1,
                    imageUrl = "https://example.com/leite.jpg"
                ),
                daysUntilExpiration = 0
            )
        )
    }
}

/**
 * Preview composable for the ExpiringItemCard component with item expiring tomorrow.
 */
@Preview(showBackground = true)
@Composable
fun ExpiringItemCardTomorrowPreview() {
    LojaSocialTheme {
        ExpiringItemCard(
            item = ExpiringItemWithProduct(
                stockItem = StockItem(
                    id = "2",
                    barcode = "987654321",
                    quantity = 10,
                    expirationDate = Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)
                ),
                product = Product(
                    id = "987654321",
                    name = "Arroz Agulha",
                    brand = "Marca X",
                    category = 1,
                    imageUrl = "https://example.com/arroz.jpg"
                ),
                daysUntilExpiration = 1
            )
        )
    }
}

/**
 * Preview composable for the ExpiringItemCard component with item expiring in 3 days.
 */
@Preview(showBackground = true)
@Composable
fun ExpiringItemCardThreeDaysPreview() {
    LojaSocialTheme {
        ExpiringItemCard(
            item = ExpiringItemWithProduct(
                stockItem = StockItem(
                    id = "3",
                    barcode = "555555555",
                    quantity = 15,
                    expirationDate = Date(System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000)
                ),
                product = Product(
                    id = "555555555",
                    name = "Azeite Extra Virgem",
                    brand = "Marca Y",
                    category = 1,
                    imageUrl = "https://example.com/azeite.jpg"
                ),
                daysUntilExpiration = 3
            )
        )
    }
}

/**
 * Preview composable for the ExpiringItemCard component with missing product information.
 */
@Preview(showBackground = true)
@Composable
fun ExpiringItemCardNoProductPreview() {
    LojaSocialTheme {
        ExpiringItemCard(
            item = ExpiringItemWithProduct(
                stockItem = StockItem(
                    id = "4",
                    barcode = "999999999",
                    quantity = 8,
                    expirationDate = Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000)
                ),
                product = null, // Product not found
                daysUntilExpiration = 2
            )
        )
    }
}
