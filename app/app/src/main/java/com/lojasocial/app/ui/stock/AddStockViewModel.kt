package com.lojasocial.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.api.BarcodeProduct
import com.lojasocial.app.domain.campaign.Campaign
import com.lojasocial.app.domain.product.Product
import com.lojasocial.app.domain.stock.StockItem
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
class AddStockViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val stockItemRepository: StockItemRepository,
    private val campaignRepository: CampaignRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(AddStockUiState())
    val uiState: StateFlow<AddStockUiState> = _uiState.asStateFlow()
    
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
        Log.d("AddStockViewModel", "AddStockViewModel constructor called!")
        Log.d("AddStockViewModel", "AddStockViewModel initialized!")
        loadCampaigns()
    }
    
    private fun loadCampaigns() {
        viewModelScope.launch {
            try {
                Log.d("AddStockViewModel", "Starting to load campaigns...")
                val campaignList = campaignRepository.getActiveAndRecentCampaigns()
                _campaigns.value = campaignList
                Log.d("AddStockViewModel", "Loaded ${campaignList.size} campaigns into ViewModel")
                campaignList.forEachIndexed { index, campaign ->
                    Log.d("AddStockViewModel", "Campaign $index: ${campaign.name} (id: ${campaign.id})")
                }
            } catch (e: Exception) {
                Log.e("AddStockViewModel", "Error loading campaigns", e)
            }
        }
    }
    
    fun onBarcodeScanned(barcode: String) {
        try {
            Log.d("AddStockViewModel", "Barcode scanned: $barcode")
            _barcode.value = barcode
            // Only fetch product data if not in manual mode
            if (!_isManualMode.value) {
                fetchProductData(barcode)
            }
        } catch (e: Exception) {
            Log.e("AddStockViewModel", "Error processing barcode", e)
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
        Log.d("AddStockViewModel", "setManualMode called with: $isManual")
        Log.d("AddStockViewModel", "Previous manual mode: ${_isManualMode.value}")
        _isManualMode.value = isManual
        Log.d("AddStockViewModel", "New manual mode: ${_isManualMode.value}")
        if (isManual) {
            // Clear product data when entering manual mode
            Log.d("AddStockViewModel", "Clearing product data for manual mode")
            _productData.value = null
            _stockItemData.value = null
            
            // Clear all product fields
            _productName.value = ""
            _productBrand.value = ""
            _productCategory.value = 1 // Default to Alimentar
            _productImageUrl.value = ""
            
            // Also clear any loading state
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    fun fetchProductDataForCurrentBarcode() {
        val currentBarcode = _barcode.value
        if (currentBarcode.isNotEmpty() && !_isManualMode.value) {
            fetchProductData(currentBarcode)
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
    
    private fun fetchProductData(barcode: String) {
        Log.d("AddStockViewModel", "!!! fetchProductData ENTRY !!! barcode: $barcode")
        
        if (barcode.isEmpty()) {
            Log.w("AddStockViewModel", "Empty barcode, skipping API call")
            return
        }
        
        Log.d("AddStockViewModel", "fetchProductData called for barcode: $barcode")
        Log.d("AddStockViewModel", "Current manual mode: ${_isManualMode.value}")
        Log.d("AddStockViewModel", "Current loading state: ${_uiState.value.isLoading}")
        
        if (_isManualMode.value) {
            Log.d("AddStockViewModel", "Skipping fetchProductData - in manual mode")
            return
        }
        
        Log.d("AddStockViewModel", "=== STARTING FETCH PROCESS FOR BARCODE: $barcode ===")
        Log.d("AddStockViewModel", "Fetching product data for barcode: $barcode")
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                Log.d("AddStockViewModel", "=== ABOUT TO CALL FIRESTORE ===")
                Log.d("AddStockViewModel", "Getting product from Firestore using barcode as document ID...")
                Log.d("AddStockViewModel", "Barcode to lookup: $barcode")
                
                // First try to get product from Firestore using barcode as document ID
                val product = productRepository.getProductByBarcodeId(barcode)
                
                Log.d("AddStockViewModel", "=== FIRESTORE CALL COMPLETED ===")
                Log.d("AddStockViewModel", "Firestore lookup completed. Product result: $product")

                if (product != null) {
                    Log.d("AddStockViewModel", "SUCCESS: Product found in Firestore")
                    Log.d("AddStockViewModel", "Product: ${product.name}, Brand: ${product.brand}")
                    
                    _productData.value = product
                    
                    // Populate all product fields
                    _productName.value = product.name
                    _productBrand.value = product.brand
                    _productCategory.value = product.category
                    _productImageUrl.value = product.imageUrl
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                } else {
                    Log.d("AddStockViewModel", "=== FIRESTORE FAILED, CALLING API ===")
                    Log.d("AddStockViewModel", "Product not found in Firestore, calling external API...")

                    // If not found in Firestore, call external API
                    productRepository.getProductByBarcode(barcode)
                        .onSuccess { apiProduct ->
                            Log.d("AddStockViewModel", "SUCCESS: Product data received from API")
                            Log.d("AddStockViewModel", "Product title: ${apiProduct.title}")
                            Log.d("AddStockViewModel", "Product brand: ${apiProduct.brand}")
                            Log.d("AddStockViewModel", "Product category: ${apiProduct.category}")

                            _productName.value = apiProduct.title

                            // Convert API product to Product format
                            val newProduct = Product(
                                name = apiProduct.title,
                                brand = apiProduct.brand ?: "",
                                category = 1, // Default to Alimentar
                                imageUrl = apiProduct.imageUrl ?: ""
                            )

                            _productData.value = newProduct
                            
                            // Populate all product fields from API data
                            _productBrand.value = apiProduct.brand ?: ""
                            _productCategory.value = 1 // Default to Alimentar
                            _productImageUrl.value = apiProduct.imageUrl ?: ""
                            Log.d("AddStockViewModel", "Updated product name in StateFlow: ${_productName.value}")

                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = null
                            )
                            Log.d("AddStockViewModel", "UI State updated with product data")
                        }
                        .onFailure { error ->
                            Log.e("AddStockViewModel", "FAILURE: Failed to fetch product data from API")
                            Log.e("AddStockViewModel", "Error message: ${error.message}")

                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Product not found: ${error.message}"
                            )
                        }
                }
            } catch (e: Exception) {
                Log.e("AddStockViewModel", "EXCEPTION: Exception during product fetch", e)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Network error: ${e.message}"
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
    
    fun addToStock() {
        viewModelScope.launch {
            try {
                val currentProduct = _productData.value
                val currentBarcode = _barcode.value
                val quantity = _quantity.value.toIntOrNull() ?: 0
                val expiryDate = _expiryDate.value
                val campaignId = _campaign.value.ifEmpty { null }

                if (currentBarcode.isEmpty() || quantity <= 0) {
                    _uiState.value = _uiState.value.copy(
                        error = "Please fill in all required fields"
                    )
                    return@launch
                }

                // Parse expiry date - allow null if not applicable ("Sem Validade")
                // "mm/dd/aaaa" means date is required but not filled, so show error
                val parsedExpiryDate: Date? = when {
                    expiryDate == "Sem Validade" -> {
                        // Expiry date not applicable (toggle is off)
                        null
                    }
                    expiryDate == "mm/dd/aaaa" || expiryDate.isEmpty() -> {
                        // Date is required but not filled
                        _uiState.value = _uiState.value.copy(
                            error = "Por favor, preencha a data de validade ou desative o campo"
                        )
                        return@launch
                    }
                    else -> {
                        // Try to parse the date
                        try {
                            val parts = expiryDate.split("/")
                            if (parts.size == 3) {
                                Date(parts[2].toInt() - 1900, parts[1].toInt() - 1, parts[0].toInt())
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    error = "Formato de data inválido. Use DD/MM/AAAA"
                                )
                                return@launch
                            }
                        } catch (e: Exception) {
                            _uiState.value = _uiState.value.copy(
                                error = "Formato de data inválido. Use DD/MM/AAAA"
                            )
                            return@launch
                        }
                    }
                }

                // Save or update product using barcode as document ID
                val productToSave = Product(
                    name = _productName.value,
                    brand = _productBrand.value,
                    category = _productCategory.value,
                    imageUrl = _productImageUrl.value
                )
                productRepository.saveOrUpdateProduct(productToSave, currentBarcode)

                // Create stock item
                val stockItem = StockItem(
                    barcode = currentBarcode,
                    campaignId = campaignId,
                    createdAt = Date(),
                    expirationDate = parsedExpiryDate,
                    quantity = quantity,
                    productId = currentBarcode // Use barcode as product ID reference
                )

                productRepository.saveStockItem(stockItem)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Produto adicionado com sucesso!"
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

            } catch (e: Exception) {
                Log.e("AddStockViewModel", "Error adding to stock", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error adding to stock: ${e.message}"
                )
            }
        }
    }
}

// UI State data class
data class AddStockUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
