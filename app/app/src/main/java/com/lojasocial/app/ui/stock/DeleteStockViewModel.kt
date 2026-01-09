package com.lojasocial.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.api.BarcodeProduct
import com.lojasocial.app.domain.campaign.Campaign
import com.lojasocial.app.domain.product.Product
import com.lojasocial.app.domain.stock.StockItem
import com.lojasocial.app.repository.audit.AuditRepository
import com.lojasocial.app.repository.auth.AuthRepository
import com.lojasocial.app.repository.campaign.CampaignRepository
import com.lojasocial.app.repository.product.ProductRepository
import com.lojasocial.app.repository.product.StockItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class DeleteStockViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val stockItemRepository: StockItemRepository,
    private val campaignRepository: CampaignRepository,
    private val auditRepository: AuditRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(DeleteStockUiState())
    val uiState: StateFlow<DeleteStockUiState> = _uiState.asStateFlow()
    
    // Product data
    private val _productData = MutableStateFlow<Product?>(null)
    val productData: StateFlow<Product?> = _productData.asStateFlow()
    
    // Stock item data
    private val _stockItemData = MutableStateFlow<StockItem?>(null)
    val stockItemData: StateFlow<StockItem?> = _stockItemData.asStateFlow()
    
    // Form fields
    private val _barcode = MutableStateFlow("")
    val barcode: StateFlow<String> = _barcode.asStateFlow()
    
    private val _productName = MutableStateFlow("")
    val productName: StateFlow<String> = _productName.asStateFlow()
    
    private val _productBrand = MutableStateFlow("")
    val productBrand: StateFlow<String> = _productBrand.asStateFlow()
    
    private val _productCategory = MutableStateFlow(1) // Default to Alimentar
    val productCategory: StateFlow<Int> = _productCategory.asStateFlow()
    
    private val _productImageUrl = MutableStateFlow("")
    val productImageUrl: StateFlow<String> = _productImageUrl.asStateFlow()
    
    // Manual mode flag
    private val _isManualMode = MutableStateFlow(false)
    val isManualMode: StateFlow<Boolean> = _isManualMode.asStateFlow()
    
    private val _quantity = MutableStateFlow("0")
    val quantity: StateFlow<String> = _quantity.asStateFlow()
    
    private val _expiryDate = MutableStateFlow("mm/dd/aaaa")
    val expiryDate: StateFlow<String> = _expiryDate.asStateFlow()
    
    private val _campaign = MutableStateFlow("")
    val campaign: StateFlow<String> = _campaign.asStateFlow()
    
    // Campaigns list for dropdown
    private val _campaigns = MutableStateFlow<List<Campaign>>(emptyList())
    val campaigns: StateFlow<List<Campaign>> = _campaigns.asStateFlow()
    
    init {
        Log.d("DeleteStockViewModel", "DeleteStockViewModel constructor called!")
        Log.d("DeleteStockViewModel", "DeleteStockViewModel initialized!")
        loadCampaigns()
    }
    
    private fun loadCampaigns() {
        viewModelScope.launch {
            try {
                Log.d("DeleteStockViewModel", "Starting to load campaigns...")
                val campaignList = campaignRepository.getActiveAndRecentCampaigns()
                _campaigns.value = campaignList
                Log.d("DeleteStockViewModel", "Loaded ${campaignList.size} campaigns into ViewModel")
                campaignList.forEachIndexed { index, campaign ->
                    Log.d("DeleteStockViewModel", "Campaign $index: ${campaign.name} (id: ${campaign.id})")
                }
            } catch (e: Exception) {
                Log.e("DeleteStockViewModel", "Error loading campaigns", e)
            }
        }
    }
    
    fun onBarcodeScanned(barcode: String) {
        try {
            Log.d("DeleteStockViewModel", "Barcode scanned: $barcode")
            _barcode.value = barcode
            // Only fetch stock data if not in manual mode
            if (!_isManualMode.value) {
                fetchStockData(barcode)
            }
        } catch (e: Exception) {
            Log.e("DeleteStockViewModel", "Error processing barcode", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Error processing barcode: ${e.message}"
            )
        }
    }
    
    fun onBarcodeChanged(barcode: String) {
        _barcode.value = barcode
    }
    
    fun setManualMode(isManual: Boolean) {
        Log.d("DeleteStockViewModel", "setManualMode called with: $isManual")
        Log.d("DeleteStockViewModel", "Previous manual mode: ${_isManualMode.value}")
        _isManualMode.value = isManual
        Log.d("DeleteStockViewModel", "New manual mode: ${_isManualMode.value}")
        if (isManual) {
            // Clear data when entering manual mode
            Log.d("DeleteStockViewModel", "Clearing data for manual mode")
            _productData.value = null
            _stockItemData.value = null
            
            // Clear all product fields
            _productName.value = ""
            _productBrand.value = ""
            _productCategory.value = 1 // Default to Alimentar
            _productImageUrl.value = ""
            _quantity.value = "0"
            _expiryDate.value = "mm/dd/aaaa"
            _campaign.value = ""
            
            // Also clear any loading state
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    fun fetchStockDataForCurrentBarcode() {
        val currentBarcode = _barcode.value
        Log.d("DeleteStockViewModel", "fetchStockDataForCurrentBarcode called with barcode: $currentBarcode, manualMode: ${_isManualMode.value}")
        if (currentBarcode.isNotEmpty()) {
            // When user explicitly clicks "Procurar", always fetch regardless of manual mode
            // Temporarily set manual mode to false to allow fetching
            val wasManualMode = _isManualMode.value
            if (wasManualMode) {
                _isManualMode.value = false
            }
            fetchStockData(currentBarcode)
        }
    }
    
    fun onProductNameChanged(name: String) {
        _productName.value = name
    }
    
    fun onProductBrandChanged(brand: String) {
        _productBrand.value = brand
    }
    
    fun onProductCategoryChanged(category: Int) {
        _productCategory.value = category
    }
    
    fun onProductImageUrlChanged(imageUrl: String) {
        _productImageUrl.value = imageUrl
    }
    
    fun onQuantityChanged(quantity: String) {
        _quantity.value = quantity
    }
    
    fun onExpiryDateChanged(date: String) {
        _expiryDate.value = date
    }
    
    fun onCampaignChanged(campaign: String) {
        _campaign.value = campaign
    }
    
    private fun fetchStockData(barcode: String) {
        Log.d("DeleteStockViewModel", "!!! fetchStockData ENTRY !!! barcode: $barcode")
        
        if (barcode.isEmpty()) {
            Log.w("DeleteStockViewModel", "Empty barcode, skipping lookup")
            return
        }
        
        Log.d("DeleteStockViewModel", "fetchStockData called for barcode: $barcode")
        Log.d("DeleteStockViewModel", "Current manual mode: ${_isManualMode.value}")
        Log.d("DeleteStockViewModel", "Current loading state: ${_uiState.value.isLoading}")
        
        if (_isManualMode.value) {
            Log.d("DeleteStockViewModel", "Skipping fetchStockData - in manual mode")
            return
        }
        
        Log.d("DeleteStockViewModel", "=== STARTING FETCH PROCESS FOR BARCODE: $barcode ===")
        Log.d("DeleteStockViewModel", "Fetching stock data for barcode: $barcode")
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                Log.d("DeleteStockViewModel", "=== ABOUT TO LOOKUP STOCK ITEMS ===")
                Log.d("DeleteStockViewModel", "Getting stock items from Firestore using barcode...")
                Log.d("DeleteStockViewModel", "Barcode to lookup: $barcode")
                
                // Get stock items for this barcode
                val stockItems = stockItemRepository.getStockItemsByBarcode(barcode)
                
                Log.d("DeleteStockViewModel", "=== STOCK LOOKUP COMPLETED ===")
                Log.d("DeleteStockViewModel", "Found ${stockItems.size} stock items for barcode: $barcode")

                if (stockItems.isNotEmpty()) {
                    Log.d("DeleteStockViewModel", "SUCCESS: Stock items found")
                    
                    // Get the first stock item (could enhance to let user choose which one)
                    val stockItem = stockItems.first()
                    _stockItemData.value = stockItem
                    
                    // Get product data
                    val product = productRepository.getProductByBarcodeId(barcode)
                    _productData.value = product
                    
                    if (product != null) {
                        // Populate all product fields
                        Log.d("DeleteStockViewModel", "Setting productName to: ${product.name}")
                        _productName.value = product.name
                        _productBrand.value = product.brand
                        _productCategory.value = product.category
                        _productImageUrl.value = product.imageUrl
                    } else {
                        // Set default values if product not found
                        _productName.value = "Produto Desconhecido"
                        _productBrand.value = ""
                        _productCategory.value = 1
                        _productImageUrl.value = ""
                    }
                    
                    // Populate stock fields
                    _quantity.value = stockItem.quantity.toString()
                    _expiryDate.value = stockItem.expirationDate?.let { date ->
                        // Format date for display
                        val day = date.date
                        val month = date.month + 1
                        val year = date.year + 1900
                        String.format("%02d/%02d/%04d", day, month, year)
                    } ?: "Sem Validade"
                    
                    // Set campaign if present
                    stockItem.campaignId?.let { campaignId ->
                        val campaign = campaignRepository.getCampaignById(campaignId)
                        _campaign.value = campaign?.name ?: ""
                    }
                    
                    Log.d("DeleteStockViewModel", "All fields set. Final values - name: ${_productName.value}, quantity: ${_quantity.value}")
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                } else {
                    Log.d("DeleteStockViewModel", "No stock items found for barcode: $barcode")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Nenhum item em stock encontrado para este código de barras"
                    )
                }
            } catch (e: Exception) {
                Log.e("DeleteStockViewModel", "EXCEPTION: Exception during stock fetch", e)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao buscar dados do stock: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun onCampaignSelected(campaign: Campaign) {
        _campaign.value = campaign.name
    }
    
    fun deleteFromStock() {
        viewModelScope.launch {
            try {
                val currentStockItem = _stockItemData.value
                val currentBarcode = _barcode.value
                val currentProduct = _productData.value
                val currentUser = authRepository.getCurrentUser()

                Log.d("DeleteStockViewModel", "Current user: ${currentUser?.uid}")
                Log.d("DeleteStockViewModel", "Current user email: ${currentUser?.email}")

                if (currentBarcode.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        error = "Código de barras é obrigatório"
                    )
                    return@launch
                }

                if (currentStockItem == null) {
                    _uiState.value = _uiState.value.copy(
                        error = "Nenhum item em stock selecionado para eliminação"
                    )
                    return@launch
                }

                Log.d("DeleteStockViewModel", "Deleting stock item: ${currentStockItem.id}")
                
                // Delete the stock item
                val deleteResult = stockItemRepository.deleteStockItem(currentStockItem.id)
                Log.d("DeleteStockViewModel", "Delete result: $deleteResult")
                
                if (!deleteResult) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erro ao eliminar item do stock. Verifique se tem permissões de administrador."
                    )
                    return@launch
                }
                
                Log.d("DeleteStockViewModel", "Stock item deleted successfully")
                
                // Log audit action
                viewModelScope.launch {
                    // Build details map
                    val detailsMap = mutableMapOf<String, Any>(
                        "barcode" to currentBarcode,
                        "deletedQuantity" to currentStockItem.quantity,
                        "productName" to (currentProduct?.name ?: "Produto Desconhecido"),
                        "stockItemId" to currentStockItem.id
                    )
                    
                    // Add campaignId to details if present
                    currentStockItem.campaignId?.let { campaignId ->
                        detailsMap["campaignId"] = campaignId
                    }
                    
                    // Log delete_item action
                    auditRepository.logAction(
                        action = "delete_item",
                        userId = currentUser?.uid,
                        details = detailsMap
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Produto eliminado do stock com sucesso!"
                )

                // Reset form
                _quantity.value = "0"
                _expiryDate.value = "mm/dd/aaaa"
                _campaign.value = ""
                _productData.value = null
                _stockItemData.value = null
                
                // Clear product fields
                _productName.value = ""
                _productBrand.value = ""
                _productCategory.value = 1 // Default to Alimentar
                _productImageUrl.value = ""
                _barcode.value = ""

            } catch (e: Exception) {
                Log.e("DeleteStockViewModel", "Error deleting from stock", e)
                _uiState.value = _uiState.value.copy(
                    error = "Erro ao eliminar do stock: ${e.message}"
                )
            }
        }
    }
}

// UI State data class
data class DeleteStockUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
