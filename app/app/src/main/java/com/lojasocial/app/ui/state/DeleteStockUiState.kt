package com.lojasocial.app.ui.state

data class DeleteStockUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val quantityToReduce: String = "1"
)
