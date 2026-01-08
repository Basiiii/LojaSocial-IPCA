package com.lojasocial.app.ui.expiringitems

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.domain.stock.ExpiringItemWithProduct
import com.lojasocial.app.domain.stock.ExpiringItemsUiState
import com.lojasocial.app.repository.product.ProductRepository
import com.lojasocial.app.repository.product.StockItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * ViewModel for managing expiring items state and business logic.
 * 
 * This ViewModel is responsible for:
 * - Loading stock items that are expiring within the threshold period (3 days)
 * - Fetching product information for each expiring item
 * - Calculating days until expiration for each item
 * - Sorting items by urgency (soonest expiration first)
 * - Managing loading, error, and success states
 * 
 * The ViewModel uses StateFlow to expose UI state, making it easy for the View
 * to observe and react to changes reactively.
 * 
 * @param stockItemRepository Repository for accessing stock item data
 * @param productRepository Repository for accessing product information
 */

@HiltViewModel
class ExpiringItemsViewModel @Inject constructor(
    private val stockItemRepository: StockItemRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpiringItemsUiState(isLoading = true))
    val uiState: StateFlow<ExpiringItemsUiState> = _uiState.asStateFlow()

    init {
        loadExpiringItems()
    }

    /**
     * Loads expiring items from the repository and enriches them with product information.
     * 
     * This method:
     * 1. Queries stock items expiring within 3 days
     * 2. Fetches product details for each item using the barcode
     * 3. Calculates days until expiration
     * 4. Sorts items by urgency (soonest first)
     * 5. Updates the UI state accordingly
     * 
     * Errors are caught and displayed to the user via the error state.
     */
    fun loadExpiringItems() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Get expiring items (within 3 days)
                val expiringItems = stockItemRepository.getExpiringItems(3)
                Log.d("ExpiringItemsViewModel", "Found ${expiringItems.size} expiring items")
                
                // Fetch product details for each item
                val itemsWithProducts = expiringItems.map { stockItem ->
                    val product = if (stockItem.barcode.isNotEmpty()) {
                        productRepository.getProductByBarcodeId(stockItem.barcode)
                    } else {
                        null
                    }
                    
                    // Calculate days until expiration
                    val daysUntilExpiration = stockItem.expirationDate?.let { expDate ->
                        val now = Calendar.getInstance()
                        val expiration = Calendar.getInstance().apply {
                            time = expDate
                        }
                        val diffInMillis = expiration.timeInMillis - now.timeInMillis
                        (diffInMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                    } ?: 0
                    
                    ExpiringItemWithProduct(
                        stockItem = stockItem,
                        product = product,
                        daysUntilExpiration = daysUntilExpiration
                    )
                }
                
                // Sort by expiration date (soonest first)
                val sortedItems = itemsWithProducts.sortedBy { it.daysUntilExpiration }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    items = sortedItems,
                    error = null
                )
            } catch (e: Exception) {
                Log.e("ExpiringItemsViewModel", "Error loading expiring items", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar itens: ${e.message}"
                )
            }
        }
    }

    /**
     * Refreshes the expiring items list.
     * 
     * This method reloads all expiring items from the repository, useful for
     * manual refresh actions or after retrying from an error state.
     */
    fun refresh() {
        loadExpiringItems()
    }
}
