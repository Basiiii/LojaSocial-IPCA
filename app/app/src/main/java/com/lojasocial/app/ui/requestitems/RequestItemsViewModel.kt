package com.lojasocial.app.ui.requestitems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.domain.request.RequestItem
import com.lojasocial.app.repository.request.ItemsRepository
import com.lojasocial.app.repository.request.RequestsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import com.google.firebase.Timestamp
import javax.inject.Inject

@HiltViewModel
class RequestItemsViewModel @Inject constructor(
    private val itemsRepository: ItemsRepository,
    private val requestsRepository: RequestsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RequestItemsUiState>(RequestItemsUiState.Loading)
    val uiState: StateFlow<RequestItemsUiState> = _uiState.asStateFlow()

    private val _productQuantities = MutableStateFlow<Map<String, Int>>(emptyMap())
    val productQuantities: StateFlow<Map<String, Int>> = _productQuantities.asStateFlow()

    private val _submissionState = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val submissionState: StateFlow<SubmissionState> = _submissionState.asStateFlow()

    private val _proposedDeliveryDate = MutableStateFlow<Date?>(null)
    val proposedDeliveryDate: StateFlow<Date?> = _proposedDeliveryDate.asStateFlow()

    private var lastVisibleId: String? = null
    private var isLoading = false
    private var isEndOfList = false

    init {
        fetchProducts()
    }

    fun fetchProducts(isLoadMore: Boolean = false) {
        if (isLoading || (isLoadMore && isEndOfList)) return

        viewModelScope.launch {
            isLoading = true
            if (!isLoadMore) {
                _uiState.value = RequestItemsUiState.Loading
            }

            try {
                val newProducts = itemsRepository.getProducts(lastVisibleId = lastVisibleId)

                if (newProducts.isNotEmpty()) {
                    // Extract pagination ID from docId if it contains "|" separator
                    // Format: "productId|lastItemDocId" or just "productId"
                    val lastDocId = newProducts.last().docId
                    lastVisibleId = if (lastDocId.contains("|")) {
                        lastDocId.split("|")[1] // Extract the actual item document ID
                    } else {
                        null // If no pagination info, we've reached the end or pagination is broken
                    }
                    
                    // Clean up docId to remove pagination info for display
                    val cleanedProducts = newProducts.map { product ->
                        product.copy(docId = product.docId.split("|")[0])
                    }

                    val currentProducts = if (isLoadMore && _uiState.value is RequestItemsUiState.Success) {
                        (_uiState.value as RequestItemsUiState.Success).products
                    } else {
                        emptyList()
                    }

                    // Merge products with the same barcode (not docId, since docId might vary)
                    val mergedProducts = if (currentProducts.isNotEmpty()) {
                        val productMap = currentProducts.associateBy { it.barcode.ifEmpty { it.docId } }.toMutableMap()
                        
                        cleanedProducts.forEach { newProduct ->
                            val barcodeKey = newProduct.barcode.ifEmpty { newProduct.docId }
                            val existingProduct = productMap[barcodeKey]
                            if (existingProduct != null) {
                                // Merge: sum quantities and use the nearest expiry date
                                val mergedQuantity = existingProduct.quantity + newProduct.quantity
                                val mergedExpiryDate = when {
                                    existingProduct.expiryDate == null -> newProduct.expiryDate
                                    newProduct.expiryDate == null -> existingProduct.expiryDate
                                    else -> {
                                        // Use the nearest expiry date
                                        val existingDate = existingProduct.expiryDate.toDate()
                                        val newDate = newProduct.expiryDate.toDate()
                                        if (existingDate.before(newDate)) existingProduct.expiryDate else newProduct.expiryDate
                                    }
                                }
                                
                                productMap[barcodeKey] = existingProduct.copy(
                                    quantity = mergedQuantity,
                                    expiryDate = mergedExpiryDate
                                )
                            } else {
                                // New product, add it
                                productMap[barcodeKey] = newProduct
                            }
                        }
                        
                        productMap.values.toList()
                    } else {
                        cleanedProducts
                    }

                    _uiState.value = RequestItemsUiState.Success(mergedProducts)
                } else if (!isLoadMore) {
                    _uiState.value = RequestItemsUiState.Success(emptyList())
                } else {
                    isEndOfList = true
                }

            } catch (e: Exception) {
                if (!isLoadMore) {
                    _uiState.value = RequestItemsUiState.Error
                }
            }
            isLoading = false
        }
    }

    fun onAddProduct(productDocId: String) {
        val currentQuantity = _productQuantities.value[productDocId] ?: 0
        val availableStock = getAvailableStock(productDocId)
        if (currentQuantity >= availableStock) {
            return
        }
        val newMap = _productQuantities.value.toMutableMap()
        newMap[productDocId] = currentQuantity + 1
        _productQuantities.value = newMap
    }

    private fun getAvailableStock(productDocId: String): Int {
        val products = when (val state = _uiState.value) {
            is RequestItemsUiState.Success -> state.products
            else -> emptyList()
        }
        return products.find { it.docId == productDocId }?.stock ?: 0
    }

    fun onRemoveProduct(productDocId: String) {
        val currentQuantity = _productQuantities.value[productDocId] ?: 0
        if (currentQuantity > 0) {
            val newMap = _productQuantities.value.toMutableMap()
            if (currentQuantity == 1) {
                newMap.remove(productDocId)
            } else {
                newMap[productDocId] = currentQuantity - 1
            }
            _productQuantities.value = newMap
        }
    }

    fun clearQuantities() {
        _productQuantities.value = emptyMap()
    }

    fun setProposedDeliveryDate(date: Date?) {
        _proposedDeliveryDate.value = date
    }

    fun clearProposedDeliveryDate() {
        _proposedDeliveryDate.value = null
    }

    fun submitRequest() {
        val itemsToSend = _productQuantities.value
        if (itemsToSend.isEmpty()) {
            return
        }
        // Date is obligatory - validate before submitting
        if (_proposedDeliveryDate.value == null) {
            _submissionState.value = SubmissionState.Error("Por favor, selecione uma data de entrega")
            return
        }
        viewModelScope.launch {
            _submissionState.value = SubmissionState.Loading
            val result = requestsRepository.submitRequest(itemsToSend, _proposedDeliveryDate.value)
            result.fold(
                onSuccess = {
                    clearQuantities()
                    clearProposedDeliveryDate()
                    _submissionState.value = SubmissionState.Success
                },
                onFailure = { error ->
                    _submissionState.value = SubmissionState.Error(error.message ?: "Erro desconhecido")
                }
            )
        }
    }

    fun resetSubmissionState() {
        _submissionState.value = SubmissionState.Idle
    }
}

sealed class SubmissionState {
    object Idle : SubmissionState()
    object Loading : SubmissionState()
    object Success : SubmissionState()
    data class Error(val message: String) : SubmissionState()
}