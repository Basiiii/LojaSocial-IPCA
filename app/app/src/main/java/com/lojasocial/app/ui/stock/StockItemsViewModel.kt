package com.lojasocial.app.ui.stock

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.domain.product.Product
import com.lojasocial.app.domain.stock.StockItem
import com.lojasocial.app.domain.campaign.Campaign
import com.lojasocial.app.repository.campaign.CampaignRepository
import com.lojasocial.app.repository.product.ProductRepository
import com.lojasocial.app.repository.product.StockItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

data class StockItemWithProduct(
    val stockItem: StockItem,
    val product: Product?
)

data class StockItemsUiState(
    val isLoading: Boolean = true,
    val items: List<StockItemWithProduct> = emptyList(),
    val product: Product? = null,
    val error: String? = null
)

data class CampaignState(
    val isLoading: Boolean = false,
    val campaign: Campaign? = null,
    val error: String? = null
)

@HiltViewModel
class StockItemsViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val stockItemRepository: StockItemRepository,
    private val campaignRepository: CampaignRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockItemsUiState(isLoading = true))
    val uiState: StateFlow<StockItemsUiState> = _uiState.asStateFlow()
    
    private val _campaignState = MutableStateFlow(CampaignState())
    val campaignState: StateFlow<CampaignState> = _campaignState.asStateFlow()

    fun loadStockItems(barcode: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Fetch product and stock items in parallel for better performance
                val productDeferred = async {
                    productRepository.getProductByBarcodeId(barcode)
                }
                val stockItemsDeferred = async {
                    stockItemRepository.getStockItemsByBarcode(barcode)
                }
                
                // Wait for both to complete
                val product = productDeferred.await()
                val stockItems = stockItemsDeferred.await()
                Log.d("StockItemsViewModel", "Found ${stockItems.size} stock items for barcode: $barcode")
                
                // Optimized grouping: Use groupBy for O(n) complexity instead of O(n²)
                // Helper function to get a day key for grouping (optimized - single Calendar operation)
                fun getDayKey(date: Date): Long {
                    val cal = Calendar.getInstance().apply { time = date }
                    return (cal.get(Calendar.YEAR) * 10000L + 
                            cal.get(Calendar.DAY_OF_YEAR)).toLong()
                }
                
                // Separate items into two groups: those that can be grouped and those that can't
                val itemsToGroup = mutableListOf<StockItem>()
                val itemsNotToGroup = mutableListOf<StockItem>()
                
                stockItems.forEach { item ->
                    if (item.expirationDate == null && item.campaignId != null) {
                        itemsToGroup.add(item)
                    } else {
                        itemsNotToGroup.add(item)
                    }
                }
                
                // Group items by campaign and day key (O(n) operation using groupBy)
                val groupedByCampaignAndDay = itemsToGroup.groupBy { item ->
                    Pair(item.campaignId, getDayKey(item.createdAt))
                }
                
                val groupedItems = mutableListOf<StockItem>()
                
                // Process grouped items
                groupedByCampaignAndDay.values.forEach { items ->
                    if (items.size > 1) {
                        // Combine quantities
                        val combinedQuantity = items.sumOf { it.quantity }
                        val groupedItem = items.first().copy(quantity = combinedQuantity)
                        groupedItems.add(groupedItem)
                    } else {
                        // Single item, add as-is
                        groupedItems.add(items.first())
                    }
                }
                
                // Add items that shouldn't be grouped
                groupedItems.addAll(itemsNotToGroup)
                
                // Create items with product info and sort
                val itemsWithProduct = groupedItems.map { item ->
                    StockItemWithProduct(
                        stockItem = item,
                        product = product
                    )
                }.sortedWith(compareBy(
                    // First, separate items with expiry dates (0) from those without (1)
                    { if (it.stockItem.expirationDate == null) 1 else 0 },
                    // Then sort by expiry date (earliest first) for items with expiry dates
                    { it.stockItem.expirationDate ?: Date(0) },
                    // Finally sort by added date (earliest first) for items without expiry dates
                    { it.stockItem.createdAt }
                ))
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    items = itemsWithProduct,
                    product = product,
                    error = null
                )
            } catch (e: Exception) {
                Log.e("StockItemsViewModel", "Error loading stock items", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar itens: ${e.message}"
                )
            }
        }
    }
    
    fun loadCampaign(campaignIdOrName: String) {
        viewModelScope.launch {
            try {
                Log.d("StockItemsViewModel", "Loading campaign with ID/Name: $campaignIdOrName")
                _campaignState.value = CampaignState(isLoading = true, campaign = null, error = null)
                
                // First try to get by ID, if not found, try by name
                var campaign = campaignRepository.getCampaignById(campaignIdOrName)
                if (campaign == null) {
                    Log.d("StockItemsViewModel", "Campaign not found by ID, trying by name: $campaignIdOrName")
                    campaign = campaignRepository.getCampaignByName(campaignIdOrName)
                }
                
                Log.d("StockItemsViewModel", "Campaign loaded - exists: ${campaign != null}, name: ${campaign?.name}, searched: $campaignIdOrName")
                _campaignState.value = CampaignState(
                    isLoading = false,
                    campaign = campaign,
                    error = if (campaign == null) "Campanha não encontrada: $campaignIdOrName" else null
                )
            } catch (e: Exception) {
                Log.e("StockItemsViewModel", "Error loading campaign: $campaignIdOrName", e)
                _campaignState.value = CampaignState(
                    isLoading = false,
                    campaign = null,
                    error = "Erro ao carregar campanha: ${e.message}"
                )
            }
        }
    }
    
    fun resetCampaignState() {
        _campaignState.value = CampaignState()
    }
}
