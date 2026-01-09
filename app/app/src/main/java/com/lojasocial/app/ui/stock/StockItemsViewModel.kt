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
                
                // Create items with product info
                val itemsWithProduct = stockItems.map { item ->
                    StockItemWithProduct(
                        stockItem = item,
                        product = product
                    )
                }.sortedByDescending { it.stockItem.createdAt }
                
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
