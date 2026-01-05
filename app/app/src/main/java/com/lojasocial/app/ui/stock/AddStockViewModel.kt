package com.lojasocial.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.api.BarcodeProduct
import com.lojasocial.app.data.model.Campaign
import com.lojasocial.app.data.model.Product
import com.lojasocial.app.repository.CampaignRepository
import com.lojasocial.app.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddStockViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val campaignRepository: CampaignRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(AddStockUiState())
    val uiState: StateFlow<AddStockUiState> = _uiState.asStateFlow()
    
    // Product data
    private val _productData = MutableStateFlow<Product?>(null)
    val productData: StateFlow<Product?> = _productData.asStateFlow()
    
    // Form fields
    private val _barcode = MutableStateFlow("")
    val barcode: StateFlow<String> = _barcode.asStateFlow()
    
    private val _productName = MutableStateFlow("")
    val productName: StateFlow<String> = _productName.asStateFlow()
    
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
        
        Log.d("AddStockViewModel", "Fetching product data for barcode: $barcode")
        Log.d("AddStockViewModel", "Current product name before API call: ${_productName.value}")
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                Log.d("AddStockViewModel", "First checking Firestore for barcode...")

                // First try to get from Firestore
                val firestoreProduct = productRepository.getProductFromFirestore(barcode)

                if (firestoreProduct != null) {
                    Log.d("AddStockViewModel", "SUCCESS: Product found in Firestore")
                    _productName.value = firestoreProduct.name
                    _productData.value = firestoreProduct
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                } else {
                    Log.d("AddStockViewModel", "Product not found in Firestore, calling external API...")

                    // If not found in Firestore, call external API
                    productRepository.getProductByBarcode(barcode)
                        .onSuccess { apiProduct ->
                            Log.d("AddStockViewModel", "SUCCESS: Product data received from API")
                            Log.d("AddStockViewModel", "Product title: ${apiProduct.title}")
                            Log.d("AddStockViewModel", "Product brand: ${apiProduct.brand}")
                            Log.d("AddStockViewModel", "Product category: ${apiProduct.category}")
                            Log.d("AddStockViewModel", "Product description: ${apiProduct.description}")

                            _productName.value = apiProduct.title

                            // Convert API product to Firestore product format
                            val firestoreProduct = Product(
                                name = apiProduct.title,
                                brand = apiProduct.brand ?: "",
                                category = apiProduct.category ?: "",
                                imageUrl = apiProduct.imageUrl ?: "",
                                quantity = 0, // Will be set by user
                                campaignId = null, // Will be set by user
                                stockBatches = emptyMap() // Will be set when adding to stock
                            )

                            _productData.value = firestoreProduct
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
                            Log.e("AddStockViewModel", "Error type: ${error::class.java.simpleName}")

                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Product not found: ${error.message}"
                            )
                        }
                }
            } catch (e: Exception) {
                Log.e("AddStockViewModel", "EXCEPTION: Exception during product fetch", e)
                Log.e("AddStockViewModel", "Exception message: ${e.message}")
                Log.e("AddStockViewModel", "Exception stack trace: ${e.stackTraceToString()}")
                
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
                val campaignId = _campaign.value

                if (currentProduct == null) {
                    _uiState.value = _uiState.value.copy(
                        error = "No product data available"
                    )
                    return@launch
                }

                if (currentBarcode.isEmpty() || quantity <= 0 || expiryDate == "mm/dd/aaaa") {
                    _uiState.value = _uiState.value.copy(
                        error = "Please fill in all required fields"
                    )
                    return@launch
                }

                // Create or update product in Firestore
                val updatedProduct = currentProduct.copy(
                    quantity = currentProduct.quantity + quantity,
                    campaignId = campaignId.ifEmpty { null },
                    stockBatches = currentProduct.stockBatches + (expiryDate to quantity)
                )

                productRepository.saveProductToFirestore(currentBarcode, updatedProduct)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Produto adicionado com sucesso!"
                )

                // Reset form
                _quantity.value = "0"
                _expiryDate.value = "mm/dd/aaaa"
                _campaign.value = ""

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
