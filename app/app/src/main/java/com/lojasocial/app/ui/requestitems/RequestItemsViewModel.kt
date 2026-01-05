package com.lojasocial.app.ui.requestitems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.domain.RequestItem
import com.lojasocial.app.repository.ItemsRepository
import com.lojasocial.app.repository.OrdersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RequestItemsViewModel @Inject constructor(
    private val itemsRepository: ItemsRepository,
    private val ordersRepository: OrdersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RequestItemsUiState>(RequestItemsUiState.Loading)
    val uiState: StateFlow<RequestItemsUiState> = _uiState.asStateFlow()

    private val _productQuantities = MutableStateFlow<Map<String, Int>>(emptyMap())
    val productQuantities: StateFlow<Map<String, Int>> = _productQuantities.asStateFlow()

    private val _submissionState = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val submissionState: StateFlow<SubmissionState> = _submissionState.asStateFlow()

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
                    lastVisibleId = newProducts.last().docId

                    val currentProducts = if (isLoadMore && _uiState.value is RequestItemsUiState.Success) {
                        (_uiState.value as RequestItemsUiState.Success).products
                    } else {
                        emptyList()
                    }

                    _uiState.value = RequestItemsUiState.Success(currentProducts + newProducts)
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

    fun submitRequest() {
        val itemsToSend = _productQuantities.value
        if (itemsToSend.isEmpty()) {
            return
        }
        viewModelScope.launch {
            _submissionState.value = SubmissionState.Loading
            val result = ordersRepository.submitOrder(itemsToSend)
            result.fold(
                onSuccess = {
                    clearQuantities()
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