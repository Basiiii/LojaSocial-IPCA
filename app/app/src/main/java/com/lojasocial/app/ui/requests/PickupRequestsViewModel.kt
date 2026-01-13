package com.lojasocial.app.ui.requests

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.domain.request.Request
import com.lojasocial.app.repository.request.RequestsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class PickupRequestsViewModel @Inject constructor(
    private val repository: RequestsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PickupRequestsUiState>(PickupRequestsUiState.Loading)
    val uiState: StateFlow<PickupRequestsUiState> = _uiState.asStateFlow()

    private val _selectedRequest = MutableStateFlow<Request?>(null)
    val selectedRequest: StateFlow<Request?> = _selectedRequest.asStateFlow()

    private val _userProfile = MutableStateFlow<com.lojasocial.app.repository.request.UserProfileData?>(null)
    val userProfile: StateFlow<com.lojasocial.app.repository.request.UserProfileData?> = _userProfile.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    // Filter by userId if provided (for beneficiary portal)
    private var filterUserId: String? = null

    private val _hasMoreRequests = MutableStateFlow(true)
    val hasMoreRequests: StateFlow<Boolean> = _hasMoreRequests.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _pendingRequestsCount = MutableStateFlow<Int?>(null)
    val pendingRequestsCount: StateFlow<Int?> = _pendingRequestsCount.asStateFlow()

    private var lastLoadedSubmissionDate: Date? = null

    init {
        // Don't fetch requests here - wait for filter to be set by the View
        // This prevents fetching all requests before we know if we should filter by user
        fetchPendingRequestsCount()
    }

    fun setFilterUserId(userId: String?) {
        filterUserId = userId
        // Clear pending count when switching to user filter (beneficiary view)
        // The count will be calculated from the filtered requests list
        if (userId != null) {
            _pendingRequestsCount.value = null
        }
        // Always fetch requests when filter is set/changed
        fetchRequests()
    }

    fun fetchRequests() {
        viewModelScope.launch {
            _uiState.value = PickupRequestsUiState.Loading
            lastLoadedSubmissionDate = null
            _hasMoreRequests.value = true
            
            // Use getRequestsByUserId() when filtering by specific user (for beneficiaries)
            // This ensures Firestore query-level filtering at the database level
            // Use pagination for admins viewing all requests
            val currentFilterUserId = filterUserId
            if (currentFilterUserId != null) {
                // For beneficiaries: fetch only requests for the specific user (database-level filter)
                try {
                    val result = repository.getRequestsByUserId(currentFilterUserId)
                    result.fold(
                        onSuccess = { requests ->
                            _uiState.value = PickupRequestsUiState.Success(requests)
                            _hasMoreRequests.value = false // No pagination for user-specific requests
                        },
                        onFailure = { error ->
                            _uiState.value = PickupRequestsUiState.Error(error.message ?: "Erro ao carregar pedidos")
                            _hasMoreRequests.value = false
                        }
                    )
                } catch (e: Exception) {
                    _uiState.value = PickupRequestsUiState.Error(e.message ?: "Erro ao carregar pedidos")
                    _hasMoreRequests.value = false
                }
            } else {
                // For admins: use pagination
                try {
                    val (requests, hasMore) = repository.getRequestsPaginated(limit = 15)
                    _uiState.value = PickupRequestsUiState.Success(requests)
                    _hasMoreRequests.value = hasMore
                    lastLoadedSubmissionDate = requests.lastOrNull()?.submissionDate
                } catch (e: Exception) {
                    _uiState.value = PickupRequestsUiState.Error(e.message ?: "Erro ao carregar pedidos")
                    _hasMoreRequests.value = false
                }
            }
        }
    }

    fun loadMoreRequests() {
        // Only load more if not filtering by user (pagination only works for admin view)
        if (filterUserId != null || _isLoadingMore.value || !_hasMoreRequests.value) return
        
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val (requests, hasMore) = repository.getRequestsPaginated(
                    limit = 15,
                    lastSubmissionDate = lastLoadedSubmissionDate
                )
                
                if (requests.isNotEmpty()) {
                    val currentState = _uiState.value
                    if (currentState is PickupRequestsUiState.Success) {
                        _uiState.value = PickupRequestsUiState.Success(currentState.requests + requests)
                        lastLoadedSubmissionDate = requests.lastOrNull()?.submissionDate
                    }
                    _hasMoreRequests.value = hasMore
                } else {
                    _hasMoreRequests.value = false
                }
            } catch (e: Exception) {
                // Error loading more - stop trying
                _hasMoreRequests.value = false
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun selectRequest(requestId: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            val result = repository.getRequestById(requestId)
            result.fold(
                onSuccess = { request ->
                    // If filtering by user (beneficiary mode), validate the request belongs to that user
                    if (filterUserId != null && request.userId != filterUserId) {
                        _actionState.value = ActionState.Error("Não tem permissão para ver este pedido")
                        _actionState.value = ActionState.Idle
                        return@launch
                    }
                    
                    _selectedRequest.value = request
                    // Fetch user profile
                    val userResult = repository.getUserProfile(request.userId)
                    userResult.fold(
                        onSuccess = { profile ->
                            _userProfile.value = profile
                        },
                        onFailure = {
                            _userProfile.value = com.lojasocial.app.repository.request.UserProfileData()
                        }
                    )
                    _actionState.value = ActionState.Idle
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Erro ao carregar detalhes")
                }
            )
        }
    }

    fun acceptRequest(requestId: String, scheduledPickupDate: Date) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            val result = repository.acceptRequest(requestId, scheduledPickupDate)
            result.fold(
                onSuccess = {
                    // Set success immediately to close dialog
                    _actionState.value = ActionState.Success("Pedido aceite com sucesso")
                    
                    // Close dialog immediately
                    clearSelectedRequest()
                    
                    // Refresh list (notification is already sent by repository)
                    launch {
                        fetchRequests()
                        fetchPendingRequestsCount() // Refresh count
                    }
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Erro ao aceitar pedido")
                }
            )
        }
    }

    fun rejectRequest(requestId: String, reason: String?) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            val result = repository.rejectRequest(requestId, reason)
            result.fold(
                onSuccess = {
                    // Set success immediately to close dialog
                    _actionState.value = ActionState.Success("Pedido rejeitado")
                    
                    // Close dialog immediately
                    clearSelectedRequest()
                    
                    // Refresh list in background (non-blocking)
                    launch {
                        fetchRequests()
                        fetchPendingRequestsCount() // Refresh count
                    }
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Erro ao rejeitar pedido")
                }
            )
        }
    }

    fun proposeNewDate(requestId: String, proposedDate: Date) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            val result = repository.proposeNewDate(requestId, proposedDate)
            result.fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Nova data proposta")
                    // Refresh the selected request to show updated date
                    selectRequest(requestId)
                    fetchRequests()
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Erro ao propor nova data")
                }
            )
        }
    }

    fun acceptEmployeeProposedDate(requestId: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            val result = repository.acceptEmployeeProposedDate(requestId)
            result.fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Data aceite")
                    clearSelectedRequest()
                    launch {
                        fetchRequests()
                        fetchPendingRequestsCount()
                    }
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Erro ao aceitar data")
                }
            )
        }
    }

    fun proposeNewDeliveryDate(requestId: String, proposedDate: Date) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            val result = repository.proposeNewDeliveryDate(requestId, proposedDate)
            result.fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Nova data proposta")
                    // Refresh the selected request to show updated date
                    selectRequest(requestId)
                    fetchRequests()
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Erro ao propor nova data")
                }
            )
        }
    }

    fun completeRequest(requestId: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            val result = repository.completeRequest(requestId)
            result.fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Pedido concluído")
                    clearSelectedRequest()
                    fetchRequests() // Refresh list
                    fetchPendingRequestsCount() // Refresh count
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Erro ao concluir pedido")
                }
            )
        }
    }

    fun cancelDelivery(requestId: String, beneficiaryAbsent: Boolean = false) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            val result = repository.cancelDelivery(requestId, beneficiaryAbsent)
            result.fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Entrega cancelada")
                    clearSelectedRequest()
                    fetchRequests() // Refresh list
                    fetchPendingRequestsCount() // Refresh count
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Erro ao cancelar entrega")
                }
            )
        }
    }

    fun rescheduleDelivery(requestId: String, newDate: Date, isEmployeeRescheduling: Boolean) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            val result = repository.rescheduleDelivery(requestId, newDate, isEmployeeRescheduling)
            result.fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Entrega reagendada")
                    clearSelectedRequest()
                    fetchRequests() // Refresh list
                    fetchPendingRequestsCount() // Refresh count
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(error.message ?: "Erro ao reagendar entrega")
                }
            )
        }
    }

    fun clearSelectedRequest() {
        _selectedRequest.value = null
        _userProfile.value = null
    }

    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }

    private fun fetchPendingRequestsCount() {
        viewModelScope.launch {
            // Only fetch count for admin view (not for filtered user requests)
            if (filterUserId == null) {
                val result = repository.getPendingRequestsCount()
                result.fold(
                    onSuccess = { count ->
                        _pendingRequestsCount.value = count
                    },
                    onFailure = {
                        // On error, keep previous count or null
                        Log.e("PickupRequestsViewModel", "Failed to fetch pending count: ${it.message}")
                    }
                )
            }
        }
    }

}

sealed class PickupRequestsUiState {
    object Loading : PickupRequestsUiState()
    data class Success(val requests: List<Request>) : PickupRequestsUiState()
    data class Error(val message: String) : PickupRequestsUiState()
}

sealed class ActionState {
    object Idle : ActionState()
    object Loading : ActionState()
    data class Success(val message: String) : ActionState()
    data class Error(val message: String) : ActionState()
}

