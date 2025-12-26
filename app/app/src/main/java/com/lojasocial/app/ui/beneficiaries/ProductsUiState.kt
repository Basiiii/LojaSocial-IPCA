package com.lojasocial.app.ui.beneficiaries

import com.lojasocial.app.domain.Product

sealed interface ProductsUiState {
    data class Success(val products: List<Product>) : ProductsUiState
    object Error : ProductsUiState
    object Loading : ProductsUiState
}
