package com.lojasocial.app.ui.beneficiaries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.repository.ProductsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RequestItemsViewModel(private val productsRepository: ProductsRepository = ProductsRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductsUiState>(ProductsUiState.Loading)
    val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

    private val _productQuantities = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val productQuantities: StateFlow<Map<Int, Int>> = _productQuantities.asStateFlow()

    init {
        fetchProducts()
    }

    private fun fetchProducts() {
        viewModelScope.launch {
            try {
                val products = productsRepository.getProducts()
                _uiState.value = ProductsUiState.Success(products)
            } catch (e: Exception) {
                _uiState.value = ProductsUiState.Error
            }
        }
    }

    fun onAddProduct(productId: Int) {
        val currentQuantity = _productQuantities.value[productId] ?: 0
        _productQuantities.value = _productQuantities.value.toMutableMap().apply {
            this[productId] = currentQuantity + 1
        }
    }

    fun onRemoveProduct(productId: Int) {
        val currentQuantity = _productQuantities.value[productId] ?: 0
        if (currentQuantity > 0) {
            _productQuantities.value = _productQuantities.value.toMutableMap().apply {
                this[productId] = currentQuantity - 1
            }
        }
    }

    fun clearQuantities() {
        _productQuantities.value = emptyMap()
    }
}
