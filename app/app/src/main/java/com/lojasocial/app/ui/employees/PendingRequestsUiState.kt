package com.lojasocial.app.ui.employees

import com.lojasocial.app.domain.PendingRequest

sealed interface PendingRequestsUiState {
    data object Loading : PendingRequestsUiState
    data class Success(val requests: List<PendingRequest>) : PendingRequestsUiState
    data class Error(val message: String) : PendingRequestsUiState
}