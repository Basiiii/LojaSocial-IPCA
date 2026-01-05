package com.lojasocial.app.ui.requestitems

import com.lojasocial.app.domain.RequestItem

sealed interface RequestItemsUiState {
    object Loading : RequestItemsUiState
    data class Success(val products: List<RequestItem>) : RequestItemsUiState
    object Error : RequestItemsUiState
}
