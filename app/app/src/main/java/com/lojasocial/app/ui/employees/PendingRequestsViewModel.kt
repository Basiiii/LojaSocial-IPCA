package com.lojasocial.app.ui.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.data.repository.PendingRequestsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingRequestsViewModel @Inject constructor(
    private val repository: PendingRequestsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PendingRequestsUiState>(PendingRequestsUiState.Loading)
    val uiState: StateFlow<PendingRequestsUiState> = _uiState.asStateFlow()

    init {
        fetchPendingRequests()
    }

    private fun fetchPendingRequests() {
        viewModelScope.launch {
            repository.getPendingRequests()
                .catch { exception ->
                    _uiState.value = PendingRequestsUiState.Error(exception.message ?: "Ocorreu um erro desconhecido")
                }
                .collect { requests ->
                    _uiState.value = PendingRequestsUiState.Success(requests)
                }
        }
    }
}