package com.lojasocial.app.ui.requests

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lojasocial.app.domain.request.Request
import com.lojasocial.app.repository.request.RequestsRepository
import com.lojasocial.app.utils.NotificationHelper
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
    private val repository: RequestsRepository,
    private val notificationHelper: NotificationHelper
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
        fetchRequests()
        fetchPendingRequestsCount()
    }

    fun setFilterUserId(userId: String?) {
        filterUserId = userId
        fetchRequests()
    }

    fun fetchRequests() {
        viewModelScope.launch {
            _uiState.value = PickupRequestsUiState.Loading
            lastLoadedSubmissionDate = null
            _hasMoreRequests.value = true
            
            // Use getRequests() Flow when filtering by current user (for beneficiaries)
            // This ensures Firestore query-level filtering which respects security rules
            // Use pagination for admins viewing all requests
            if (filterUserId != null) {
                // For beneficiaries: use Flow-based fetching (respects security rules)
                repository.getRequests()
                    .catch { exception ->
                        _uiState.value = PickupRequestsUiState.Error(exception.message ?: "Erro ao carregar pedidos")
                        _hasMoreRequests.value = false
                    }
                    .collect { requests ->
                        _uiState.value = PickupRequestsUiState.Success(requests)
                        _hasMoreRequests.value = false // No pagination for user-specific requests
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
                    
                    // Refresh list and send notification in background (non-blocking)
                    launch {
                        // Get request to send notification
                        val requestResult = repository.getRequestById(requestId)
                        requestResult.fold(
                            onSuccess = { request ->
                                // Send notification to user
                                sendAcceptanceNotification(request.userId, scheduledPickupDate)
                            },
                            onFailure = { }
                        )
                        // Refresh list after notification (reset pagination)
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

    private suspend fun sendAcceptanceNotification(userId: String, pickupDate: Date) {
        val tokenResult = repository.getUserFcmToken(userId)
        tokenResult.fold(
            onSuccess = { token ->
                token?.let {
                    val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "PT"))
                    val formattedDate = dateFormat.format(pickupDate)
                    notificationHelper.sendNotification(
                        token = it,
                        title = "Pedido Aceite",
                        body = "O teu pedido foi aceite. Data de levantamento: $formattedDate"
                    )
                }
            },
            onFailure = { }
        )
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

