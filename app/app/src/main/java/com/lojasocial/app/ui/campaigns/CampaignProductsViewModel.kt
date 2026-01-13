package com.lojasocial.app.ui.campaigns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.repository.audit.AuditRepository
import com.lojasocial.app.repository.audit.CampaignProductReceipt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CampaignProductsUiState {
    object Loading : CampaignProductsUiState()
    data class Success(val products: List<CampaignProductReceipt>) : CampaignProductsUiState()
    data class Error(val message: String) : CampaignProductsUiState()
}

@HiltViewModel
class CampaignProductsViewModel @Inject constructor(
    private val auditRepository: AuditRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CampaignProductsUiState>(CampaignProductsUiState.Loading)
    val uiState: StateFlow<CampaignProductsUiState> = _uiState.asStateFlow()

    fun fetchCampaignProducts(campaignId: String) {
        viewModelScope.launch {
            // Validate campaign ID before making API call
            if (campaignId.isBlank()) {
                _uiState.value = CampaignProductsUiState.Error("ID da campanha inválido")
                return@launch
            }
            
            _uiState.value = CampaignProductsUiState.Loading
            val result = auditRepository.getCampaignProducts(campaignId)
            result.fold(
                onSuccess = { products ->
                    _uiState.value = CampaignProductsUiState.Success(products)
                },
                onFailure = { error ->
                    _uiState.value = CampaignProductsUiState.Error(error.message ?: "Erro ao carregar produtos")
                }
            )
        }
    }
}
