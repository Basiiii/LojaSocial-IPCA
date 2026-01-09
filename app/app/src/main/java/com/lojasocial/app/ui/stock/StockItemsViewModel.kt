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
                
                // Get product
                val product = productRepository.getProductByBarcodeId(barcode)
                
                // Get stock items for this barcode
                val stockItems = stockItemRepository.getStockItemsByBarcode(barcode)
                Log.d("StockItemsViewModel", "Found ${stockItems.size} stock items for barcode: $barcode")
                
                // Helper function to normalize date to start of day for comparison
                fun normalizeDate(date: Date): Date {
                    val cal = Calendar.getInstance().apply {
                        time = date
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    return cal.time
                }
                
                // Helper function to check if two dates are on the same day
                fun isSameDay(date1: Date, date2: Date): Boolean {
                    val cal1 = Calendar.getInstance().apply { time = date1 }
                    val cal2 = Calendar.getInstance().apply { time = date2 }
                    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
                }
                
                // Group items that have no expiry date, same campaign, and same added date
                val groupedItems = mutableListOf<StockItem>()
                val processedIds = mutableSetOf<String>()
                
                stockItems.forEach { item ->
                    // Skip if already processed
                    if (processedIds.contains(item.id)) return@forEach
                    
                    // Check if this item should be grouped (no expiry date and has campaign)
                    if (item.expirationDate == null && item.campaignId != null) {
                        // Find all items with same campaign, no expiry date, and same added date
                        val itemsToGroup = stockItems.filter { 
                            it.expirationDate == null && 
                            it.campaignId == item.campaignId &&
                            isSameDay(it.createdAt, item.createdAt) &&
                            !processedIds.contains(it.id)
                        }
                        
                        if (itemsToGroup.size > 1) {
                            // Group them: combine quantities and use the same createdAt date
                            val combinedQuantity = itemsToGroup.sumOf { it.quantity }
                            
                            // Create a grouped item (use the first item's ID as representative)
                            val groupedItem = item.copy(
                                quantity = combinedQuantity,
                                createdAt = item.createdAt // Keep the same date since they're all the same day
                            )
                            groupedItems.add(groupedItem)
                            
                            // Mark all grouped items as processed
                            itemsToGroup.forEach { processedIds.add(it.id) }
                        } else {
                            // Single item, add as-is
                            groupedItems.add(item)
                            processedIds.add(item.id)
                        }
                    } else {
                        // Item has expiry date or no campaign, add as-is
                        groupedItems.add(item)
                        processedIds.add(item.id)
                    }
                }
                
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
