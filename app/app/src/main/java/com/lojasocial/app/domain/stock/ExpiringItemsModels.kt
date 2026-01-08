package com.lojasocial.app.domain.stock

import com.lojasocial.app.domain.product.Product
import com.lojasocial.app.domain.stock.StockItem

/**
 * Represents a stock item that is expiring soon, combined with its product information.
 * 
 * This domain model combines stock item data with product details to provide a complete
 * view of items that need attention due to approaching expiration dates. It includes
 * calculated information about how many days remain until expiration.
 * 
 * This model is used in the expiring items feature to display items that are expiring
 * within a threshold period (typically 3 days) to administrators, allowing them to
 * take appropriate action before items expire.
 * 
 * @property stockItem The stock item that is expiring, containing quantity and expiration date
 * @property product The product information associated with the stock item (may be null if product not found in database)
 * @property daysUntilExpiration Number of days until the item expires (0 = expires today, 1 = expires tomorrow, etc.)
 * 
 * @see StockItem The stock item data model
 * @see Product The product data model
 */
data class ExpiringItemWithProduct(
    val stockItem: StockItem,
    val product: Product?,
    val daysUntilExpiration: Int
)

/**
 * UI state representation for the expiring items screen.
 * 
 * This data class encapsulates all the observable state needed by the expiring items
 * view to render properly. It follows the unidirectional data flow pattern where
 * the ViewModel manages this state and the View observes it.
 * 
 * The state can represent different stages:
 * - Loading: Items are being fetched from the repository
 * - Success: Items have been loaded successfully (may be empty list)
 * - Error: An error occurred while loading items
 * 
 * @property isLoading Whether the items are currently being loaded from the repository
 * @property items List of expiring items with their product information, sorted by urgency
 * @property error Error message if loading failed, null otherwise. Should be displayed to the user when not null.
 */
data class ExpiringItemsUiState(
    val isLoading: Boolean = false,
    val items: List<ExpiringItemWithProduct> = emptyList(),
    val error: String? = null
)
